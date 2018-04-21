package lee.study.proxyee.filter;

import java.util.ArrayList;
import java.util.List;

public class Filter {

    public final static List<String> filter = new ArrayList<>();

    static {
        for (FilterEnum ext : FilterEnum.values()) {
            filter.add(ext.getValue());
        }
    }

    /**
     * 通过后缀名进行过滤
     * @param extension
     * @return
     */
    public static boolean ignoreByExtension(String extension){
        return filter.contains(extension);
    }

    /**
     * 待实现
     * @return
     */
    public static boolean ignoreByContentType(){
        return false;
    }

}
