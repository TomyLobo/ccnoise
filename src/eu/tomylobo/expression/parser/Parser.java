/*
 * Expression Parser
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.tomylobo.expression.Identifiable;
import eu.tomylobo.expression.lexer.tokens.IdentifierToken;
import eu.tomylobo.expression.lexer.tokens.KeywordToken;
import eu.tomylobo.expression.lexer.tokens.NumberToken;
import eu.tomylobo.expression.lexer.tokens.OperatorToken;
import eu.tomylobo.expression.lexer.tokens.Token;
import eu.tomylobo.expression.runtime.Break;
import eu.tomylobo.expression.runtime.Conditional;
import eu.tomylobo.expression.runtime.Constant;
import eu.tomylobo.expression.runtime.For;
import eu.tomylobo.expression.runtime.Functions;
import eu.tomylobo.expression.runtime.LValue;
import eu.tomylobo.expression.runtime.RValue;
import eu.tomylobo.expression.runtime.Return;
import eu.tomylobo.expression.runtime.Sequence;
import eu.tomylobo.expression.runtime.SimpleFor;
import eu.tomylobo.expression.runtime.Variable;
import eu.tomylobo.expression.runtime.While;

/**
 * Processes a list of tokens into an executable tree.
 *
 * Tokens can be numbers, identifiers, operators and assorted other characters.
 *
 * @author TomyLobo
 */
public class Parser {
    private final class NullToken extends Token {
        private NullToken(int position) {
            super(position);
        }

        public char id() {
            return '\0';
        }

        public String toString() {
            return "NullToken";
        }
    }

    private final List<Token> tokens;
    private int position = 0;
    private Map<String, RValue> variables;

    private Parser(List<Token> tokens, Map<String, RValue> variables) {
        this.tokens = tokens;
        this.variables = variables;
    }

    public static final RValue parse(List<Token> tokens, Map<String, RValue> variables) throws ParserException {
        return new Parser(tokens, variables).parse();
    }

    private RValue parse() throws ParserException {
        final RValue ret = parseStatements(false);
        if (position < tokens.size()) {
            final Token token = peek();
            throw new ParserException(token.getPosition(), "Extra token at the end of the input: " + token);
        }
        return ret;
    }

    private RValue parseStatements(boolean singleStatement) throws ParserException {
        List<RValue> statements = new ArrayList<RValue>();
        loop: while (true) {
            if (position >= tokens.size()) {
                break;
            }

            final Token current = peek();
            switch (current.id()) {
            case '{':
                consumeCharacter('{');

                statements.add(parseStatements(false));

                consumeCharacter('}');

                if (singleStatement) {
                    break loop;
                }
                break;

            case '}':
                break loop;

            case 'k':
                final String keyword = ((KeywordToken) current).value;
                switch (keyword.charAt(0)) {
                case 'i': { // if
                    ++position;
                    final RValue condition = parseBracket();
                    final RValue truePart = parseStatements(true);
                    final RValue falsePart;

                    if (hasKeyword("else")) {
                        ++position;
                        falsePart = parseStatements(true);
                    } else {
                        falsePart = null;
                    }

                    statements.add(new Conditional(current.getPosition(), condition, truePart, falsePart));
                    break;
                }

                case 'w': { // while
                    ++position;
                    final RValue condition = parseBracket();
                    final RValue body = parseStatements(true);

                    statements.add(new While(current.getPosition(), condition, body, false));
                    break;
                }

                case 'd': { // do
                    ++position;
                    final RValue body = parseStatements(true);

                    consumeKeyword("while");

                    final RValue condition = parseBracket();

                    statements.add(new While(current.getPosition(), condition, body, true));
                    break;
                }

                case 'f': { // for
                    ++position;
                    consumeCharacter('(');
                    int oldPosition = position;
                    final RValue init = parseExpression(true);
                    //if ((init instanceof LValue) && )
                    if (peek().id() == ';') {
                        ++position;
                        final RValue condition = parseExpression(true);
                        consumeCharacter(';');
                        final RValue increment = parseExpression(true);
                        consumeCharacter(')');
                        final RValue body = parseStatements(true);

                        statements.add(new For(current.getPosition(), init, condition, increment, body));
                    } else {
                        position = oldPosition;

                        final Token variableToken = peek();
                        if (!(variableToken instanceof IdentifierToken)) {
                            throw new ParserException(variableToken.getPosition(), "Expected identifier");
                        }

                        // In theory, I should have to create non-existant variables here.
                        // However, the java-for parsing attempt further up already takes care of that :) 
                        RValue variable = variables.get(((IdentifierToken) variableToken).value);
                        if (!(variable instanceof LValue)) {
                            throw new ParserException(variableToken.getPosition(), "Expected variable");
                        }

                        ++position;

                        final Token equalsToken = peek();
                        if (!(equalsToken instanceof OperatorToken) || !((OperatorToken) equalsToken).operator.equals("=")) {
                            throw new ParserException(variableToken.getPosition(), "Expected '=' or a term and ';'");
                        }
                        ++position;

                        final RValue first = parseExpression(true);
                        consumeCharacter(',');
                        final RValue last = parseExpression(true);
                        consumeCharacter(')');
                        final RValue body = parseStatements(true);

                        statements.add(new SimpleFor(current.getPosition(), (LValue) variable, first, last, body));
                    }
                    break;
                }

                case 'b': // break
                    ++position;
                    statements.add(new Break(current.getPosition(), false));
                    break;

                case 'c': // continue
                    ++position;
                    statements.add(new Break(current.getPosition(), true));
                    break;

                case 'r': // return
                    ++position;
                    statements.add(new Return(current.getPosition(), parseExpression(true)));

                    if (peek().id() == ';') {
                        ++position;
                        break;
                    } else {
                        break loop;
                    }

                default:
                    throw new ParserException(current.getPosition(), "Unimplemented keyword '" + keyword + "'");
                }

                if (singleStatement) {
                    break loop;
                }
                break;

            default:
                statements.add(parseExpression(true));

                if (peek().id() == ';') {
                    ++position;
                    if (singleStatement) {
                        break loop;
                    }
                    break;
                } else {
                    break loop;
                }
            }
        }

        switch (statements.size()) {
        case 0:
            if (singleStatement) {
                throw new ParserException(peek().getPosition(), "Statement expected.");
            } else {
                return new Sequence(peek().getPosition());
            }

        case 1:
            return statements.get(0);

        default:
            return new Sequence(peek().getPosition(), statements.toArray(new RValue[statements.size()]));
        }
    }

