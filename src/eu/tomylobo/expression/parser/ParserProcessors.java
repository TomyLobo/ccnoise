package eu.tomylobo.expression.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import eu.tomylobo.expression.Identifiable;
import eu.tomylobo.expression.lexer.tokens.OperatorToken;
import eu.tomylobo.expression.lexer.tokens.Token;
import eu.tomylobo.expression.runtime.RValue;
import eu.tomylobo.expression.runtime.Operators;
import eu.tomylobo.expression.runtime.Sequence;

public final class ParserProcessors {
    private static final Map<String, String> unaryOpMap = new HashMap<String, String>();

    private static final Map<String, String>[] binaryOpMapsLA;
    private static final Map<String, String>[] binaryOpMapsRA;

    static {
        unaryOpMap.put("-", "neg");
        unaryOpMap.put("!", "not");
        unaryOpMap.put("~", "inv");
        unaryOpMap.put("++", "inc");
        unaryOpMap.put("--", "dec");

        final Object[][][] binaryOpsLA = {
                {
                    { "^", "pow" },
                    { "**", "pow" },
                },
                {
                    { "*", "mul" },
                    { "/", "div" },
                    { "%", "mod" },
                },
                {
                    { "+", "add" },
                    { "-", "sub" },
                },
                {
                    { "<<", "shl" },
                    { ">>", "shr" },
                },
                {
                    { "<", "lth" },
                    { ">", "gth" },
                    { "<=", "leq" },
                    { ">=", "geq" },
                },
                {
                    { "==", "equ" },
                    { "!=", "neq" },
                    { "~=", "near" },
                },
                {
                    { "&&", "and" },
                },
                {
                    { "||", "or" },
                },
        };
        final Object[][][] binaryOpsRA = {
                {
                    { "=", "ass" },
                    { "+=", "aadd" },
                    { "-=", "asub" },
                    { "*=", "amul" },
                    { "/=", "adiv" },
                    { "%=", "amod" },
                    { "^=", "aexp" },
                },
        };

        @SuppressWarnings("unchecked")
        final Map<String, String>[] lBinaryOpMapsLA = binaryOpMapsLA = new Map[binaryOpsLA.length];
        for (int i = 0; i < binaryOpsLA.length; ++i) {
            final Object[][] a = binaryOpsLA[i];
            switch (a.length) {
            case 0:
                lBinaryOpMapsLA[i] = Collections.emptyMap();
                break;

            case 1:
                final Object[] first = a[0];
                lBinaryOpMapsLA[i] = Collections.singletonMap((String) first[0], (String) first[1]);
                break;

            default:
                Map<String, String> m = lBinaryOpMapsLA[i] = new HashMap<String, String>();
                for (int j = 0; j < a.length; ++j) {
                    final Object[] element = a[j];
                    m.put((String) element[0], (String) element[1]);
                }
            }
        }

        @SuppressWarnings("unchecked")
        final Map<String, String>[] lBinaryOpMapsRA = binaryOpMapsRA = new Map[binaryOpsRA.length];
        for (int i = 0; i < binaryOpsRA.length; ++i) {
            final Object[][] a = binaryOpsRA[i];
            switch (a.length) {
            case 0:
                lBinaryOpMapsRA[i] = Collections.emptyMap();
                break;

            case 1:
                final Object[] first = a[0];
                lBinaryOpMapsRA[i] = Collections.singletonMap((String) first[0], (String) first[1]);
                break;

            default:
                Map<String, String> m = lBinaryOpMapsRA[i] = new HashMap<String, String>();
                for (int j = 0; j < a.length; ++j) {
                    final Object[] element = a[j];
                    m.put((String) element[0], (String) element[1]);
                }
            }
        }
    }

    static RValue processStatement(LinkedList<Identifiable> input) throws ParserException {
        LinkedList<Identifiable> lhs = new LinkedList<Identifiable>();
        LinkedList<Identifiable> rhs = new LinkedList<Identifiable>();
        boolean semicolonFound = false;

        for (Identifiable identifiable : input) {
            if (semicolonFound) {
                rhs.addLast(identifiable);
            }
            else {
                if (identifiable.id() == ';') {
                    semicolonFound = true;
                }
                else {
                    lhs.addLast(identifiable);
                }
            }
        }

        if (rhs.isEmpty()) {
            if (lhs.isEmpty()) {
                return new Sequence(semicolonFound ? input.get(0).getPosition() : -1);
            }

            return processExpression(lhs);
        }
        else if (lhs.isEmpty()) {
            return processStatement(rhs);
        }
        else {
            assert(semicolonFound);

            RValue lhsInvokable = processExpression(lhs);
            RValue rhsInvokable = processStatement(rhs);

            return new Sequence(lhsInvokable.getPosition(), lhsInvokable, rhsInvokable);
        }
    }

