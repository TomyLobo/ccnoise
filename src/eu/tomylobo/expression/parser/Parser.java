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
import eu.tomylobo.expression.lexer.tokens.CharacterToken;
import eu.tomylobo.expression.lexer.tokens.IdentifierToken;
import eu.tomylobo.expression.lexer.tokens.NumberToken;
import eu.tomylobo.expression.lexer.tokens.OperatorToken;
import eu.tomylobo.expression.lexer.tokens.Token;
import eu.tomylobo.expression.runtime.Constant;
import eu.tomylobo.expression.runtime.Functions;
import eu.tomylobo.expression.runtime.RValue;
import eu.tomylobo.expression.runtime.Variable;

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
        final RValue ret = parseInternal(true);
        if (position < tokens.size()) {
            final Token token = peek();
            throw new ParserException(token.getPosition(), "Extra token at the end of the input: " + token);
        }
        return ret;
    }

    private final RValue parseInternal(boolean isStatement) throws ParserException {
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
                        if (next instanceof OperatorToken && ((OperatorToken)next).operator.equals("=")) {
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

            case '{':
                halfProcessed.add(parseBlock());
                halfProcessed.add(new CharacterToken(-1, ';'));
                expressionStart = false;
                break;

            case ',':
            case ')':
            case '}':
                break loop;

            case 'o':
                if (expressionStart) {
                    halfProcessed.add(new PrefixOperator((OperatorToken) current));
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

        if (isStatement) {
            return ParserProcessors.processStatement(halfProcessed);
        }
        else {
            return ParserProcessors.processExpression(halfProcessed);
        }
    }


    private Token peek() {
        if (position >= tokens.size()) {
            return new NullToken(tokens.get(tokens.size() - 1).getPosition() + 1);
        }

        return tokens.get(position);
    }

    private Identifiable parseFunctionCall(IdentifierToken identifierToken) throws ParserException {
        if (peek().id() != '(') {
            throw new ParserException(peek().getPosition(), "Unexpected character in parseFunctionCall");
        }
        ++position;

        try {
            if (peek().id() == ')') {
                return Functions.getFunction(identifierToken.getPosition(), identifierToken.value);
            }

            List<RValue> args = new ArrayList<RValue>();

            loop: while (true) {
                args.add(parseInternal(false));

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
            throw new ParserException(identifierToken.getPosition(), "Function not found", e);
        }
    }

    private final RValue parseBracket() throws ParserException {
        if (peek().id() != '(') {
            throw new ParserException(peek().getPosition(), "Unexpected character in parseBracket");
        }
        ++position;

        final RValue ret = parseInternal(false);

        if (peek().id() != ')') {
            throw new ParserException(peek().getPosition(), "Unmatched opening bracket");
        }
        ++position;

        return ret;
    }

    private final RValue parseBlock() throws ParserException {
        if (peek().id() != '{') {
            throw new ParserException(peek().getPosition(), "Unexpected character in parseBlock");
        }
        ++position;

        final RValue ret = parseInternal(true);

        if (peek().id() != '}') {
            throw new ParserException(peek().getPosition(), "Unmatched opening brace");
        }
        ++position;

        return ret;
    }
}
