/*
 * Expression Parser
 * Copyright (C) 2011, 2012, 2013 TomyLobo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package eu.tomylobo.expression.parser;

import eu.tomylobo.expression.lexer.tokens.OperatorToken;

/**
 * The parser uses this pseudo-token to mark operators as unary operators.
 *
 * @author TomyLobo
 */
public class UnaryOperator extends PseudoToken {
    final String operator;

    public UnaryOperator(OperatorToken operatorToken) {
        this(operatorToken.getPosition(), operatorToken.operator);
    }

    public UnaryOperator(int position, String operator) {
        super(position);
        this.operator = operator;
    }

    @Override
    public char id() {
        return 'p';
    }

    @Override
    public String toString() {
        return "UnaryOperator(" + operator + ")";
    }
}
