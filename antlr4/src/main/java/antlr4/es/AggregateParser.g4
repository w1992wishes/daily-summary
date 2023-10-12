parser grammar AggregateParser;

options {
    // 聚类的语法分析器也可以使用SearchLexer
    tokenVocab = SearchLexer;
}

expr:
    // 多个聚类条件用分号隔开
    aggClause (SEMI aggClause)*
;
// aggClause表示代表以下聚类的任意一种
aggClause:
    cardinalityAggClause|termsAggClause|termsAfterAggClause|geoBoundingBoxAggClause
;
// 去重值计数 -> (country)
cardinalityAggClause:
    LPAREN ID RPAREN
;
// 桶聚类分页 -> province after 湖南
termsAfterAggClause:
    field = ID AFTER after=ID
;
// 桶聚类嵌套子聚类 -> country>province>city
termsAggClause:
    field = ID (GT termsAggClause)?
;
// 地理边框聚类 -> [coordinate]
geoBoundingBoxAggClause:
    LBRACKET ID RBRACKET
;