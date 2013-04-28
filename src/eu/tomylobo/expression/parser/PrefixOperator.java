package eu.tomylobo.expression.parser;

import eu.tomylobo.expression.lexer.tokens.OperatorToken;

/**
 * The parser uses this pseudo-token to mark operators as prefix operators.
 *
 * @author TomyLobo
 */
public class PrefixOperator extends PseudoToken {
    final String operator;

    public PrefixOperator(OperatorToken operatorToken) {
        super(operatorToken.getPosition());
        operator = operatorToken.operator;
    }

    @Override
    public char id() {
        return 'p';
    }

    @Override
    public String toString() {
        return "PrefixOperator(" + operator + ")";
    }
}
