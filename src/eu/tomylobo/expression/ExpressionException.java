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

/**
 * Thrown when there's a problem during any stage of the expression compilation or evaluation.
 *
 * @author TomyLobo
 */
public class ExpressionException extends Exception {
    private static final long serialVersionUID = 1L;

    private final int position;

    public ExpressionException(int position) {
        this.position = position;
    }

    public ExpressionException(int position, String message, Throwable cause) {
        super(message, cause);
        this.position = position;
    }

    public ExpressionException(int position, String message) {
        super(message);
        this.position = position;
    }

    public ExpressionException(int position, Throwable cause) {
        super(cause);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
