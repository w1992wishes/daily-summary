// 表明SearchParser.g4文件是语法解析器(parser)定义文件
// 同理，语法分析器的名称一定要和文件名保持一致
parser grammar SearchParser;

options {
    // 表示解析token的词法解析器使用SearchLexer
    tokenVocab = SearchLexer;
}

// EOF(end of file)表示文件结束符，这个是Antlr中已经定义好的
prog: expression | STAR EOF;

expression:
    // 表示表达式可以被括号括起来
    // 如果语法后面加上了#{name}，相当于将这个name作为这个语法块的名字，这个#{name}要加都得加上，要不加都不加
    // (country:中国)
    LPAREN expression RPAREN                                                            #lrExpr
    // leftExpr是给定义的语法起的别名(alias)，可有可无，但是有会更好点
    // 因为antlr解析同一语法块的同一类token时，会将他们放在一个list里面
    // 比如下面的语法块，有两个expression，antlr会将他们放在一个列表expressions里
    // 获取第一个expression时需要expressions.get(0)，获取第二个expression时需要expressions.get(1)
    // 如果给第一个expression起了个别名叫leftExpr，给第二个expression起了个别名叫rightExpr
    // 那样在java里面调用时就可以直接调用leftExpr和rightExpr，而不需要指定expressions中的索引(0或1)
    // 这样做的好处是：如果之后添加了新的token，比如在下面语法中间添加一个expression的token
    // 这时如果不使用别名leftExpr，rightExpr就可能需要修改java代码，因为原来rightExpr对应的expression在expressions中索引变为2了
    // 使用别名leftExpr，rightExpr(当然还可以取别的名字)就没有这个问题，使语法文件和生成的java代码更便于维护
    // country:中国
    | leftExpr = expression operator = (EQ |COLON| NE ) rightExpr = expression          #eqExpr
    // (country:中国||country:美国)&&city:北京
    | leftExpr = expression operator = (BOOLAND|BOOLOR) rightExpr = expression          #boolExpr
    // country等字面量
    | ID                                                                                #identityExpr
;