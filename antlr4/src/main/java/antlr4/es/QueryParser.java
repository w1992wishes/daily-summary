package antlr4.es;
 
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
/**
 * @descrition 遍历搜索条件和聚类条件的抽象语法树
 **/
public class QueryParser {
 
    public QueryBuilder parse(String expr) {
        //如果表达式为*，则返回全部数据
        if ("*".equals(expr.trim())) {
            return QueryBuilders.matchAllQuery();
        }
        //生成遍历树的实例
        SearchWalker walker = new SearchWalker(expr);
        //调用方法，遍历表达式
        SearchParser searchParser = walker.buildAntlrTree();
        //将搜索表达式转换为查询elasticsearch的querybuilder
        return parseExpressionContext(searchParser.prog().expression());
    }
 
    private QueryBuilder parseExpressionContext(SearchParser.ExpressionContext expressionContext) {
        //如果表达式是被括号包含的话，调用parseLrExprContext
        if (expressionContext instanceof SearchParser.LrExprContext) {
            return parseLrExprContext((SearchParser.LrExprContext) expressionContext);
        } 
        //如果表达式是条件表达式包含与或非的话，调用parseBoolExprContext
        else if (expressionContext instanceof SearchParser.BoolExprContext) {
            return parseBoolExprContext((SearchParser.BoolExprContext) expressionContext);
        } 
        //如果表达式是等式的话，调用parseEqExprContext
        else if (expressionContext instanceof SearchParser.EqExprContext) {
            return parseEqExprContext((SearchParser.EqExprContext) expressionContext);
        } 
        else {
            //不满足上述条件，则抛出异常
            throw new RuntimeException("不支持该查询语法!!!");
        }
    }
 
    //解析括号中的表达式
    private QueryBuilder parseLrExprContext(SearchParser.LrExprContext lrExprContext) {
        SearchParser.ExpressionContext expression = lrExprContext.expression();
        return parseExpressionContext(expression);
    }
 
    private BoolQueryBuilder parseBoolExprContext(SearchParser.BoolExprContext boolExprContext) {
        //解析条件表达式的左半边表达式
        SearchParser.ExpressionContext leftExpr = boolExprContext.expression(0);
        //解析条件表达式的右半边表达式
        SearchParser.ExpressionContext rightExpr = boolExprContext.expression(1);
 
        //将左半边表达式转换成querybuilder
        QueryBuilder leftQuery = parseExpressionContext(leftExpr);
        //将右半边表达式转换为querybuilder
        QueryBuilder rightQuery = parseExpressionContext(rightExpr);
        //如果表达式表示的是且的关系
        if (boolExprContext.BOOLAND() != null) {
            return QueryBuilders.boolQuery().must(leftQuery).must(rightQuery);
        } else {
            //如果表达式表示的是或的关系
            return QueryBuilders.boolQuery().should(leftQuery).should(rightQuery);
        }
    }
 
