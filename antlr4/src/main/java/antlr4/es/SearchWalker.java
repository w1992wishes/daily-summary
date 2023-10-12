package antlr4.es;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.StringUtils;

/**
 * @descrition 生成遍历搜索条件的抽象语法树的遍历器
 **/
class SearchWalker{

    private String expression;

    SearchWalker(String expression){
        this.expression=expression;
    }

    SearchParser buildAntlrTree(){
        if(StringUtils.isBlank(this.expression)){
            throw new RuntimeException("搜索表达式不能为空!!!");
        }
        CharStream stream= CharStreams.fromString(this.expression);
        SearchLexer lexer=new SearchLexer(stream);
        return new SearchParser(new CommonTokenStream(lexer));
    }
}