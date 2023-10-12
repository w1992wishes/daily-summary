// 表明SearchLexer.g4文件是词法分析器(lexer)定义文件
// 词法分析器的名称一定要和文件名保持一致
lexer grammar SearchLexer;

channels {
    ESQLCOMMENT,
    ERRORCHANNEL
}

//SKIP 当Antlr解析到下面的代码时，会选择跳过
// 遇到 \t\r\n 会忽略
SPACE: [ \t\r\n]+ -> channel(HIDDEN);
// 遇到 /*!  */ 会当作注释跳过
SPEC_ESSQL_COMMENT: '/*!' .+? '*/' -> channel(ESQLCOMMENT);
// 遇到 /* */ 会当作注释跳过
COMMENT_INPUT: '/*' .*? '*/' -> channel(HIDDEN);
// 遇到 -- 会当作注释跳过
// 遇到 # 会当作注释跳过
LINE_COMMENT: (
        ('-- ' | '#') ~[\r\n]* ('\r'? '\n' | EOF)
        | '--' ('\r'? '\n' | EOF)
    ) -> channel(HIDDEN);

// 定义Token，模式为 {field}:{value}
MINUS: '-';  //使MINUS和-等价，以下同理
STAR: '*';

COLON: ':'|'\uFF1A';
EQ: '=';
NE: '!=';
BOOLOR: '||'|'|';  // 使BOOLOR与||或者|等价
BOOLAND: '&&'|COMMA|'&';

//CONSTRUCTORS

DOT: '.' -> mode(AFTER_DOT);
LBRACKET: '[';
RBRACKET: ']';
LPAREN: '(';
RPAREN: ')';
COMMA: ','|'\uFF0C';  // 使COMMA与,或，等价(\uFF0C表示，的unicode编码)
SEMI: ';';
GT: '>';

// 这里和以下代码等价
// AFTER: 'after'  但是这种代码只能表示小写的after，是大小写区分的，这样不好
// 通过下面定义的fragment，将AFTER用A F T E R表示，一定要每个字母空一格，就可以不区分大小写了
// 所有语法的关键字都建议使用这种方式声明
AFTER: A F T E R;
SINGLE_QUOTE: '\'';
DOUBLE_QUOTE: '"';
REVERSE_QUOTE: '`';
UNDERLINE: '_';
CHINESE: '\u4E00'..'\u9FA5';  //表示所有中文的unicode编码，以支持中文
ID: (CHINESE|ID_LETTER|DOT|MINUS|UNDERLINE|INT|FLOAT|REVERSE_QUOTE|DOUBLE_QUOTE|SINGLE_QUOTE)+;
// ? 表示可有可无
// + 表示至少有一个
// | 表示或的关系
// * 表示有0或者多个
INT: MINUS? DEC_DIGIT+;
FLOAT: (MINUS? DEC_DIGIT+ DOT DEC_DIGIT+)| (MINUS? DOT DEC_DIGIT+);
// 使用DEC_DIGIT代表0到9之间的数字
fragment DEC_DIGIT: [0-9];
// 使用ID_LETTER代表a-z的大写小写字母和_
fragment ID_LETTER: [a-zA-Z]| UNDERLINE;
// 表示用A代表a和A，这样就可以不区分大小写了，以下同理
fragment A: [aA];
fragment B: [bB];
fragment C: [cC];
fragment D: [dD];
fragment E: [eE];
fragment F: [fF];
fragment G: [gG];
fragment H: [hH];
fragment I: [iI];
fragment J: [jJ];
fragment K: [kK];
fragment L: [lL];
fragment M: [mM];
fragment N: [nN];
fragment O: [oO];
fragment P: [pP];
fragment Q: [qQ];
fragment R: [rR];
fragment S: [sS];
fragment T: [tT];
fragment U: [uU];
fragment V: [vV];
fragment W: [wW];
fragment X: [xX];
fragment Y: [yY];
fragment Z: [zZ];
mode AFTER_DOT;
//DEFAULT_MODE是Antlr中默认定义好的mode
DOTINTEGER: ( '0' | [1-9] [0-9]*) -> mode(DEFAULT_MODE);
DOTID: [_a-zA-Z] [_a-zA-Z0-9]* -> mode(DEFAULT_MODE);