    private final RValue parseExpression(boolean canBeEmpty) throws ParserException {
        LinkedList<Identifiable> halfProcessed = new LinkedList<Identifiable>();

        // process brackets, numbers, functions, variables and detect prefix operators
        boolean expressionStart = true;
        loop: while (position < tokens.size()) {
            final Token current = peek();

            switch (current.id()) {
            case '0':
                halfProcessed.add(new Constant(current.getPosition(), ((NumberToken) current).value));
                ++position;
                expressionStart = false;
                break;

            case 'i':
                final IdentifierToken identifierToken = (IdentifierToken) current;
                ++position;

                final Token next = peek();
                if (next.id() == '(') {
                    halfProcessed.add(parseFunctionCall(identifierToken));
                }
                else {
                    RValue variable = variables.get(identifierToken.value);
                    if (variable == null) {
                        if (next instanceof OperatorToken && ((OperatorToken) next).operator.equals("=")) {
                            // Ugly hack to make temporary variables work while not sacrificing error reporting.
                            variables.put(identifierToken.value, variable = new Variable(0));
                        } else {
                            throw new ParserException(current.getPosition(), "Variable '" + identifierToken.value + "' not found");
                        }
                    }
                    halfProcessed.add(variable);
                }
                expressionStart = false;
                break;

            case '(':
                halfProcessed.add(parseBracket());
                expressionStart = false;
                break;

            case ',':
            case ')':
            case '}':
            case ';':
                break loop;

            case 'o':
                if (expressionStart) {
                    // Preprocess prefix operators into unary operators
                    halfProcessed.add(new UnaryOperator((OperatorToken) current));
                }
                else {
                    halfProcessed.add(current);
                }
                ++position;
                expressionStart = true;
                break;

            default:
                halfProcessed.add(current);
                ++position;
                expressionStart = false;
                break;
            }
        }

        if (halfProcessed.isEmpty() && canBeEmpty) {
            return new Sequence(peek().getPosition());
        }

        return ParserProcessors.processExpression(halfProcessed);
    }


    private Token peek() {
        if (position >= tokens.size()) {
            return new NullToken(tokens.get(tokens.size() - 1).getPosition() + 1);
        }

        return tokens.get(position);
    }

    private Identifiable parseFunctionCall(IdentifierToken identifierToken) throws ParserException {
        consumeCharacter('(');

        try {
            if (peek().id() == ')') {
                ++position;
                return Functions.getFunction(identifierToken.getPosition(), identifierToken.value);
            }

            List<RValue> args = new ArrayList<RValue>();

            loop: while (true) {
                args.add(parseExpression(false));

                final Token current = peek();
                ++position;

                switch (current.id()) {
                case ',':
                    continue;

                case ')':
                    break loop;

                default:
                    throw new ParserException(current.getPosition(), "Unmatched opening bracket");
                }
            }

            return Functions.getFunction(identifierToken.getPosition(), identifierToken.value, args.toArray(new RValue[args.size()]));
        }
        catch (NoSuchMethodException e) {
            throw new ParserException(identifierToken.getPosition(), "Function '" + identifierToken.value + "' not found", e);
        }
    }

    private final RValue parseBracket() throws ParserException {
        consumeCharacter('(');

        final RValue ret = parseExpression(false);

        consumeCharacter(')');

        return ret;
    }

    private boolean hasKeyword(String keyword) {
        final Token next = peek();
        if (!(next instanceof KeywordToken)) {
            return false;
        }
        return ((KeywordToken) next).value.equals(keyword);
    }

    private void assertCharacter(char character) throws ParserException {
        final Token next = peek();
        if (next.id() != character) {
            throw new ParserException(next.getPosition(), "Expected '" + character + "'");
        }
    }

    private void assertKeyword(String keyword) throws ParserException {
        if (!hasKeyword(keyword)) {
            throw new ParserException(peek().getPosition(), "Expected '" + keyword + "'");
        }
    }

    private void consumeCharacter(char character) throws ParserException {
        assertCharacter(character);
        ++position;
    }

    private void consumeKeyword(String keyword) throws ParserException {
        assertKeyword(keyword);
        ++position;
    }
}
