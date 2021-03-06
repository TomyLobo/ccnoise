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
 * A node in the execution tree of an expression.
 *
 * @author TomyLobo
 */
public abstract class Node implements RValue {
    private final int position;

    public Node(int position) {
        this.position = position;
    }

    @Override
    public abstract String toString();

    public RValue optimize() throws EvaluationException {
        return this;
    }

    @Override
    public final int getPosition() {
        return position;
    }

    @Override
    public RValue bindVariables(Expression expression, boolean preferLValue) throws ParserException {
        return this;
    }
}
