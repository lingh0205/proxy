package lee.study.proxyee.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import lee.study.proxyee.crt.CertUtil;
import lee.study.proxyee.exception.HttpProxyExceptionHandle;
import lee.study.proxyee.handler.HttpProxyServerHandle;
import lee.study.proxyee.intercept.*;
import lee.study.proxyee.proxy.ProxyConfig;
import lee.study.proxyee.util.GlobalProxyConfigUtil;
import org.apache.log4j.Logger;

public class HttpProxyServer {

  private final static Logger LOGGER = Logger.getLogger(HttpProxyServer.class);
  private static int PORT = 1080;
  private static String CONFIG_PATH = null;
  public final static HttpResponseStatus SUCCESS = new HttpResponseStatus(200,
      "Connection established");

  private HttpProxyCACertFactory caCertFactory;
  private HttpProxyServerConfig serverConfig;
  private HttpProxyInterceptInitializer proxyInterceptInitializer;
  private HttpProxyExceptionHandle httpProxyExceptionHandle;
  private ProxyConfig proxyConfig;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  public HttpProxyServer() {
    try {
      init();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static final String getConfigPath(){
    return CONFIG_PATH;
  }

  public HttpProxyServer(HttpProxyCACertFactory caCertFactory) {
    this.caCertFactory = caCertFactory;
    try {
      init();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void init() throws Exception {
    if (serverConfig == null) {
      serverConfig = new HttpProxyServerConfig();
      serverConfig.setClientSslCtx(
          SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
              .build());
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      X509Certificate caCert;
      PrivateKey caPriKey;
      if (caCertFactory == null) {
        caCert = CertUtil.loadCert(classLoader.getResourceAsStream("ca.crt"));
        caPriKey = CertUtil.loadPriKey(classLoader.getResourceAsStream("ca_private.der"));
      } else {
        caCert = caCertFactory.getCACert();
        caPriKey = caCertFactory.getCAPriKey();
      }
      serverConfig.setIssuer(CertUtil.getSubject(caCert));
      serverConfig.setCaNotBefore(caCert.getNotBefore());
      serverConfig.setCaNotAfter(caCert.getNotAfter());
      serverConfig.setCaPriKey(caPriKey);
      KeyPair keyPair = CertUtil.genKeyPair();
      serverConfig.setServerPriKey(keyPair.getPrivate());
      serverConfig.setServerPubKey(keyPair.getPublic());
      serverConfig.setLoopGroup(new NioEventLoopGroup());
    }

    if (proxyInterceptInitializer == null) {
      proxyInterceptInitializer = new HttpProxyInterceptInitializer();
    }
    if (httpProxyExceptionHandle == null) {
      httpProxyExceptionHandle = new HttpProxyExceptionHandle();
    }
  }

  public HttpProxyServer serverConfig(HttpProxyServerConfig serverConfig) {
    this.serverConfig = serverConfig;
    return this;
  }

  public HttpProxyServer proxyInterceptInitializer(
      HttpProxyInterceptInitializer proxyInterceptInitializer) {
    this.proxyInterceptInitializer = proxyInterceptInitializer;
    return this;
  }

  public HttpProxyServer httpProxyExceptionHandle(
      HttpProxyExceptionHandle httpProxyExceptionHandle) {
    this.httpProxyExceptionHandle = httpProxyExceptionHandle;
    return this;
  }

  public HttpProxyServer proxyConfig(ProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
    return this;
  }

  public void start(int port) {
    bossGroup = new NioEventLoopGroup();
    workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
//          .option(ChannelOption.SO_BACKLOG, 100)
//          .handler(new LoggingHandler(LogLevel.DEBUG))
          .childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
              ch.pipeline().addLast("httpCodec", new HttpServerCodec());
              ch.pipeline().addLast("aggregator", new HttpObjectAggregator(1048576));
              ch.pipeline().addLast("serverHandle",
                  new HttpProxyServerHandle(serverConfig, proxyInterceptInitializer, proxyConfig,
                      httpProxyExceptionHandle));
            }
          });
      ChannelFuture f = b
          .bind(port)
          .sync();
      f.channel().closeFuture().sync();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  public void close() {
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 2){
      CONFIG_PATH = args[0];
      PORT = Integer.valueOf(args[1]);
      LOGGER.info(String.format("Start to proxy at address 127.0.0.1:%d, Using config : %s.", PORT, CONFIG_PATH));
    }else {
      LOGGER.info(String.format("Start to proxy at address 127.0.0.1:%d, Using config : default config file.", PORT));
    }
    new HttpProxyServer().proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
          @Override
          public void init(HttpProxyInterceptPipeline pipeline) {
            pipeline.addLast(new CertDownIntercept());  //处理证书下载
            if (GlobalProxyConfigUtil.openRecord()) {
              pipeline.addLast(new HttpRecordIntercept());
            }else {
              pipeline.addLast(new HttpProxyIntercept(){
                @Override
                public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) throws Exception {
                  httpResponse.headers().add("intercept", "proxy response");
                  pipeline.afterResponse(clientChannel, proxyChannel, httpResponse);
                }
              });
            }
          }
        })
        .httpProxyExceptionHandle(new HttpProxyExceptionHandle() {
          @Override
          public void beforeCatch(Channel clientChannel, Throwable cause) throws Exception {

          }

          @Override
          public void afterCatch(Channel clientChannel, Channel proxyChannel, Throwable cause)
              throws Exception {
            LOGGER.error("Failed to handler proxy request, Cause by : ", cause);
          }
        })
        .start(PORT);
  }

}
