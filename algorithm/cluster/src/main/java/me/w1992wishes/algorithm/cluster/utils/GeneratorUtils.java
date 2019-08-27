package me.w1992wishes.algorithm.cluster.utils;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: w1992wishes
 * @Date: 2018/5/15 13:14
 */
public class GeneratorUtils {
    private Connection conn = null;

    public Connection getConn(){
        PropertiesUtils.loadFile("db.properties");
        String driver = PropertiesUtils.getPropertyValue("driver");
        String url = PropertiesUtils.getPropertyValue("url");
        String username  = PropertiesUtils.getPropertyValue("username");
        String password = PropertiesUtils.getPropertyValue("password");

        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url,username,password);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
        return conn;
    }
    public  void close(){
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTableName(String sql, String column) {
        // sql语句
        // 获取到连接
        Connection conn = getConn();
        PreparedStatement pst = null;
        // 定义一个list用于接受数据库查询到的内容
        List<String> list = new ArrayList<String>();
        try {
            pst = (PreparedStatement) conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                // 将查询出的内容添加到list中，其中userName为数据库中的字段名称
                list.add(rs.getString(column));
            }
        } catch (Exception e) {
        }
        close();
        return list;
    }

    public List<String> getLines(String tableName) throws Exception {
        Connection conn = getConn();
        PreparedStatement pst = null;
        pst = (PreparedStatement) conn.prepareStatement("select face_feature, image_data from " + tableName);
        ResultSet rs = pst.executeQuery();
        List<String> lines = new ArrayList<>();
        while (rs.next()) {
            byte[] features = blobToBytes(rs.getBlob("face_feature"));
            StringBuilder newLine = new StringBuilder();
            for (int i=0; i<128; i++){
                float one = byteToFloat(features, 4*i);
                newLine.append(one).append( (i==127) ? "," :"_");
            }
            newLine.append("http://192.168.11.241").append(rs.getString("image_data"));
            lines.add(newLine.toString());
        }
        return lines;
    }

    private void writeDate(String file, List<String> lines){
        File csv = new File(file); // CSV数据文件
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true)); // 附加
            for (String line : lines){
                bw.write(line);
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 字节转换为浮点
     *
     * @param bytes 字节（至少4个字节）
     * @param offset 开始位置
     * @return 转换后浮点数
     */
    public static float byteToFloat(byte[] bytes, int offset) {
        return Float.intBitsToFloat((bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3]) << 24));
    }

    public static void main(String[] args) throws Exception {
        GeneratorUtils generatorUtil = new GeneratorUtils();
        generatorUtil.writeDate("data.csv", generatorUtil.getLines("t_face_143"));
    }

    public static byte[] blobToBytes(Blob blob) throws Exception {
        BufferedInputStream is = null;
        try {
            is = new BufferedInputStream(blob.getBinaryStream());
            byte[] bytes = new byte[(int) blob.length()];
            int len = bytes.length;
            int offset = 0;
            int read = 0;
            while (offset < len && (read = is.read(bytes, offset, len - offset)) >= 0) {
                offset += read;
            }
            return bytes;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                is.close();
                is = null;
            } catch (IOException e) {
                return null;
            }
        }
    }
}
