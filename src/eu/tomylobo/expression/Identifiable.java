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

package eu.tomylobo.expression;

/**
 * A common superinterface for everything passed to parser processors.
 *
 * @author TomyLobo
 */
public interface Identifiable {
    /**
     * Returns a character that helps identify the token, pseudo-token or invokable in question.
     *
     * <pre>
     * Tokens:
     * i - IdentifierToken
     * 0 - NumberToken
     * o - OperatorToken
     * \0 - NullToken
     * CharacterTokens are returned literally
     *
     * PseudoTokens:
     * p - UnaryOperator
     * V - UnboundVariable
     *
     * Nodes:
     * c - Constant
     * v - Variable
     * f - Function
     * l - LValueFunction
     * s - Sequence
     * I - Conditional
     * w - While
     * F - For
     * r - Return
     * b - Break (includes continue)
     * S - SimpleFor
     * C - Switch
     * </pre>
     */
    public abstract char id();

    public int getPosition();
}
