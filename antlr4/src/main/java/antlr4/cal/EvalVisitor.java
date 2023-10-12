package antlr4.cal;

public class EvalVisitor extends CalcBaseVisitor<Integer> {
    @Override
    public Integer visitMulDiv(CalcParser.MulDivContext ctx) {
        System.out.println("Left expression: " + ctx.expr(0).getText());
        System.out.println("Right expression: " + ctx.expr(1).getText());
        Integer left = ctx.expr(0).accept(this);
        Integer right = ctx.expr(1).accept(this);
        if (ctx.op.getType() == CalcParser.MUL) {
            return left * right;
        } else {
            return left / right;
        }
    }

    @Override
    public Integer visitAddSub(CalcParser.AddSubContext ctx) {
        System.out.println("Left expression: " + ctx.expr(0).getText());
        System.out.println("Right expression: " + ctx.expr(1).getText());
        Integer left = ctx.expr(0).accept(this);
        Integer right = ctx.expr(1).accept(this);
        if (ctx.op.getType() == CalcParser.ADD) {
            return left + right;
        } else {
            return left - right;
        }
    }

}
