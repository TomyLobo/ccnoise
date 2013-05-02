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

package eu.tomylobo.expression.runtime;

import eu.tomylobo.expression.Expression;
import eu.tomylobo.expression.parser.ParserException;

/**
 * An if/else statement or a ternary operator.
 *
 * @author TomyLobo
 */
public class Conditional extends Node {
    RValue condition;
    RValue truePart;
    RValue falsePart;

    public Conditional(int position, RValue condition, RValue truePart, RValue falsePart) {
        super(position);

        this.condition = condition;
        this.truePart = truePart;
        this.falsePart = falsePart;
    }

    @Override
    public double getValue() throws EvaluationException {
        if (condition.getValue() > 0.0) {
            return truePart.getValue();
        }
        else {
            return falsePart == null ? 0.0 : falsePart.getValue();
        }
    }

    @Override
    public char id() {
        return 'I';
    }

    @Override
    public String toString() {
        if (falsePart == null) {
            return "if (" + condition + ") { " + truePart + " }";
        }
        else if (truePart instanceof Sequence || falsePart instanceof Sequence) {
            return "if (" + condition + ") { " + truePart + " } else { " + falsePart + " }";
        }
        else {
            return "(" + condition + ") ? (" + truePart + ") : (" + falsePart + ")";
        }
    }

    @Override
    public RValue optimize() throws EvaluationException {
        final RValue newCondition = condition.optimize();

        if (newCondition instanceof Constant) {
            if (newCondition.getValue() > 0) {
                return truePart.optimize();
            }
            else {
                return falsePart == null ? new Constant(getPosition(), 0.0) : falsePart.optimize();
            }
        }

        return new Conditional(getPosition(), newCondition, truePart.optimize(), falsePart == null ? null : falsePart.optimize());
    }

    @Override
    public RValue bindVariables(Expression expression, boolean preferLValue) throws ParserException {
        condition = condition.bindVariables(expression, false);
        truePart = truePart.bindVariables(expression, false);
        falsePart = falsePart.bindVariables(expression, false);

        return this;
    }
}
