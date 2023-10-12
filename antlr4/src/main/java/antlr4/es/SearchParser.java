// Generated from D:/code/myself/daily-summary/antlr4/src/main/java/antrl4/es/SearchParser.g4 by ANTLR 4.13.1
package antlr4.es;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class SearchParser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            SPACE = 1, SPEC_ESSQL_COMMENT = 2, COMMENT_INPUT = 3, LINE_COMMENT = 4, MINUS = 5,
            STAR = 6, COLON = 7, EQ = 8, NE = 9, BOOLOR = 10, BOOLAND = 11, DOT = 12, LBRACKET = 13,
            RBRACKET = 14, LPAREN = 15, RPAREN = 16, COMMA = 17, SEMI = 18, GT = 19, AFTER = 20,
            SINGLE_QUOTE = 21, DOUBLE_QUOTE = 22, REVERSE_QUOTE = 23, UNDERLINE = 24, CHINESE = 25,
            ID = 26, INT = 27, FLOAT = 28, DOTINTEGER = 29, DOTID = 30;
    public static final int
            RULE_prog = 0, RULE_expression = 1;

    private static String[] makeRuleNames() {
        return new String[]{
                "prog", "expression"
        };
    }

    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, null, null, null, null, "'-'", "'*'", null, "'='", "'!='", null,
                null, "'.'", "'['", "']'", "'('", "')'", null, "';'", "'>'", null, "'''",
                "'\"'", "'`'", "'_'"
        };
    }

    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "SPACE", "SPEC_ESSQL_COMMENT", "COMMENT_INPUT", "LINE_COMMENT",
                "MINUS", "STAR", "COLON", "EQ", "NE", "BOOLOR", "BOOLAND", "DOT", "LBRACKET",
                "RBRACKET", "LPAREN", "RPAREN", "COMMA", "SEMI", "GT", "AFTER", "SINGLE_QUOTE",
                "DOUBLE_QUOTE", "REVERSE_QUOTE", "UNDERLINE", "CHINESE", "ID", "INT",
                "FLOAT", "DOTINTEGER", "DOTID"
        };
    }

    private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
    public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;

    static {
        tokenNames = new String[_SYMBOLIC_NAMES.length];
        for (int i = 0; i < tokenNames.length; i++) {
            tokenNames[i] = VOCABULARY.getLiteralName(i);
            if (tokenNames[i] == null) {
                tokenNames[i] = VOCABULARY.getSymbolicName(i);
            }

            if (tokenNames[i] == null) {
                tokenNames[i] = "<INVALID>";
            }
        }
    }

    @Override
    @Deprecated
    public String[] getTokenNames() {
        return tokenNames;
    }

    @Override

    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }

    @Override
    public String getGrammarFileName() {
        return "SearchParser.g4";
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public String getSerializedATN() {
        return _serializedATN;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public SearchParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ProgContext extends ParserRuleContext {
        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
        }

        public TerminalNode STAR() {
            return getToken(SearchParser.STAR, 0);
        }

        public TerminalNode EOF() {
            return getToken(SearchParser.EOF, 0);
        }

        public ProgContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_prog;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SearchParserListener) ((SearchParserListener) listener).enterProg(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SearchParserListener) ((SearchParserListener) listener).exitProg(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SearchParserVisitor)
                return ((SearchParserVisitor<? extends T>) visitor).visitProg(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ProgContext prog() throws RecognitionException {
        ProgContext _localctx = new ProgContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_prog);
        try {
            setState(7);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case LPAREN:
                case ID:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(4);
                    expression(0);
                }
                break;
                case STAR:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(5);
                    match(STAR);
                    setState(6);
                    match(EOF);
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExpressionContext extends ParserRuleContext {
        public ExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_expression;
        }

        public ExpressionContext() {
        }

        public void copyFrom(ExpressionContext ctx) {
            super.copyFrom(ctx);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class EqExprContext extends ExpressionContext {
        public ExpressionContext leftExpr;
        public Token operator;
        public ExpressionContext rightExpr;

        public List<ExpressionContext> expression() {
            return getRuleContexts(ExpressionContext.class);
        }

        public ExpressionContext expression(int i) {
            return getRuleContext(ExpressionContext.class, i);
        }

        public TerminalNode EQ() {
            return getToken(SearchParser.EQ, 0);
        }

        public TerminalNode COLON() {
            return getToken(SearchParser.COLON, 0);
        }

        public TerminalNode NE() {
            return getToken(SearchParser.NE, 0);
        }

        public EqExprContext(ExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SearchParserListener) ((SearchParserListener) listener).enterEqExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SearchParserListener) ((SearchParserListener) listener).exitEqExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SearchParserVisitor)
                return ((SearchParserVisitor<? extends T>) visitor).visitEqExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IdentityExprContext extends ExpressionContext {
        public TerminalNode ID() {
            return getToken(SearchParser.ID, 0);
        }

        public IdentityExprContext(ExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SearchParserListener) ((SearchParserListener) listener).enterIdentityExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SearchParserListener) ((SearchParserListener) listener).exitIdentityExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SearchParserVisitor)
                return ((SearchParserVisitor<? extends T>) visitor).visitIdentityExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class LrExprContext extends ExpressionContext {
        public TerminalNode LPAREN() {
            return getToken(SearchParser.LPAREN, 0);
        }

        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(SearchParser.RPAREN, 0);
        }

        public LrExprContext(ExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SearchParserListener) ((SearchParserListener) listener).enterLrExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SearchParserListener) ((SearchParserListener) listener).exitLrExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SearchParserVisitor)
                return ((SearchParserVisitor<? extends T>) visitor).visitLrExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BoolExprContext extends ExpressionContext {
        public ExpressionContext leftExpr;
        public Token operator;
        public ExpressionContext rightExpr;

        public List<ExpressionContext> expression() {
            return getRuleContexts(ExpressionContext.class);
        }

        public ExpressionContext expression(int i) {
            return getRuleContext(ExpressionContext.class, i);
        }

        public TerminalNode BOOLAND() {
            return getToken(SearchParser.BOOLAND, 0);
        }

        public TerminalNode BOOLOR() {
            return getToken(SearchParser.BOOLOR, 0);
        }

        public BoolExprContext(ExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SearchParserListener) ((SearchParserListener) listener).enterBoolExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SearchParserListener) ((SearchParserListener) listener).exitBoolExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SearchParserVisitor)
                return ((SearchParserVisitor<? extends T>) visitor).visitBoolExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ExpressionContext expression() throws RecognitionException {
        return expression(0);
    }

    private ExpressionContext expression(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
        ExpressionContext _prevctx = _localctx;
        int _startState = 2;
        enterRecursionRule(_localctx, 2, RULE_expression, _p);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(15);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case LPAREN: {
                        _localctx = new LrExprContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;

                        setState(10);
                        match(LPAREN);
                        setState(11);
                        expression(0);
                        setState(12);
                        match(RPAREN);
                    }
                    break;
                    case ID: {
                        _localctx = new IdentityExprContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(14);
                        match(ID);
                    }
                    break;
                    default:
                        throw new NoViableAltException(this);
                }
                _ctx.stop = _input.LT(-1);
                setState(25);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 3, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            setState(23);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 2, _ctx)) {
                                case 1: {
                                    _localctx = new EqExprContext(new ExpressionContext(_parentctx, _parentState));
                                    ((EqExprContext) _localctx).leftExpr = _prevctx;
                                    pushNewRecursionContext(_localctx, _startState, RULE_expression);
                                    setState(17);
                                    if (!(precpred(_ctx, 3)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                                    setState(18);
                                    ((EqExprContext) _localctx).operator = _input.LT(1);
                                    _la = _input.LA(1);
                                    if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 896L) != 0))) {
                                        ((EqExprContext) _localctx).operator = (Token) _errHandler.recoverInline(this);
                                    } else {
                                        if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                        _errHandler.reportMatch(this);
                                        consume();
                                    }
                                    setState(19);
                                    ((EqExprContext) _localctx).rightExpr = expression(4);
                                }
                                break;
                                case 2: {
                                    _localctx = new BoolExprContext(new ExpressionContext(_parentctx, _parentState));
                                    ((BoolExprContext) _localctx).leftExpr = _prevctx;
                                    pushNewRecursionContext(_localctx, _startState, RULE_expression);
                                    setState(20);
                                    if (!(precpred(_ctx, 2)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                                    setState(21);
                                    ((BoolExprContext) _localctx).operator = _input.LT(1);
                                    _la = _input.LA(1);
                                    if (!(_la == BOOLOR || _la == BOOLAND)) {
                                        ((BoolExprContext) _localctx).operator = (Token) _errHandler.recoverInline(this);
                                    } else {
                                        if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                        _errHandler.reportMatch(this);
                                        consume();
                                    }
                                    setState(22);
                                    ((BoolExprContext) _localctx).rightExpr = expression(3);
                                }
                                break;
                            }
                        }
                    }
                    setState(27);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 3, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
        switch (ruleIndex) {
            case 1:
                return expression_sempred((ExpressionContext) _localctx, predIndex);
        }
        return true;
    }

    private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
        switch (predIndex) {
            case 0:
                return precpred(_ctx, 3);
            case 1:
                return precpred(_ctx, 2);
        }
        return true;
    }

    public static final String _serializedATN =
            "\u0004\u0001\u001e\u001d\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001" +
                    "\u0001\u0000\u0001\u0000\u0001\u0000\u0003\u0000\b\b\u0000\u0001\u0001" +
                    "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001" +
                    "\u0010\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
                    "\u0001\u0001\u0005\u0001\u0018\b\u0001\n\u0001\f\u0001\u001b\t\u0001\u0001" +
                    "\u0001\u0000\u0001\u0002\u0002\u0000\u0002\u0000\u0002\u0001\u0000\u0007" +
                    "\t\u0001\u0000\n\u000b\u001e\u0000\u0007\u0001\u0000\u0000\u0000\u0002" +
                    "\u000f\u0001\u0000\u0000\u0000\u0004\b\u0003\u0002\u0001\u0000\u0005\u0006" +
                    "\u0005\u0006\u0000\u0000\u0006\b\u0005\u0000\u0000\u0001\u0007\u0004\u0001" +
                    "\u0000\u0000\u0000\u0007\u0005\u0001\u0000\u0000\u0000\b\u0001\u0001\u0000" +
                    "\u0000\u0000\t\n\u0006\u0001\uffff\uffff\u0000\n\u000b\u0005\u000f\u0000" +
                    "\u0000\u000b\f\u0003\u0002\u0001\u0000\f\r\u0005\u0010\u0000\u0000\r\u0010" +
                    "\u0001\u0000\u0000\u0000\u000e\u0010\u0005\u001a\u0000\u0000\u000f\t\u0001" +
                    "\u0000\u0000\u0000\u000f\u000e\u0001\u0000\u0000\u0000\u0010\u0019\u0001" +
                    "\u0000\u0000\u0000\u0011\u0012\n\u0003\u0000\u0000\u0012\u0013\u0007\u0000" +
                    "\u0000\u0000\u0013\u0018\u0003\u0002\u0001\u0004\u0014\u0015\n\u0002\u0000" +
                    "\u0000\u0015\u0016\u0007\u0001\u0000\u0000\u0016\u0018\u0003\u0002\u0001" +
                    "\u0003\u0017\u0011\u0001\u0000\u0000\u0000\u0017\u0014\u0001\u0000\u0000" +
                    "\u0000\u0018\u001b\u0001\u0000\u0000\u0000\u0019\u0017\u0001\u0000\u0000" +
                    "\u0000\u0019\u001a\u0001\u0000\u0000\u0000\u001a\u0003\u0001\u0000\u0000" +
                    "\u0000\u001b\u0019\u0001\u0000\u0000\u0000\u0004\u0007\u000f\u0017\u0019";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}