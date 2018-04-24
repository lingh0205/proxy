package lee.study.proxyee.pojo;

/**
 * @author LinGH
 * @date 2018-04-24
 */
public enum  CaptureEntity {

    ID("_id",0),
    STATIC_RESOURCE("static_resource",1),
    METHOD("method",2),
    STATUS_CODE("status_code",3),
    CONTENT_TYPE("content_type",4),
    CONTENT_LENGTH("content_length",5),
    HOST("host",6),
    URL("url",7),
    SCHEME("scheme",8),
    PATH("path",9),
    HEADER("header",10),
    CONTENT("content",11),
    REQUEST_HEADER("request_header",12),
    REQUEST_CONTENT("request_content",13),
    DATE_START("date_start",14),
    DATE_END("date_end",15),
    EXTENSION("extension",16),
    PORT("port",17),
    LENGTH("length",18);

    private String cname;
    private int code;

    CaptureEntity(String cname, int code) {
        this.cname = cname;
        this.code = code;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }
}
