package lee.study.proxyee.pojo;

import java.util.List;

/**
 * @author LinGH
 * @date 2018-04-24
 */
public class IgnoreConfig {

    private List<String> exts;
    private List<String> contentTypes;

    public List<String> getExts() {
        return exts;
    }

    public void setExts(List<String> exts) {
        this.exts = exts;
    }

    public List<String> getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(List<String> contentTypes) {
        this.contentTypes = contentTypes;
    }

}
