package eu.tomylobo.expression.parser;

import eu.tomylobo.expression.lexer.tokens.OperatorToken;

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
