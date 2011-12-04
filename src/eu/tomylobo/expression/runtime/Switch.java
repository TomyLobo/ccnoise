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

package eu.tomylobo.expression.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A switch/case construct.
 *
 * @author TomyLobo
 */
public class Switch extends Node implements RValue {
    private final RValue parameter;
    private final Map<Double, Integer> valueMap;
    private final RValue[] caseStatements;
    private final RValue defaultCase;

    public Switch(int position, RValue parameter, List<Double> values, List<RValue> caseStatements, RValue defaultCase) {
        this(position, parameter, invertList(values), caseStatements, defaultCase);

    }

    private static Map<Double, Integer> invertList(List<Double> values) {
        Map<Double, Integer> valueMap = new HashMap<Double, Integer>();
        for (int i = 0; i < values.size(); ++i) {
            valueMap.put(values.get(i), i);
        }
        return valueMap;
    }

    private Switch(int position, RValue parameter, Map<Double, Integer> valueMap, List<RValue> caseStatements, RValue defaultCase) {
        super(position);

        this.parameter = parameter;
        this.valueMap = valueMap;
        this.caseStatements = caseStatements.toArray(new RValue[caseStatements.size()]);
        this.defaultCase = defaultCase;
    }

    @Override
    public char id() {
        return 'W';
    }

    @Override
    public double getValue() throws EvaluationException {
        final double parameter = this.parameter.getValue();

        try {
            double ret = 0.0;

            final Integer index = valueMap.get(parameter);
            if (index != null) {
                for (int i = index; i < caseStatements.length; ++i) {
                    ret = caseStatements[i].getValue();
                }
            }

            return defaultCase == null ? ret : defaultCase.getValue();
        } catch (BreakException e) {
            return 0.0;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("switch (");
        sb.append(parameter);
        sb.append(") { ");

        for (int i = 0; i < caseStatements.length; ++i) {
            RValue caseStatement = caseStatements[i];
            sb.append("case ");
            for (Entry<Double, Integer> entry : valueMap.entrySet()) {
                if (entry.getValue() == i) {
                    sb.append(entry.getKey());
                    break;
                }
            }
            sb.append(": ");
            sb.append(caseStatement);
            sb.append(' ');
        }

        if (defaultCase != null) {
            sb.append("default: ");
            sb.append(defaultCase);
            sb.append(' ');
        }

        sb.append("}");

        return sb.toString();
    }

    @Override
    public RValue optimize() throws EvaluationException {
        final List<RValue> newSequence = new ArrayList<RValue>();
        final Map<Double, Integer> newValueMap = new HashMap<Double, Integer>();

        Map<Integer, Double> backMap = new HashMap<Integer, Double>();
        for (Entry<Double, Integer> entry : valueMap.entrySet()) {
            backMap.put(entry.getValue(), entry.getKey());
        }

        for (int i = 0; i < caseStatements.length; ++i) {
            final RValue invokable = caseStatements[i].optimize();

            final Double caseValue = backMap.get(i);
            if (caseValue != null) {
                newValueMap.put(caseValue, newSequence.size());
            }

            if (invokable instanceof Sequence) {
                for (RValue subInvokable : ((Sequence) invokable).sequence) {
                    newSequence.add(subInvokable);
                }
            } else {
                newSequence.add(invokable);
            }
        }

        return new Switch(getPosition(), parameter.optimize(), newValueMap, newSequence, defaultCase.optimize());
    }
}