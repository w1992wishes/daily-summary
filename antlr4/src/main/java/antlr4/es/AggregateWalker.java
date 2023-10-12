package antlr4.es;
 
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.StringUtils;
 
/**
 * @descrition 生成遍历聚类条件的抽象语法树的遍历器
 **/
class AggregateWalker {
 
    private String expression;
 
    AggregateWalker(String expression){
        this.expression=expression;
    }
 
    AggregateParser buildAntlrTree(){
        if(StringUtils.isBlank(this.expression)){
            throw new RuntimeException("搜索表达式不能为空!!!");
        }
        CharStream stream= CharStreams.fromString(this.expression);
        SearchLexer lexer=new SearchLexer(stream);
        return new AggregateParser(new CommonTokenStream(lexer));
    }
}