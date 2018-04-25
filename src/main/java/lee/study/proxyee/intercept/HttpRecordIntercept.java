package lee.study.proxyee.intercept;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import lee.study.proxyee.filter.Filter;
import lee.study.proxyee.pojo.CaptureEntity;
import lee.study.proxyee.util.GZIPUtil;
import lee.study.proxyee.util.HexUtil;
import lee.study.proxyee.util.MongoUtil;
import lee.study.proxyee.util.ProtoUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

/**
 * @author LinGH
 * @date 2018-04-24
 */
public class HttpRecordIntercept extends HttpProxyIntercept {

    private final static Logger LOGGER = Logger.getLogger(HttpRecordIntercept.class);

    private String token;
    private StringBuilder req = new StringBuilder();
    private StringBuilder resp = new StringBuilder();
    private Document document = new Document();

    private final static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final static String IGNORE = "IGNORE";

    private boolean isGzip;
    private int contentLength = 0;
    private byte[] body = new byte[contentLength];

    public HttpRecordIntercept(){
        this.token = String.format("%d-%s", System.currentTimeMillis(), UUID.randomUUID().toString());
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpRequest httpRequest,ProtoUtil.RequestProto requestProto, HttpProxyInterceptPipeline pipeline) throws Exception {
        URI uri = new URI(httpRequest.uri());
        String path = uri.getPath();
        if (StringUtils.isNotBlank(path) && path.substring(path.lastIndexOf("/")).contains(".")){
            String ext = path.substring(path.lastIndexOf("/"));
            document.put(CaptureEntity.EXTENSION.getCname(), ext.substring(ext.lastIndexOf('.')));
        }else {
            document.put(CaptureEntity.EXTENSION.getCname(), "/");
        }
        // filter by ext
        if (Filter.ignoreByExtension(document.getString(CaptureEntity.EXTENSION.getCname()))){
            this.token = String.format("%s-%s", this.token, IGNORE);
        }else {
            document.put(CaptureEntity.ID.getCname(), this.token);
            document.put(CaptureEntity.STATIC_RESOURCE.getCname(), 0);
            document.put(CaptureEntity.METHOD.getCname(), httpRequest.method().name());
            if (requestProto.getSsl()){
                document.put(CaptureEntity.PORT.getCname(), requestProto.getPort());
                document.put(CaptureEntity.HOST.getCname(), requestProto.getHost());
                document.put(CaptureEntity.URL.getCname(), String.format("%s://%s%s%s", "https",
                        requestProto.getHost(), requestProto.getPort() == 443 ? "" : ":" +requestProto.getPort(), uri.toString()));
                document.put(CaptureEntity.SCHEME.getCname(), "https");
            }else {
                document.put(CaptureEntity.PORT.getCname(), uri.getPort() == -1 ? 80 : uri.getPort());
                document.put(CaptureEntity.HOST.getCname(), uri.getHost());
                document.put(CaptureEntity.URL.getCname(), uri.toString());
                document.put(CaptureEntity.SCHEME.getCname(), uri.getScheme());
            }
            document.put(CaptureEntity.PATH.getCname(), path);

            document.put(CaptureEntity.REQUEST_HEADER.getCname(), convert2Document(httpRequest.headers()));
            if (httpRequest instanceof FullHttpRequest) {
                document.put(CaptureEntity.REQUEST_CONTENT.getCname(), HexUtil.bytes2hex03(((FullHttpRequest) httpRequest).content()));
            }

            document.put(CaptureEntity.DATE_START.getCname(), new Date());
        }
        pipeline.beforeRequest(clientChannel, httpRequest, requestProto);
    }

