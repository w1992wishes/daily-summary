package me.w1992wishes.hbase.common.dao;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UsersDAO {

    public static final byte[] TABLE_NAME = Bytes.toBytes("users");
    public static final byte[] INFO_FAM = Bytes.toBytes("info");

    public static final byte[] USER_COL = Bytes.toBytes("user");
    public static final byte[] NAME_COL = Bytes.toBytes("name");
    public static final byte[] EMAIL_COL = Bytes.toBytes("email");
    public static final byte[] PASS_COL = Bytes.toBytes("password");
    public static final byte[] TWEETS_COL = Bytes.toBytes("tweet_count");

    public static final byte[] HAMLET_COL = Bytes.toBytes("hamlet_tag");

    private static final Logger log = LoggerFactory.getLogger(UsersDAO.class);

    private Connection conn;

    public UsersDAO(Connection conn) {
        this.conn = conn;
    }

    private static Get mkGet(String user) {
        log.debug("Creating Get for {}", user);

        Get g = new Get(Bytes.toBytes(user));
        g.addFamily(INFO_FAM);
        return g;
    }

    private static Put mkPut(User u) {
        log.debug("Creating Put for {}", u);

        Put p = new Put(Bytes.toBytes(u.user));
        p.addColumn(INFO_FAM, USER_COL, Bytes.toBytes(u.user));
        p.addColumn(INFO_FAM, NAME_COL, Bytes.toBytes(u.name));
        p.addColumn(INFO_FAM, EMAIL_COL, Bytes.toBytes(u.email));
        p.addColumn(INFO_FAM, PASS_COL, Bytes.toBytes(u.password));
        return p;
    }

    public static Put mkPut(String username,
                            byte[] fam,
                            byte[] qual,
                            byte[] val) {
        Put p = new Put(Bytes.toBytes(username));
        p.addColumn(fam, qual, val);
        return p;
    }

    private static Delete mkDel(String user) {
        log.debug("Creating Delete for {}", user);

        return new Delete(Bytes.toBytes(user));
    }

    private static Scan mkScan() {
        Scan s = new Scan();
        s.addFamily(INFO_FAM);
        return s;
    }

    public void addUser(String user,
                        String name,
                        String email,
                        String password)
            throws IOException {

        Table users = conn.getTable(TableName.valueOf(TABLE_NAME));

        Put p = mkPut(new User(user, name, email, password));
        users.put(p);

        users.close();
    }

    public me.w1992wishes.hbase.common.model.User getUser(String user)
            throws IOException {
        Table users = conn.getTable(TableName.valueOf(TABLE_NAME));

        Get g = mkGet(user);
        Result result = users.get(g);
        if (result.isEmpty()) {
            log.info("user {} not found.", user);
            return null;
        }

        User u = new User(result);
        users.close();
        return u;
    }

    public void deleteUser(String user) throws IOException {
        Table users = conn.getTable(TableName.valueOf(TABLE_NAME));

        Delete d = mkDel(user);
        users.delete(d);

        users.close();
    }

    public List<me.w1992wishes.hbase.common.model.User> getUsers()
            throws IOException {
        Table users = conn.getTable(TableName.valueOf(TABLE_NAME));

        ResultScanner results = users.getScanner(mkScan());
        ArrayList<me.w1992wishes.hbase.common.model.User> ret
                = new ArrayList<>();
        for (Result r : results) {
            ret.add(new User(r));
        }

        users.close();
        return ret;
    }

    public long incTweetCount(String user) throws IOException {
        Table users = conn.getTable(TableName.valueOf(TABLE_NAME));

        long ret = users.incrementColumnValue(Bytes.toBytes(user),
                INFO_FAM,
                TWEETS_COL,
                1L);

        users.close();
        return ret;
    }

    private static class User
            extends me.w1992wishes.hbase.common.model.User {
        private User(Result r) {
            this(r.getValue(INFO_FAM, USER_COL),
                    r.getValue(INFO_FAM, NAME_COL),
                    r.getValue(INFO_FAM, EMAIL_COL),
                    r.getValue(INFO_FAM, PASS_COL),
                    r.getValue(INFO_FAM, TWEETS_COL) == null
                            ? Bytes.toBytes(0L)
                            : r.getValue(INFO_FAM, TWEETS_COL));
        }

        private User(byte[] user,
                     byte[] name,
                     byte[] email,
                     byte[] password,
                     byte[] tweetCount) {
            this(Bytes.toString(user),
                    Bytes.toString(name),
                    Bytes.toString(email),
                    Bytes.toString(password));
            this.tweetCount = Bytes.toLong(tweetCount);
        }

        private User(String user,
                     String name,
                     String email,
                     String password) {
            this.user = user;
            this.name = name;
            this.email = email;
            this.password = password;
        }
    }
}
