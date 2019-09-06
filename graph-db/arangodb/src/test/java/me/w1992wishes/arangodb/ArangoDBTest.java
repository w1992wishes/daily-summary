package me.w1992wishes.arangodb;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoCollection;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.util.MapBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/***Created by mo
 *On 2017/10/31  ***15:55.
 ******/
public class ArangoDBTest {

    private ArangoDatabase db;

    @Before
    public void before() {
        //连接和mongo差不多，本地地址
        ArangoDB arangoDB = new ArangoDB.Builder().host("127.0.0.1", 8529).user("root").password("082608jy").build();
        //数据库
        db = arangoDB.db("_system");
    }

    @Test
    public void insertDocument() {
        BaseDocument document = new BaseDocument();
        document.addAttribute("id", 01);
        document.addAttribute("name", "Demo");
        document.addAttribute("tag", "10");
        db.collection("test").insertDocument(document);
    }

    @Test
    public void findRole() throws Exception {
        try {
            String queryCommand = "for doc in @@collection return  doc";
            AqlQueryOptions options = new AqlQueryOptions();
            options.ttl(1000000);//持续时间
            ArangoCursor<BaseDocument> cursor = db.query(queryCommand,
                    new MapBuilder().put("@collection", "test").get(), options, BaseDocument.class);
            int ii = 0;
            while (cursor.hasNext()) {
                BaseDocument object = cursor.next();
                System.out.println(object.toString());
                System.out.println(ii++);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void insertGraph() {
        //集合
        ArangoCollection writerColl = db.collection("Ewriters");

        BaseDocument writerDocument = new BaseDocument();
        writerDocument.addAttribute("name", "滚开");
        Set<String> books = new HashSet<>();
        books.add("巫师世界");
        books.add("神秘之旅");
        books.add("永恒剑主");
        books.add("剑道真解");
        books.add("极道天魔");
        writerDocument.addAttribute("books", books);
        writerColl.insertDocument(writerDocument);

        ArangoCollection bookColl = db.collection("Ebooks");
        BaseDocument bookDocument1 = new BaseDocument();
        bookDocument1.addAttribute("name", "巫师世界");
        bookDocument1.addAttribute("writer", "滚开");
        bookColl.insertDocument(bookDocument1);

        BaseDocument bookDocument2 = new BaseDocument();
        bookDocument2.addAttribute("name", "神秘之旅");
        bookDocument2.addAttribute("writer", "滚开");
        bookColl.insertDocument(bookDocument2);

        BaseDocument bookDocument3 = new BaseDocument();
        bookDocument3.addAttribute("name", "永恒剑主");
        bookDocument3.addAttribute("writer", "滚开");
        bookColl.insertDocument(bookDocument3);

        BaseDocument bookDocument4 = new BaseDocument();
        bookDocument4.addAttribute("name", "剑道真解");
        bookDocument4.addAttribute("writer", "滚开");
        bookColl.insertDocument(bookDocument4);

        BaseDocument bookDocument5 = new BaseDocument();
        bookDocument5.addAttribute("name", "极道天魔");
        bookDocument5.addAttribute("writer", "滚开");
        bookColl.insertDocument(bookDocument5);
    }


    @Test
    public void getGraphPicture() {
        long time = System.currentTimeMillis() / 1000;
        //集合
        ArangoCollection coll = db.collection("QiandianBooks");

        String queryCommand = "for doc in @@collection return  doc";

        AqlQueryOptions options = new AqlQueryOptions();
        //持续时间
        options.ttl(1000000);
        ArangoCursor<BaseEdgeDocument> cursor = db.query(queryCommand,
                new MapBuilder().put("@collection", "Ebooks").get(), options, BaseEdgeDocument.class);
        while (cursor.hasNext()) {
            BaseEdgeDocument books = cursor.next();
            String writerName = books.getAttribute("writer").toString();
            String _to = books.getId();
            String _from;

            ArangoCursor<BaseEdgeDocument> writers = db.query(queryCommand,
                    new MapBuilder().put("@collection", "Ewriters").get(), options, BaseEdgeDocument.class);
            while (writers.hasNext()) {
                //Document
                BaseEdgeDocument writer = writers.next();
                if (writer.getAttribute("name").equals(writerName)) {
                    _from = writer.getId();
                    BaseEdgeDocument baseDocument = new BaseEdgeDocument();
                    baseDocument.setFrom(_from);
                    baseDocument.setTo(_to);
                    baseDocument.addAttribute("relation", "write");
                    baseDocument.addAttribute("status", "active");
                    baseDocument.addAttribute("updataAt", time);
                    coll.insertDocument(baseDocument);
                    break;
                }
            }

        }
    }
}