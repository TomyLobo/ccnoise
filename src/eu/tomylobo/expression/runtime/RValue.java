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
import eu.tomylobo.expression.Identifiable;
import eu.tomylobo.expression.parser.ParserException;

/**
 * A value that can be used on the right side of an assignment.
 *
 * @author TomyLobo
 */
public interface RValue extends Identifiable {
    public double getValue() throws EvaluationException;

    public RValue optimize() throws EvaluationException;

    public RValue bindVariables(Expression expression, boolean preferLValue) throws ParserException;
}
