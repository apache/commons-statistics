/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.statistics.distribution;

import java.util.function.Supplier;

/**
 * Utilities for creating String messages.
 */
final class StringUtils {
    /** The space ' ' character. */
    private static final char SPACE_CHAR = ' ';
    /** The AND string. */
    private static final String AND = " && ";
    /** The OR string. */
    private static final String OR = " || ";
    /** The wrapped unknown string: "(NA)". */
    private static final String NA = "(NA)";

    /**
     * Do not allow public construction.
     */
    private StringUtils() {}

    /**
     * Checks if the string is not null or empty whitespace.
     *
     * @param string the string
     * @return true, if is not empty
     */
    static boolean isNotEmpty(String string) {
        return string != null && hasNonWhiteSpace(string);
    }

    /**
     * Checks if the string is null or empty whitespace.
     *
     * @param string the string
     * @return true, if null or empty
     */
    static boolean isNullOrEmpty(String string) {
        return string == null || !hasNonWhiteSpace(string);
    }

    /**
     * Checks for non white space.
     *
     * @param string the string
     * @return true, if successful
     */
    private static boolean hasNonWhiteSpace(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) > SPACE_CHAR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the string representation. The object may be a {@link String} or {@link Supplier}.
     *
     * <ul>
     *  <li>If the object is a {@link String} then the string is returned.
     *  <li>If the object is a {@link Supplier} then the supplied object is obtained and,
     *      if not null, {@link Object#toString()} is returned.
     *  <li>Otherwise returns {@code null}.
     * </ul>
     *
     * @param object the object
     * @return the string representation
     */
    static String toString(Object object) {
        if (object instanceof String) {
            return (String) object;
        }
        if (object instanceof Supplier) {
            final Object supplied = ((Supplier<?>) object).get();
            if (supplied != null) {
                return supplied.toString();
            }
        }
        return null;
    }

    /**
     * Gets the negation of the string representation, obtained from {@link #toString(Object)}.
     *
     * <ul>
     *  <li>If the object has a string representation the result is {@code !(string)}.
     *  <li>Otherwise returns {@code null}.
     * </ul>
     *
     * @param object the object
     * @return the string representation
     */
    static String negateToString(Object object) {
        final String s = toString(object);
        return (isNotEmpty(s)) ? "!(" + s + ")" : null;
    }

    /**
     * Gets the logical OR combination of the string representation of two objects, each obtained from
     * {@link #toString(Object)}.
     *
     * <ul>
     *  <li>If both strings are valid the result is {@code (string1) || (string2)}.
     *  <li>If either string is {@code null} or empty it will be replaced with {@code NA}.
     *  <li>Otherwise returns {@code null}.
     * </ul>
     *
     * @param object1 the first object
     * @param object2 the second object
     * @return the OR string representation
     * @see #toString(Object)
     */
    static String orToString(Object object1, Object object2) {
        final String s1 = toString(object1);
        final String s2 = toString(object2);
        if (isNotEmpty(s1) || isNotEmpty(s2)) {
            return concatenate(s1, OR, s2);
        }
        return null;
    }

    /**
     * Gets the logical AND combination of the string representation of two objects, each obtained
     * from {@link #toString(Object)}.
     *
     * <ul>
     *  <li>If both strings are valid the result is {@code (string1) && (string2)}.
     *  <li>If either string is {@code null} or empty it will be replaced with {@code NA}.
     *  <li>Otherwise returns {@code null}.
     * </ul> {@link String} or {@link Supplier} of {@link String}.
     *
     * @param object1 the first object
     * @param object2 the second object
     * @return the AND string representation
     * @see #toString(Object)
     */
    static String andToString(Object object1, Object object2) {
        final String s1 = toString(object1);
        final String s2 = toString(object2);
        if (isNotEmpty(s1) || isNotEmpty(s2)) {
            return concatenate(s1, AND, s2);
        }
        return null;
    }

    /**
     * Concatenate the two strings. Adds parentheses if required.
     * This is made simple by only supporting {@code &&} or {@code ||}.
     *
     * @param s1 the first string
     * @param operator the operator to join the strings
     * @param s2 the second string
     * @return the string
     */
    private static String concatenate(String s1, String operator, String s2) {
        // Detect the last and first operators in the two strings.
        // If the same as the joining operator then no parentheses are required.
        final StringBuilder sb = new StringBuilder(64);
        if (isNotEmpty(s1)) {
            final String op = lastOperator(s1);
            if (op == null || op.equals(operator)) {
                sb.append(s1);
            } else {
                sb.append('(').append(s1).append(')');
            }
        } else {
            sb.append(NA);
        }
        sb.append(operator);
        if (isNotEmpty(s2)) {
            final String op = firstOperator(s2);
            if (op == null || op.equals(operator)) {
                sb.append(s2);
            } else {
                sb.append('(').append(s2).append(')');
            }
        } else {
            sb.append(NA);
        }
        return sb.toString();
    }

    /**
     * Find the last operator in the string.
     *
     * @param s the string
     * @return the operator (or null)
     */
    private static String lastOperator(String s) {
        final int a = s.lastIndexOf(AND);
        final int o = s.lastIndexOf(OR);
        if (a > o) {
            return AND;
        }
        return o != -1 ? OR : null;
    }

    /**
     * Find the last operator in the string.
     *
     * @param s the string
     * @return the operator (or null)
     */
    private static String firstOperator(String s) {
        int a = s.indexOf(AND);
        int o = s.indexOf(OR);
        final int len = s.length();
        a = a < 0 ? len : a;
        o = o < 0 ? len : o;
        if (a < o) {
            return AND;
        }
        return o < len ? OR : null;
    }
}
