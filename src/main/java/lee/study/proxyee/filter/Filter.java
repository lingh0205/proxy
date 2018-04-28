package lee.study.proxyee.filter;

import lee.study.proxyee.pojo.IgnoreConfig;
import lee.study.proxyee.util.GlobalProxyConfigUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LinGH
 * @date 2018-04-24
 */
public class Filter {

    private static List<String> EXT_FILTER = new ArrayList<>();
    private static List<String> CONTENT_TYPE_FILTER = new ArrayList<>();

    static {
        IgnoreConfig ignoreConfig = GlobalProxyConfigUtil.getIgnoreConfig();
        if (ignoreConfig != null){
            EXT_FILTER = ignoreConfig.getExts();
            CONTENT_TYPE_FILTER = ignoreConfig.getContentTypes();
        }
    }

    /**
     * @param extension
     * @return
     */
    public static boolean ignoreByExtension(String extension){
        return EXT_FILTER.contains(extension);
    }

    /**
     * @param contentType
     * @return
     */
    public static boolean ignoreByContentType(String contentType){
        return CONTENT_TYPE_FILTER.contains(contentType);
    }

}