    public Document convert2Document(HttpHeaders headers){
        Document doc = new Document();
        for(Map.Entry<String, String> entry : headers.entries()){
            doc.put(entry.getKey(), entry.getValue());
        }
        return doc;
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpContent httpContent, ProtoUtil.RequestProto requestProto, HttpProxyInterceptPipeline pipeline) throws Exception {
        if (httpContent instanceof DefaultHttpContent && ! this.token.endsWith(IGNORE)){
            DefaultHttpContent defaultHttpContent = (DefaultHttpContent) httpContent;
            req.append(HexUtil.bytes2hex03(defaultHttpContent.content()));
        }else if (! this.token.endsWith(IGNORE)  && httpContent instanceof LastHttpContent) {
            document.put(CaptureEntity.REQUEST_CONTENT.getCname(), req.toString());
        }
        super.beforeRequest(clientChannel, httpContent, requestProto, pipeline);
    }

    @Override
    public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) throws Exception {
        String contentType = httpResponse.headers().get("Content-Type");
        // filter by ext or content-type
        if ( ! this.token.endsWith(IGNORE) && ! Filter.ignoreByContentType(contentType)){
            document.put(CaptureEntity.STATUS_CODE.getCname(), httpResponse.getStatus().code());
            document.put(CaptureEntity.CONTENT_TYPE.getCname(), contentType);
            document.put(CaptureEntity.CONTENT_LENGTH.getCname(),  httpResponse.headers().get("Content-Length"));
            String encoding = httpResponse.headers().getAsString("Content-Encoding");
            if (StringUtils.isNotBlank(encoding) && encoding.contains("gzip")){
                isGzip = Boolean.TRUE;
            }
            document.put(CaptureEntity.HEADER.name(),convert2Document(httpResponse.headers()));
            document.put(CaptureEntity.DATE_END.getCname(), new Date());
        }else {
            if (!this.token.endsWith(IGNORE)){
                this.token = String.format("%s-%s", this.token, IGNORE);
            }
        }
        httpResponse.headers().add("intercept", "test");
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse);
    }

    @Override
    public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpContent httpContent, HttpProxyInterceptPipeline pipeline) throws Exception {
        if (! this.token.endsWith(IGNORE) && httpContent instanceof DefaultHttpContent){
            DefaultHttpContent defaultHttpContent = (DefaultHttpContent) httpContent;
            ByteBuf byteBuf = defaultHttpContent.content();
            if (isGzip){
                int extLength = byteBuf.writerIndex() - byteBuf.readerIndex();
                if (extLength != 0) {
                    byte[] newBody = new byte[contentLength + extLength];
                    byte[] temp = new byte[extLength];
                    ByteBuf copy = null;
                    try{
                        copy = byteBuf.copy();
                        copy.readBytes(temp);
                        //ԭ���ݿ���
                        System.arraycopy(body, 0, newBody, 0, contentLength);
                        //�����ݿ���
                        System.arraycopy(temp, 0, newBody, contentLength, temp.length);
                        body = newBody;
                        contentLength = body.length;
                    }finally {
                        if (copy != null){
                            copy.release();
                        }
                    }
                }
            }else {
                resp.append(HexUtil.bytes2hex03(byteBuf));
            }
        }else if (! this.token.endsWith(IGNORE)  && httpContent instanceof LastHttpContent) {
            document.put(CaptureEntity.ID.getCname(), this.token);

            document.put(CaptureEntity.DATE_END.getCname(), new Date());
            if (isGzip){
                document.put(CaptureEntity.CONTENT.getCname(), GZIPUtil.uncompressToString(body));
            }else {
                document.put(CaptureEntity.CONTENT.getCname(), resp.toString());
            }
            executorService.submit(new InsertDocumentTask(document));
        }
        pipeline.afterResponse(clientChannel, proxyChannel, httpContent);
    }

    class InsertDocumentTask implements Runnable{

        private Document document ;

        public InsertDocumentTask(Document document){
            this.document = document;
        }

        @Override
        public void run() {
            try {
                MongoUtil.save2Capture(document);
            }catch (Exception e){
                LOGGER.error("Failed to save data to mongodb for url : " + document.getString(CaptureEntity.URL.getCname()), e);
            }
        }
    }

}


