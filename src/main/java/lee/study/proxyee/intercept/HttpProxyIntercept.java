package lee.study.proxyee.intercept;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import lee.study.proxyee.util.ProtoUtil;

public class HttpProxyIntercept {

  public void beforeRequest(Channel clientChannel, HttpRequest httpRequest,ProtoUtil.RequestProto requestProto,
      HttpProxyInterceptPipeline pipeline) throws Exception {
    pipeline.beforeRequest(clientChannel, httpRequest, requestProto);
  }

  public void beforeRequest(Channel clientChannel, HttpContent httpContent,
                            ProtoUtil.RequestProto requestProto, HttpProxyInterceptPipeline pipeline) throws Exception {
    pipeline.beforeRequest(clientChannel, httpContent, requestProto);
  }

  public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpResponse httpResponse,
      HttpProxyInterceptPipeline pipeline) throws Exception {
    pipeline.afterResponse(clientChannel, proxyChannel, httpResponse);
  }


  public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpContent httpContent,
      HttpProxyInterceptPipeline pipeline)
      throws Exception {
    pipeline.afterResponse(clientChannel, proxyChannel, httpContent);
  }
}
