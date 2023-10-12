package antlr4.es;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        QueryParser queryParser=new QueryParser();
        //将搜索条件转换成QueryBuilder
        QueryBuilder queryBuilder = queryParser.parse("country:中国,province:湖南,city:张家界");
        //然后将queryBuilder传给Elasticsearch进行查询

        //将聚类条件转换成List<AggregationBuilder>
        List<AggregationBuilder> aggregationBuilders = queryParser.parseAggregationExpr("country,(country),country>province>city,province after 湖南");
        //然后将aggregationBuilders传给Elasticsearch进行聚类
    }
}
