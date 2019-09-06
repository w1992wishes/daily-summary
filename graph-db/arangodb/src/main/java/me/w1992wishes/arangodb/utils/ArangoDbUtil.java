package me.w1992wishes.arangodb.utils;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import com.arangodb.util.MapBuilder;

import java.util.Collection;
import java.util.Map;

/**
 * @author Administrator
 */
public class ArangoDbUtil {

    private static ArangoDB arangoDB;

    private static ArangoDatabase[] dbs;

    public static void shutDown() {
        arangoDB.shutdown();
    }

    protected static void initArango(String host, int port, String username, String password) {
        // 连接Arangodb服务器
        if (arangoDB == null) {
            synchronized (ArangoDbUtil.class) {
                if (arangoDB == null) {
                    arangoDB = new ArangoDB.Builder().host(host, port).user(username).password(password).build();
                }
            }
        }
    }

    protected static void initDbs() {
        if (dbs == null) {
            synchronized (ArangoDbUtil.class) {
                if (dbs == null) {
                    Collection<String> dbNames = arangoDB.getDatabases();
                    //判断database是否已经存在，不存在就新创建
                    int index = dbNames.size();
                    dbs = new ArangoDatabase[index];
                    int i = 0;
                    for (String dbName : dbNames) {
                        dbs[i++] = arangoDB.db(dbName);
                    }
                }
            }
        }
    }

    protected static void insert(int dbIndex, String doc, BaseDocument insertDoc) {
        String insertCmmd = "insert @insertDoc into @@doc";
        dbs[dbIndex].query(insertCmmd, new MapBuilder().put("insertDoc", insertDoc).put("@doc", doc).get(), null, null);
    }

    protected static void update(int dbIndex, String doc, String key, BaseDocument updateDoc) {
        String updateCmmd = "update {_key:@key} with @updateDoc into @@doc";
        dbs[dbIndex].query(updateCmmd, new MapBuilder().put("key", key).put("updateDoc", updateDoc).put("@doc", doc).get(), null, null);
    }

    protected static void upsert(int dbIndex, String doc, BaseDocument upsertDoc, BaseDocument insertDoc, BaseDocument updateDoc) {
        String upsertCmmd = "upsert @upsertDoc insert @insertDoc update @updateDoc in @doc  OPTIONS { keepNull: false }";
        dbs[dbIndex].query(upsertCmmd, new MapBuilder().put("upsertDoc", upsertDoc).put("insertDoc", insertDoc).put("updateDoc", updateDoc).put("doc", doc).get(), null, null);
    }

    public ArangoCursor<BaseDocument> query(int dbIndex, String query, Map<String, Object> params) {
        return dbs[dbIndex].query(query, params, null, BaseDocument.class);
    }
}