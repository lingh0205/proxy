package lee.study.proxyee.util;

import lee.study.proxyee.pojo.GlobalProxyConfig;
import lee.study.proxyee.pojo.IgnoreConfig;
import lee.study.proxyee.pojo.MongoDb;
import org.ho.yaml.Yaml;

import java.io.File;
import java.net.URL;

public class GlobalProxyConfigUtil {

    private static GlobalProxyConfig globalProxyConfig;

    static {
        try{
            if (globalProxyConfig == null){
                URL url = Thread.currentThread().getContextClassLoader().getResource("");
                String path = url.getPath();
                File file = new File(path + "/config.yml");
                globalProxyConfig = Yaml.loadType(file, GlobalProxyConfig.class);
            }
        }catch (Exception e){

        }
    }

    public static MongoDb getMongoDbConfig(){
        if (globalProxyConfig != null) {
            return globalProxyConfig.getMongodb();
        }
        return null;
    }

    public static IgnoreConfig getIgnoreConfig(){
        if (globalProxyConfig != null) {
            return globalProxyConfig.getIgnore();
        }
        return null;
    }

}