    private QueryBuilder parseEqExprContext(SearchParser.EqExprContext eqExprContext) {
        String field, value;
        //如果左半边的字段值
        if (eqExprContext.leftExpr instanceof SearchParser.IdentityExprContext) {
            //获取该字段实际的映射字段
            field = parseIdentityContext((SearchParser.IdentityExprContext) eqExprContext.leftExpr);
        } else {
            //否则抛出异常
            throw new RuntimeException("不支持该查询语法!!!");
        }
        //如果右半边是个值
        if (eqExprContext.rightExpr instanceof SearchParser.IdentityExprContext) {
            //则不动
            value = parseIdentityContext((SearchParser.IdentityExprContext) eqExprContext.rightExpr);
        } else {
            //否则抛出异常
            throw new RuntimeException("不支持该查询语法!!!");
        }
        //如果条件表达式的关联条件是不等于
        if (eqExprContext.NE() != null) {
            //则调用must_not
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(field, value));
        } 
        //如果条件表达式的关联条件是冒号或者等于号
        return QueryBuilders.termQuery(field, value);
    }
 
    //解析字段名和字段值
    private String parseIdentityContext(SearchParser.IdentityExprContext identityExprContext) {
        return identityExprContext.getText();
    }
 
    //解析聚类表达式
    public List<AggregationBuilder> parseAggregationExpr(String expr) {
        //生成聚类遍历树实例
        AggregateWalker walker = new AggregateWalker(expr);
        //遍历聚类表达式
        AggregateParser aggregateParser = walker.buildAntlrTree();
        //将聚类表达式转换成elasticsearch的aggregationbuilder的列表
        return parseAggregationContext(aggregateParser.expr().aggClause());
    }
 
    private List<AggregationBuilder> parseAggregationContext(List<AggregateParser.AggClauseContext> aggClauseContexts) {
        //创建aggregationbuilder空列表
        List<AggregationBuilder> aggregationBuilders = new ArrayList<>(0);
        //是否支持聚类分页，默认值为false
        boolean hasCompositeAggregation = false;
        //创建CompositeValuesSourceBuilder的列表
        List<CompositeValuesSourceBuilder<?>> compositeValuesSourceBuilders = new ArrayList<>(0);
        //创建afterkey的map
        Map<String, Object> afterKeys = new HashMap<>(0);
        //对聚类表达式进行遍历
        for (AggregateParser.AggClauseContext aggClauseContext : aggClauseContexts) {
            //如果聚类表达式形如(ip)，则调用AggregationBuilders.cardinality方法，并添加到聚类列表中
            if (aggClauseContext.cardinalityAggClause() != null) {
                //获取聚类字段名
                String field = aggClauseContext.cardinalityAggClause().ID().getText();
                //将转换后的字段掺入AggregationBuilders.cardinality方法中
                aggregationBuilders.add(AggregationBuilders.cardinality(field + "_cardinality").field(field));
            }
            //如果聚类表达式形如country after 湖南，则调用CompositeValuesSourceBuilder进行聚类分页
            else if (aggClauseContext.termsAfterAggClause() != null) {
                //获取字段值
                String field = aggClauseContext.termsAfterAggClause().field.getText();
                //将是否聚类分页字段设置为true
                hasCompositeAggregation = true;
                //获取after的值
                String after = aggClauseContext.termsAfterAggClause().after.getText();
                //设置查询的名称
                String compositeField = aggClauseContext.termsAfterAggClause().field.getText() + "_composite";
                //生成聚类分页的实例
                CompositeValuesSourceBuilder sourceBuilder = new TermsValuesSourceBuilder(compositeField).field(field);
                //添加到聚类分页的列表中
                compositeValuesSourceBuilders.add(sourceBuilder);
                afterKeys.put(compositeField, after);
            }
            //如果聚类表达式形如[coordinate]，则进行地理边框聚类
             else if (aggClauseContext.geoBoundingBoxAggClause() != null) {
                 //获取地理字段的值
                String field = aggClauseContext.geoBoundingBoxAggClause().ID().getText();
                //添加到聚类列表中
                aggregationBuilders.add(AggregationBuilders.geoBounds(field + "_geoBound").field(field));
            } 
            //如果聚类表达式形如ip，则对其进行桶聚类
            else if (aggClauseContext.termsAggClause() != null) {
                //将转换后的桶聚类实例添加到聚类列表中
                aggregationBuilders.add(parseTermsAggregationContext(aggClauseContext.termsAggClause()));
            }
        }
        //如果请求中存在聚类分页
        if (hasCompositeAggregation) {
            CompositeAggregationBuilder composite = AggregationBuilders.composite("composites", compositeValuesSourceBuilders).size(15);
            //则设置聚类分页的值，但每次请求只支持一个字段进行分页
            composite.aggregateAfter(afterKeys);
            aggregationBuilders.add(composite);
            if (compositeValuesSourceBuilders.size() > 1) {
                throw new RuntimeException("暂不支持多字段分页功能");
            }
        }
        return aggregationBuilders;
    }
 
    //解析桶聚类表达式，形如ip
    private AggregationBuilder parseTermsAggregationContext(AggregateParser.TermsAggClauseContext termsAggClauseContext) {
        //获取字段名
        String field = termsAggClauseContext.field.getText();
        //生成桶聚类实例
        AggregationBuilder aggregationBuilder = AggregationBuilders.terms(field + "_terms").field(field).size(5000);
        if (termsAggClauseContext.termsAggClause() != null) {
            //如果桶聚类下有子聚类，则添加子聚类
            aggregationBuilder.subAggregation(parseTermsAggregationContext(termsAggClauseContext.termsAggClause()));
        }
        return aggregationBuilder;
    }
}