// Generated from D:/code/myself/daily-summary/antlr4/src/main/java/antrl4/es/SearchParser.g4 by ANTLR 4.13.1
package antlr4.es;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SearchParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SearchParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SearchParser#prog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg(SearchParser.ProgContext ctx);
	/**
	 * Visit a parse tree produced by the {@code eqExpr}
	 * labeled alternative in {@link SearchParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqExpr(SearchParser.EqExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code identityExpr}
	 * labeled alternative in {@link SearchParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentityExpr(SearchParser.IdentityExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lrExpr}
	 * labeled alternative in {@link SearchParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLrExpr(SearchParser.LrExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code boolExpr}
	 * labeled alternative in {@link SearchParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolExpr(SearchParser.BoolExprContext ctx);
}