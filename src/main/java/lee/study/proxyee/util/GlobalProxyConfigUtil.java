package lee.study.proxyee.util;

import lee.study.proxyee.pojo.GlobalProxyConfig;
import lee.study.proxyee.pojo.IgnoreConfig;
import lee.study.proxyee.pojo.MongoDb;
import lee.study.proxyee.server.HttpProxyServer;
import org.apache.commons.lang3.StringUtils;
import org.ho.yaml.Yaml;

import java.io.File;
import java.net.URL;

public class GlobalProxyConfigUtil {

    private static GlobalProxyConfig globalProxyConfig;

    static {
        try{
            String configPath = HttpProxyServer.getConfigPath();
            if (globalProxyConfig == null){
                if (StringUtils.isBlank(configPath)){
                    URL url = Thread.currentThread().getContextClassLoader().getResource("");
                    String path = url.getPath();
                    configPath = path + "/config.yml";
                }
                File file = new File(configPath);
                globalProxyConfig = Yaml.loadType(file, GlobalProxyConfig.class);
            }
        }catch (Exception e){
            e.printStackTrace();
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

    public static Boolean openRecord(){
        if (globalProxyConfig != null) {
            return globalProxyConfig.isRecord();
        }
        return Boolean.FALSE;
    }

}
