package lee.study.proxyee.intercept;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import lee.study.proxyee.filter.Filter;
import lee.study.proxyee.pojo.CaptureEntity;
import lee.study.proxyee.util.HexUtil;
import lee.study.proxyee.util.MongoUtil;
import lee.study.proxyee.util.ProtoUtil;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpRecordIntercept extends HttpProxyIntercept {

    private String token;

    private final static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final static String IGNORE = "IGNORE";

    public HttpRecordIntercept(){
        this.token = String.format("%d-%s", System.currentTimeMillis(), UUID.randomUUID().toString());
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpRequest httpRequest,ProtoUtil.RequestProto requestProto, HttpProxyInterceptPipeline pipeline) throws Exception {
        Document document = new Document();
        URI uri = new URI(httpRequest.uri());
        String path = uri.getPath();
        if (StringUtils.isNotBlank(path) && path.contains(".")){
            document.put(CaptureEntity.EXTENSION.getCname(), path.substring(path.indexOf('.',-1)));
        }
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
                document.put(CaptureEntity.REQUEST_CONTENT.getCname(), HexUtil.bytes2hex03(((FullHttpRequest) httpRequest).content().array()));
            }

            document.put(CaptureEntity.DATE_START.getCname(), new Date());
            executorService.submit(new RequestTask(document));
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
        super.beforeRequest(clientChannel, httpContent, requestProto, pipeline);
    }

    @Override
    public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) throws Exception {
        Document document = new Document();
        if ( ! this.token.endsWith(IGNORE)){
            document.put(CaptureEntity.ID.getCname(), this.token);
            document.put(CaptureEntity.STATUS_CODE.getCname(), httpResponse.getStatus().code());
            document.put(CaptureEntity.CONTENT_TYPE.getCname(), httpResponse.headers().get("Content-Type"));
            document.put(CaptureEntity.CONTENT_LENGTH.getCname(), httpResponse.headers().get("Content-Length"));
            document.put(CaptureEntity.HEADER.name(),convert2Document(httpResponse.headers()));
            if (httpResponse instanceof FullHttpResponse) {
                document.put(CaptureEntity.REQUEST_CONTENT.getCname(), HexUtil.bytes2hex03(((FullHttpResponse)httpResponse).content().array()));
            }
            document.put(CaptureEntity.DATE_END.getCname(), new Date());
            executorService.submit(new ResponseTask(document));
        }
        httpResponse.headers().add("intercept", "test");
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse);
    }

    @Override
    public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpContent httpContent, HttpProxyInterceptPipeline pipeline) throws Exception {
        super.afterResponse(clientChannel, proxyChannel, httpContent, pipeline);
    }
}

class RequestTask implements Runnable{

    private Document document ;

    public RequestTask(Document document){
        this.document = document;
    }

    @Override
    public void run() {
        try {
            MongoUtil.save2Capture(document);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

class ResponseTask implements Runnable{

    private Document document ;

    public ResponseTask(Document document){
        this.document = document;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        try {
            while (MongoUtil.find(document) == null) {
                if (System.currentTimeMillis() - start > 100){
                    //超时，直接丢弃
                    return;
                }
                Thread.sleep(10);
            }
            MongoUtil.save2Capture(document);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