    static RValue processExpression(LinkedList<Identifiable> input) throws ParserException {
        return processBinaryOpsRA(input, binaryOpMapsRA.length - 1);
    }

    private static RValue processBinaryOpsLA(LinkedList<Identifiable> input, int level) throws ParserException {
        if (level < 0) {
            return processUnaryOps(input);
        }

        LinkedList<Identifiable> lhs = new LinkedList<Identifiable>();
        LinkedList<Identifiable> rhs = new LinkedList<Identifiable>();
        String operator = null;

        for (Iterator<Identifiable> it = input.descendingIterator(); it.hasNext();) {
            Identifiable identifiable = it.next();
            if (operator == null) {
                rhs.addFirst(identifiable);

                if (!(identifiable instanceof OperatorToken)) {
                    continue;
                }

                operator = binaryOpMapsLA[level].get(((OperatorToken) identifiable).operator);
                if (operator == null) {
                    continue;
                }

                rhs.removeFirst();
            }
            else {
                lhs.addFirst(identifiable);
            }
        }

        RValue rhsInvokable = processBinaryOpsLA(rhs, level - 1);
        if (operator == null) return rhsInvokable;

        RValue lhsInvokable = processBinaryOpsLA(lhs, level);

        try {
            return Operators.getOperator(input.get(0).getPosition(), operator, lhsInvokable, rhsInvokable);
        }
        catch (NoSuchMethodException e) {
            final Token operatorToken = (Token) input.get(lhs.size());
            throw new ParserException(operatorToken.getPosition(), "Couldn't find operator '" + operator + "'");
        }
    }

    private static RValue processBinaryOpsRA(LinkedList<Identifiable> input, int level) throws ParserException {
        if (level < 0) {
            return processTernaryOps(input);
        }

        LinkedList<Identifiable> lhs = new LinkedList<Identifiable>();
        LinkedList<Identifiable> rhs = new LinkedList<Identifiable>();
        String operator = null;

        for (Identifiable identifiable : input) {
            if (operator == null) {
                lhs.addLast(identifiable);

                if (!(identifiable instanceof OperatorToken)) {
                    continue;
                }

                operator = binaryOpMapsRA[level].get(((OperatorToken) identifiable).operator);
                if (operator == null) {
                    continue;
                }

                lhs.removeLast();
            }
            else {
                rhs.addLast(identifiable);
            }
        }

        RValue lhsInvokable = processBinaryOpsRA(lhs, level - 1);
        if (operator == null) return lhsInvokable;

        RValue rhsInvokable = processBinaryOpsRA(rhs, level);

        try {
            return Operators.getOperator(input.get(0).getPosition(), operator, lhsInvokable, rhsInvokable);
        }
        catch (NoSuchMethodException e) {
            final Token operatorToken = (Token) input.get(lhs.size());
            throw new ParserException(operatorToken.getPosition(), "Couldn't find operator '" + operator + "'");
        }
    }

    private static RValue processTernaryOps(LinkedList<Identifiable> input) throws ParserException {
        return processBinaryOpsLA(input, binaryOpMapsLA.length - 1);
    }

    private static RValue processUnaryOps(LinkedList<Identifiable> input) throws ParserException {
        if (input.isEmpty()) {
            throw new ParserException(-1, "Expression missing.");
        }

        RValue ret = (RValue) input.removeLast();
        while (!input.isEmpty()) {
            final Identifiable last = input.removeLast();
            final int lastPosition = last.getPosition();
            if (last instanceof PrefixOperator) {
                final String operator = ((PrefixOperator) last).operator;
                if (operator.equals("+")) {
                    continue;
                }

                String opName = unaryOpMap.get(operator);
                if (opName != null) {
                    try {
                        ret = Operators.getOperator(lastPosition, opName, ret);
                        continue;
                    }
                    catch (NoSuchMethodException e) {
                        throw new ParserException(lastPosition, "No such prefix operator: " + operator);
                    }
                }
            }
            if (last instanceof Token) {
                throw new ParserException(lastPosition, "Extra token found in expression: " + last);
            }
            else if (last instanceof RValue) {
                throw new ParserException(lastPosition, "Extra expression found: " + last);
            }
            else {
                throw new ParserException(lastPosition, "Extra element found: " + last);
            }
        }
        return ret;
    }
}