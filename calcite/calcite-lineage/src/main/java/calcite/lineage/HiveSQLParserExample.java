package calcite.lineage;

import org.apache.hadoop.hive.ql.parse.ParseException;
import org.apache.hadoop.hive.ql.parse.ParseDriver;
import org.apache.hadoop.hive.ql.parse.ASTNode;

public class HiveSQLParserExample {
    public static void main(String[] args) {
        // 定义待解析的 Hive 查询
        String hiveQuery = "WITH cte AS (SELECT col1 FROM table1) " +
                           "INSERT INTO table2 SELECT col1 FROM cte";

        // 创建 ParseDriver
        ParseDriver parseDriver = new ParseDriver();

        try {
            // 解析 Hive 查询
            ASTNode astNode = parseDriver.parse(hiveQuery);

            // 处理解析得到的 ASTNode
            // 这里可以根据具体需求，进一步处理解析得到的抽象语法树
            // 例如，可以遍历 ASTNode，提取表名、字段名等信息
            // 这里只是简单示例，打印解析得到的 ASTNode
            System.out.println(astNode.toStringTree());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}