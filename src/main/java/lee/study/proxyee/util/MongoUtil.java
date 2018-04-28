package lee.study.proxyee.util;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;
import lee.study.proxyee.pojo.CaptureEntity;
import lee.study.proxyee.pojo.GlobalProxyConfig;
import lee.study.proxyee.pojo.MongoDb;
import org.bson.Document;
import org.ho.yaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MongoUtil {

    private static MongoClient mongoClient = null;

    private static String HOST = "10.158.192.8";
    private static int PORT = 15001;
    private static String USER = "***";
    private static String PASSWD = "***";
    private static String DATABASE = "proxyee";
    private static boolean ISAUTH = Boolean.TRUE;

    private final static String TABLE = "capture";

    private static MongoCollection<Document> collection = null;

    static {
        mongoClient = getMonGoConnection();
    }

    private static MongoClient getMonGoConnection() {
        if (mongoClient == null) {
            List<ServerAddress> addrs = new ArrayList<>();
            try {
                MongoDb mongoDbConfig = GlobalProxyConfigUtil.getMongoDbConfig();
                if (mongoDbConfig!=null) {
                    HOST = mongoDbConfig.getHost();
                    PORT = mongoDbConfig.getPort();
                    USER = mongoDbConfig.getUser();
                    PASSWD = mongoDbConfig.getPasswd();
                    DATABASE = mongoDbConfig.getDatabase();
                    ISAUTH = mongoDbConfig.isAuth();
                }
                ServerAddress serverAddress = new ServerAddress(HOST, PORT);
                addrs.add(serverAddress);
            }catch (Exception e){
                System.out.println("Failed to Connect to mongo, Cause by : " + e);
            }

            MongoCredential credential = MongoCredential.createScramSha1Credential(USER, DATABASE, PASSWD.toCharArray());
            List<MongoCredential> credentials = new ArrayList<>();
            credentials.add(credential);
            if (ISAUTH) {
                mongoClient = new MongoClient(addrs, credentials);
            }else {
                mongoClient = new MongoClient(HOST, PORT);
            }
        }
        return mongoClient;
    }

    public static MongoCollection<Document> connect(String table){
        MongoDatabase mongoDatabase = getMonGoConnection().getDatabase(DATABASE);
        return mongoDatabase.getCollection(table);
    }

    private static void connect(){
        if (collection == null) {
            collection = connect(TABLE);
        }
        return;
    }

    public static void saveMany2Capture(List<Document> list, InsertManyOptions options){
        connect();
        saveMany(collection, list,options);
    }

    public static void saveMany(MongoCollection<Document> collection, List<Document> list, InsertManyOptions options){
        collection.insertMany(list, options);
    }

    public static void save2Capture(Document document){
        connect();
        save(collection, document);
    }

    public static void save(MongoCollection<Document> collection, Document document){
        for (int i=0;i<3;i++) {
            try {
                Document doc = new Document(CaptureEntity.ID.getCname(), new Document("$eq", document.get(CaptureEntity.ID.getCname())));
                FindIterable<Document> documents = collection.find(doc);
                Document first = documents.first();
                if (first != null) {
                    first.putAll(document);
                    Document update = new Document("$set", first);
                    collection.updateOne(doc, update);
                } else {
                    collection.insertOne(document);
                }
            } catch (Exception e) {
                save(collection, document);
            }
        }
    }

    public static Document find(Document document){
        Document doc = new Document(CaptureEntity.ID.getCname(), new Document("$eq", document.get(CaptureEntity.ID.getCname())));
        FindIterable<Document> documents = collection.find(doc);
        return documents.first();
    }

    public static void main(String[] args) throws FileNotFoundException, MalformedURLException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("");
        String path = url.getPath();
        File file = new File(path + "/config.yml");
        GlobalProxyConfig proxyConfig = Yaml.loadType(file, GlobalProxyConfig.class);
    }

}
