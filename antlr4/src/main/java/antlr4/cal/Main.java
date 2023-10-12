package antlr4.cal;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {

    public static void main(String[] args) {
        CharStream input = CharStreams.fromString("22*3+12\r\n");
        CalcLexer lexer = new CalcLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CalcParser parser = new CalcParser(tokens);
        ParseTree tree = parser.prog(); // parse
        EvalVisitor vt = new EvalVisitor();
        System.out.printf("%s\n", vt.visit(tree));
    }

}
