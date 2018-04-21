package lee.study.proxyee.util;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;
import lee.study.proxyee.pojo.CaptureEntity;
import org.bson.Document;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MongoUtil {

    private static MongoClient mongoClient = null;

    private static String HOST = "10.158.192.8";
    private static int PORT = 15001;
    private static String USER = "scan";
    private static String PASSWD = "scanlol66";
    private static String DATABASE = "proxyee";

    private final static String TABLE = "capture";

    private static MongoCollection<Document> collection = null;

    static {
        mongoClient = getMonGoConnection();
    }

    private static MongoClient getMonGoConnection() {
        if (mongoClient == null) {
            List<ServerAddress> addrs = new ArrayList<ServerAddress>();
            try {
                try (InputStream stream = MongoUtil.class.getClassLoader().getResourceAsStream("mongo.properties");) {
                    Properties prop = new Properties();
                    if (stream!=null) {
                        prop.load(stream);
                        HOST = prop.getProperty("mongo.host");
                        PORT = Integer.valueOf(prop.getProperty("mongo.port"));
                        USER = prop.getProperty("mongo.user");
                        PASSWD = prop.getProperty("mongo.passwd");
                        DATABASE = prop.getProperty("mongo.database");
                    }
                    ServerAddress serverAddress = new ServerAddress(HOST, PORT);
                    addrs.add(serverAddress);
                }
            }catch (Exception e){
                System.out.println("Failed to Connect to mongo, Cause by : " + e);
            }

            MongoCredential credential = MongoCredential.createScramSha1Credential(USER, DATABASE, PASSWD.toCharArray());
            List<MongoCredential> credentials = new ArrayList<MongoCredential>();
            credentials.add(credential);

//            mongoClient = new MongoClient(addrs, credentials);
            mongoClient = new MongoClient( HOST, PORT );
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
                    document.putAll(first);
                    Document update = new Document("$set", document);
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

    public static void main(String[] args){
        connect();
        FindIterable<Document> documents = collection.find(new Document(CaptureEntity.ID.getCname(), new Document("$eq", "1524228250277-78a6762a-c04d-43fb-9ebe-f6dda2ad8e45")));
        FindIterable<Document> docs = collection.find(new Document(CaptureEntity.ID.getCname(), new Document("$eq", documents.first().get(CaptureEntity.ID.getCname()))));
        System.out.println(docs.first());
    }

}
