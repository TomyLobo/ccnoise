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

package eu.tomylobo.expression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.tomylobo.expression.lexer.Lexer;
import eu.tomylobo.expression.lexer.tokens.Token;
import eu.tomylobo.expression.parser.Parser;
import eu.tomylobo.expression.parser.ParserException;
import eu.tomylobo.expression.runtime.Constant;
import eu.tomylobo.expression.runtime.EvaluationException;
import eu.tomylobo.expression.runtime.RValue;
import eu.tomylobo.expression.runtime.Variable;

/**
 * Compiles and evaluates expressions.
 *
 * Supported operators:
 * Logical: &&, ||, ! (unary)
 * Bitwise: ~ (unary), >>, << 
 * Arithmetic: +, -, *, /, % (modulo), ^ (power), - (unary), --, ++ (prefix only)
 * Comparison: <=, >=, >, <, ==, !=, ~= (near)
 *
 * Supported functions: abs, acos, asin, atan, atan2, cbrt, ceil, cos, cosh, exp, floor, ln, log, log10, max, max, min, min, rint, round, sin, sinh, sqrt, tan, tanh
 *
 * Constants: e, pi
 *
 * To compile an equation, run <code>Expression.compile("expression here", "var1", "var2"...)</code>
 * If you wish to run the equation multiple times, you can then optimize it, by calling myExpression.optimize();
 * You can then run the equation as many times as you want by calling myExpression.evaluate(var1, var2...)
 * You do not need to pass values for all variables specified while compiling.
 * To query variables after evaluation, you can use myExpression.getVariable("variable name").
 * To get a value out of these, use myVariable.getValue()
 *
 * Variables are also supported and can be set either by passing values to <code>evaluate</code>
 *
 * @author TomyLobo
 */
public class Expression {
    private final Map<String, RValue> variables = new HashMap<String, RValue>();
    private final String[] variableNames;
    private RValue root;

    public static Expression compile(String expression, String... variableNames) throws ExpressionException {
        return new Expression(expression, variableNames);
    }

    private Expression(String expression, String... variableNames) throws ExpressionException {
        this(Lexer.tokenize(expression), variableNames);
    }

    private Expression(List<Token> tokens, String... variableNames) throws ParserException {
        this.variableNames = variableNames;
        variables.put("e", new Constant(-1, Math.E));
        variables.put("pi", new Constant(-1, Math.PI));
        for (String variableName : variableNames) {
            variables.put(variableName, new Variable(0));
        }

        root = Parser.parse(tokens, variables);
    }

    public double evaluate(double... values) throws EvaluationException {
        for (int i = 0; i < values.length; ++i) {
            final String variableName = variableNames[i];
            final RValue invokable = variables.get(variableName);
            if (!(invokable instanceof Variable)) {
                throw new EvaluationException(invokable.getPosition(), "Tried to assign constant " + variableName + ".");
            }

            ((Variable) invokable).value = values[i];
        }

        return root.getValue();
    }

    public void optimize() throws EvaluationException {
        root = root.optimize();
    }

    @Override
    public String toString() {
        return root.toString();
    }

    public RValue getVariable(String name) {
        return variables.get(name);
    }
}
