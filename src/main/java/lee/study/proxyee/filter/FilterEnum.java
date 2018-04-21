package lee.study.proxyee.filter;

public enum FilterEnum {

    JS(".js"),
    JPG(".jpg"),
    PNG(".png"),
    GIFF(".giff"),
    JPEG(".jpeg"),
    GIF(".gif"),
    ICO(".ico"),
    MP4(".mp4"),
    AVI(".api"),
    FLV(".flv"),
    CSS(".css");

    private String value;

    FilterEnum(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
