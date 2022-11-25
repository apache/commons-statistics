/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.statistics.distribution;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Contains the data for the distribution parameters, the expected properties
 * of the distribution (moments and support bounds) and test points to evaluate
 * with expected values.
 */
abstract class DistributionTestData {
    // Keys for values that are set to test defaults for a distribution.
    // These values are expected to be the same for all test cases
    // and may be set in the properties before creating the test data instance.

    /** The key for the absolute tolerance value. */
    static final String KEY_TOLERANCE_ABSOLUTE = "tolerance.absolute";
    /** The key for the relative tolerance value. */
    static final String KEY_TOLERANCE_RELATIVE = "tolerance.relative";

    /** The key suffix to disable a test. */
    private static final String SUFFIX_DISABLE = ".disable";
    /** The key suffix for the absolute tolerance value. */
    private static final String SUFFIX_TOLERANCE_ABSOLUTE = ".absolute";
    /** The key suffix for the relative tolerance value. */
    private static final String SUFFIX_TOLERANCE_RELATIVE = ".relative";
    /** The index for the absolute tolerance value in the array of tolerances. */
    private static final int INDEX_ABSOLUTE = 0;
    /** The index for the relative tolerance value in the array of tolerances. */
    private static final int INDEX_RELATIVE = 1;
    /** The unset (default) value for the tolerance. */
    private static final double UNSET_TOLERANCE = -1;
    /** The unset (default) values for the array of tolerances. */
    private static final double[] UNSET_TOLERANCES = {UNSET_TOLERANCE, UNSET_TOLERANCE};

    /** Regex to split delimited text data (e.g. arrays of numbers). */
    private static final Pattern PATTERN = Pattern.compile("[ ,]+");

    /** Expected probability function values. */
    protected final double[] pfValues;
    /** Expected log probability function values. */
    protected final double[] logPfValues;

    /** Distribution parameters. */
    private final Object[] parameters;
    /** Mean. */
    private final double mean;
    /** Variance. */
    private final double variance;
    /** Test tolerances. */
    private final Map<TestName, double[]> tolerance;
    /** Disabled tests. */
    private final Set<TestName> disabled;

    /** Test absolute tolerance for calculations. */
    private final double absoluteTolerance;
    /** Test relative tolerance for calculations. */
    private final double relativeTolerance;
    /** Expected CDF values. */
    private final double[] cdfValues;
    /** Expected SF values for the survival function test points. */
    private final double[] sfValues;
    /** Expected CDF values for the high-precision CDF test points. */
    private final double[] cdfHpValues;
    /** Expected CDF values for the high-precision survival function test points. */
    private final double[] sfHpValues;

    /**
     * Contains the data for the continuous distribution parameters, the expected properties
     * of the distribution (moments and support bounds) and test points to evaluate
     * with expected values.
     */
    static class ContinuousDistributionTestData extends DistributionTestData {
        /** Support lower bound. */
        private final double lower;
        /** Support upper bound. */
        private final double upper;
        /** Test points to evaluate the CDF. */
        private final double[] cdfPoints;
        /** Test points to evaluate the PDF. */
        private final double[] pdfPoints;
        /** Test points to evaluate survival function computations. */
        private final double[] sfPoints;
        /** Test points to evaluate high-precision CDF computations. */
        private final double[] cdfHpPoints;
        /** Test points to evaluate high-precision survival function computations. */
        private final double[] sfHpPoints;
        /** Test points to evaluate the inverse CDF. */
        private final double[] icdfPoints;
        /** Expected inverse CDF values. */
        private final double[] icdfValues;
        /** Test points to evaluate the inverse SF. */
        private final double[] isfPoints;
        /** Expected inverse SF values. */
        private final double[] isfValues;

        /**
         * @param props Properties containing the test data
         */
        ContinuousDistributionTestData(Properties props) {
            super(props);
            // Load all the data
            lower = getAsDouble(props, "lower", Double.NEGATIVE_INFINITY);
            upper = getAsDouble(props, "upper", Double.POSITIVE_INFINITY);
            // Required
            cdfPoints = getAsDoubleArray(props, "cdf.points");
            // Optional
            pdfPoints = getAsDoubleArray(props, "pdf.points", cdfPoints);
            sfPoints = getAsDoubleArray(props, "sf.points", cdfPoints);
            cdfHpPoints = getAsDoubleArray(props, "cdf.hp.points", null);
            sfHpPoints = getAsDoubleArray(props, "sf.hp.points", null);
            // Do not default to an inverse mapping.
            // A separate [cdf|sf].inverse property controls an inverse mapping test.
            icdfPoints = getAsDoubleArray(props, "icdf.points", null);
            icdfValues = getAsDoubleArray(props, "icdf.values", null);
            isfPoints = getAsDoubleArray(props, "isf.points", null);
            isfValues = getAsDoubleArray(props, "isf.values", null);
            // Validation
            validatePair(cdfPoints, getCdfValues(), "cdf");
            validatePair(pdfPoints, getPdfValues(), "pdf");
            validatePair(pdfPoints, getLogPdfValues(), "logpdf");
            validatePair(sfPoints, getSfValues(), "sf");
            validatePair(cdfHpPoints, getCdfHpValues(), "cdf.hp");
            validatePair(sfHpPoints, getSfHpValues(), "sf.hp");
            validatePair(icdfPoints, icdfValues, "icdf");
            validatePair(isfPoints, isfValues, "isf");
        }

        @Override
        String getProbabilityFunctionName() {
            return "pdf";
        }

        /**
         * Gets the support lower bound of the distribution.
         *
         * @return the lower bound
         */
        double getLower() {
            return lower;
        }

        /**
         * Gets the support upper bound of the distribution.
         *
         * @return the upper bound
         */
        double getUpper() {
            return upper;
        }

        /**
         * Gets the points to evaluate the CDF.
         *
         * @return the points
         */
        double[] getCdfPoints() {
            return cdfPoints;
        }

        /**
         * Gets the points to evaluate the PDF.
         *
         * @return the points
         */
        double[] getPdfPoints() {
            return pdfPoints;
        }

        /**
         * Gets the expected density values for the PDF test points.
         *
         * @return the PDF values
         */
        double[] getPdfValues() {
            return pfValues;
        }

        /**
         * Gets the expected log density values for the PDF test points.
         *
         * @return the log PDF values
         */
        double[] getLogPdfValues() {
            return logPfValues;
        }

        /**
         * Gets the points to evaluate for survival function.
         *
         * @return the SF points
         */
        double[] getSfPoints() {
            return sfPoints;
        }

        /**
         * Gets the points to evaluate the cumulative probability where the result
         * is expected to be approaching zero and requires a high-precision computation.
         *
         * @return the CDF high-precision points
         */
        double[] getCdfHpPoints() {
            return cdfHpPoints;
        }

        /**
         * Gets the points to evaluate the survival probability where the result
         * is expected to be approaching zero and requires a high-precision computation.
         *
         * @return the survival function high-precision points
         */
        double[] getSfHpPoints() {
            return sfHpPoints;
        }

        @Override
        double[] getIcdfPoints() {
            return icdfPoints;
        }

        /**
         * Gets the expected inverse cumulative probability values for the test inverse CDF points.
         *
         * @return the inverse CDF values
         */
        double[] getIcdfValues() {
            return icdfValues;
        }

        @Override
        double[] getIsfPoints() {
            return isfPoints;
        }

        /**
         * Gets the expected inverse survival probability values for the test inverse SF points.
         *
         * @return the inverse SF values
         */
        double[] getIsfValues() {
            return isfValues;
        }
    }

    /**
     * Contains the data for the continuous distribution parameters, the expected properties
     * of the distribution (moments and support bounds) and test points to evaluate
     * with expected values.
     */
    static class DiscreteDistributionTestData extends DistributionTestData {
        /** Support lower bound. */
        private final int lower;
        /** Support upper bound. */
        private final int upper;
        /** Test points to evaluate the CDF. */
        private final int[] cdfPoints;
        /** Test points to evaluate the PDF. */
        private final int[] pmfPoints;
        /** Test points to evaluate survival function computations. */
        private final int[] sfPoints;
        /** Test points to evaluate high-precision CDF computations. */
        private final int[] cdfHpPoints;
        /** Test points to evaluate high-precision survival function computations. */
        private final int[] sfHpPoints;
        /** Test points to evaluate the inverse CDF. */
        private final double[] icdfPoints;
        /** Expected inverse CDF values. */
        private final int[] icdfValues;
        /** Test points to evaluate the inverse SF. */
        private final double[] isfPoints;
        /** Expected inverse SF values. */
        private final int[] isfValues;

        /**
         * @param props Properties containing the test data
         */
        DiscreteDistributionTestData(Properties props) {
            super(props);
            // Load all the data
            lower = getAsInt(props, "lower", Integer.MIN_VALUE);
            upper = getAsInt(props, "upper", Integer.MAX_VALUE);
            // Required
            cdfPoints = getAsIntArray(props, "cdf.points");
            // Optional
            pmfPoints = getAsIntArray(props, "pmf.points", cdfPoints);
            sfPoints = getAsIntArray(props, "sf.points", cdfPoints);
            cdfHpPoints = getAsIntArray(props, "cdf.hp.points", null);
            sfHpPoints = getAsIntArray(props, "sf.hp.points", null);
            // Do not default to an inverse mapping.
            // A separate [cdf|sf].inverse property controls an inverse mapping test.
            icdfPoints = getAsDoubleArray(props, "icdf.points", null);
            icdfValues = getAsIntArray(props, "icdf.values", null);
            isfPoints = getAsDoubleArray(props, "isf.points", null);
            isfValues = getAsIntArray(props, "isf.values", null);
            // Validation
            validatePair(cdfPoints, getCdfValues(), "cdf");
            validatePair(pmfPoints, getPmfValues(), "pmf");
            validatePair(pmfPoints, getLogPmfValues(), "logpmf");
            validatePair(sfPoints, getSfValues(), "sf");
            validatePair(cdfHpPoints, getCdfHpValues(), "cdf.hp");
            validatePair(sfHpPoints, getSfHpValues(), "sf.hp");
            validatePair(icdfPoints, icdfValues, "icdf");
            validatePair(isfPoints, isfValues, "isf");
        }

        @Override
        String getProbabilityFunctionName() {
            return "pmf";
        }

        /**
         * Gets the support lower bound of the distribution.
         *
         * @return the lower bound
         */
        int getLower() {
            return lower;
        }

        /**
         * Gets the support upper bound of the distribution.
         *
         * @return the upper bound
         */
        int getUpper() {
            return upper;
        }

        /**
         * Gets the points to evaluate the CDF.
         *
         * @return the points
         */
        int[] getCdfPoints() {
            return cdfPoints;
        }

        /**
         * Gets the points to evaluate the PMF.
         *
         * @return the points
         */
        int[] getPmfPoints() {
            return pmfPoints;
        }

        /**
         * Gets the expected density values for the PMF test points.
         *
         * @return the PDF values
         */
        double[] getPmfValues() {
            return pfValues;
        }

        /**
         * Gets the expected log density values for the PMF test points.
         *
         * @return the log PDF values
         */
        double[] getLogPmfValues() {
            return logPfValues;
        }

        /**
         * Gets the points to evaluate for survival function.
         *
         * @return the SF points
         */
        int[] getSfPoints() {
            return sfPoints;
        }

        /**
         * Gets the points to evaluate the cumulative probability where the result
         * is expected to be approaching zero and requires a high-precision computation.
         *
         * @return the CDF high-precision points
         */
        int[] getCdfHpPoints() {
            return cdfHpPoints;
        }

        /**
         * Gets the points to evaluate the survival probability where the result
         * is expected to be approaching zero and requires a high-precision computation.
         *
         * @return the survival function high-precision points
         */
        int[] getSfHpPoints() {
            return sfHpPoints;
        }

        @Override
        double[] getIcdfPoints() {
            return icdfPoints;
        }

        /**
         * Gets the expected inverse cumulative probability values for the test inverse CDF points.
         *
         * @return the inverse CDF values
         */
        int[] getIcdfValues() {
            return icdfValues;
        }

        @Override
        double[] getIsfPoints() {
            return isfPoints;
        }

        /**
         * Gets the expected inverse survival probability values for the test inverse SF points.
         *
         * @return the inverse SF values
         */
        int[] getIsfValues() {
            return isfValues;
        }
    }

    /**
     * @param props Properties containing the test data
     */
    DistributionTestData(Properties props) {
        // Load all the data
        parameters = PATTERN.splitAsStream(get(props, "parameters"))
                            .map(DistributionTestData::parseParameter).toArray();
        mean = getAsDouble(props, "mean");
        variance = getAsDouble(props, "variance");
        absoluteTolerance = getAsDouble(props, KEY_TOLERANCE_ABSOLUTE);
        relativeTolerance = getAsDouble(props, KEY_TOLERANCE_RELATIVE);
        // Required
        cdfValues = getAsDoubleArray(props, "cdf.values");
        final String pf = getProbabilityFunctionName();
        pfValues = getAsDoubleArray(props, pf + ".values");
        // Optional
        double[] tmp = getAsDoubleArray(props, "log" + pf + ".values", null);
        if (tmp == null && pfValues != null) {
            tmp = Arrays.stream(pfValues).map(Math::log).toArray();
        }
        logPfValues = tmp;
        tmp = getAsDoubleArray(props, "sf.values", null);
        if (tmp == null && cdfValues != null) {
            tmp = Arrays.stream(cdfValues).map(d -> 1.0 - d).toArray();
        }
        sfValues = tmp;
        cdfHpValues = getAsDoubleArray(props, "cdf.hp.values", null);
        sfHpValues = getAsDoubleArray(props, "sf.hp.values", null);

        // Remove keys to prevent detection in when searching for test tolerances
        props.remove(KEY_TOLERANCE_ABSOLUTE);
        props.remove(KEY_TOLERANCE_RELATIVE);

        // Store custom tolerances and disabled tests
        EnumMap<TestName, double[]> map = new EnumMap<>(TestName.class);
        EnumSet<TestName> set = EnumSet.noneOf(TestName.class);
        props.stringPropertyNames().forEach(key -> {
            if (key.endsWith(SUFFIX_DISABLE) && getAsBoolean(props, key, false)) {
                final TestName name = TestName.fromString(key.substring(0, key.length() - SUFFIX_DISABLE.length()));
                if (name != null) {
                    set.add(name);
                }
            } else if (key.endsWith(SUFFIX_TOLERANCE_ABSOLUTE)) {
                final TestName name = TestName.fromString(key.substring(0, key.length() - SUFFIX_TOLERANCE_ABSOLUTE.length()));
                if (name != null) {
                    final double[] tolerances = map.computeIfAbsent(name, k -> UNSET_TOLERANCES.clone());
                    tolerances[INDEX_ABSOLUTE] = getAsDouble(props, key);
                }
            } else if (key.endsWith(SUFFIX_TOLERANCE_RELATIVE)) {
                final TestName name = TestName.fromString(key.substring(0, key.length() - SUFFIX_TOLERANCE_RELATIVE.length()));
                if (name != null) {
                    final double[] tolerances = map.computeIfAbsent(name, k -> UNSET_TOLERANCES.clone());
                    tolerances[INDEX_RELATIVE] = getAsDouble(props, key);
                }
            }
        });

        this.tolerance = map.isEmpty() ? Collections.emptyMap() : map;
        this.disabled = set.isEmpty() ? Collections.emptySet() : set;
    }

    /**
     * Gets the name of the probability density function.
     * For continuous distributions this is PDF and discrete distributions is PMF.
     *
     * @return the PDF name
     */
    abstract String getProbabilityFunctionName();

    /**
     * Parses the String parameter to an appropriate object. Supports Double and Integer.
     *
     * @param value Value
     * @return the object
     * @throws IllegalArgumentException if the parameter type is unknown
     */
    private static Object parseParameter(String value) {
        // Only support int or double parameters.
        // This uses inefficient parsing which will relies on catching parse exceptions.
        try {
            return parseInt(value);
        } catch (NumberFormatException ex) { /* ignore */ }
        try {
            return parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Unknown parameter type: " + value, ex);
        }
    }

    /**
     * Gets the property.
     *
     * @param props Properties
     * @param key Key
     * @return the value
     * @throws NullPointerException if the parameter is missing
     */
    private static String get(Properties props, String key) {
        return Objects.requireNonNull(props.getProperty(key), () -> "Missing test data: " + key);
    }

    /**
     * Returns a new {@code int} initialized to the value
     * represented by the input String.
     *
     * <p>A special concession is made for 'max' and 'min'
     * as a short representation of the maximum and minimum
     * integer values.
     *
     * @param s Input String
     * @return the int
     * @see Integer#parseInt(String)
     * @see Integer#MAX_VALUE
     * @see Integer#MIN_VALUE
     */
    private static int parseInt(String s) {
        if ("max".equals(s)) {
            return Integer.MAX_VALUE;
        } else if ("min".equals(s)) {
            return Integer.MIN_VALUE;
        }
        return Integer.parseInt(s);
    }

    /**
     * Returns a new {@code double} initialized to the value
     * represented by the input String.
     *
     * <p>A special concession is made for 'Inf' or 'inf' as a short
     * representation of 'Infinity'. This format is used by
     * matlab and R (Inf) and python (inf).
     *
     * @param s Input String
     * @return the double
     * @see Double#parseDouble(String)
     */
    private static double parseDouble(String s) {
        // Detect other forms of infinity: -Inf, Inf or inf, -inf
        final int len = s.length();
        if ((len == 3 || len == 4) &&
            s.charAt(len - 1) == 'f' &&
            s.charAt(len - 2) == 'n') {
            // Sign detection
            final int start = s.charAt(0) == '-' ? 1 : 0;
            // Remaining length must be 3.
            // Final unchecked character is 'i'.
            if (s.length() - start == 3 && (s.charAt(start) == 'I' || s.charAt(start) == 'i')) {
                return start == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
        }
        return Double.parseDouble(s);
    }

    /**
     * Gets the property as a double.
     *
     * @param props Properties
     * @param key Key
     * @return the value
     * @throws NullPointerException if the parameter is missing.
     * @throws IllegalArgumentException if the parameter is not a double.
     */
    private static double getAsDouble(Properties props, String key) {
        try {
            return parseDouble(get(props, key));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid double: " + key, ex);
        }
    }

    /**
     * Gets the property as a double, or a default value if the property is missing.
     *
     * @param props Properties
     * @param key Key
     * @param defaultValue Default value
     * @return the value
     * @throws IllegalArgumentException if the parameter is not a double.
     */
    private static double getAsDouble(Properties props, String key, double defaultValue) {
        try {
            final String s = props.getProperty(key);
            return s == null ? defaultValue : parseDouble(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid double: " + key, ex);
        }
    }

    /**
     * Gets the property as a double, or a default value if the property is missing.
     *
     * @param props Properties
     * @param key Key
     * @param defaultValue Default value
     * @return the value
     * @throws IllegalArgumentException if the parameter is not a double.
     */
    private static int getAsInt(Properties props, String key, int defaultValue) {
        try {
            final String s = props.getProperty(key);
            return s == null ? defaultValue : parseInt(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid double: " + key, ex);
        }
    }

    /**
     * Gets the property as a boolean, or a default value if the property is missing.
     *
     * @param props Properties
     * @param key Key
     * @param defaultValue Default value
     * @return the value
     * @throws IllegalArgumentException if the parameter is not a boolean.
     */
    private static boolean getAsBoolean(Properties props, String key, boolean defaultValue) {
        try {
            final String s = props.getProperty(key);
            return s == null ? defaultValue : Boolean.parseBoolean(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid boolean: " + key, ex);
        }
    }

    /**
     * Gets the property as a double array.
     *
     * @param props Properties
     * @param key Key
     * @return the value
     * @throws NullPointerException if the parameter is missing.
     * @throws IllegalArgumentException if the parameter is not a double array.
     */
    private static double[] getAsDoubleArray(Properties props, String key) {
        try {
            return PATTERN.splitAsStream(get(props, key)).mapToDouble(DistributionTestData::parseDouble).toArray();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid double: " + key, ex);
        }
    }

    /**
     * Gets the property as a double array, or a default value if the property is missing.
     *
     * @param props Properties
     * @param key Key
     * @param defaultValue Default value
     * @return the value
     * @throws IllegalArgumentException if the parameter is not a double array.
     */
    private static double[] getAsDoubleArray(Properties props, String key, double[] defaultValue) {
        try {
            final String s = props.getProperty(key);
            return s == null ? defaultValue :
                PATTERN.splitAsStream(s).mapToDouble(DistributionTestData::parseDouble).toArray();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid double: " + key, ex);
        }
    }
    /**
     * Gets the property as a double array.
     *
     * @param props Properties
     * @param key Key
     * @return the value
     * @throws NullPointerException if the parameter is missing.
     * @throws IllegalArgumentException if the parameter is not a double array.
     */
    private static int[] getAsIntArray(Properties props, String key) {
        try {
            return PATTERN.splitAsStream(get(props, key)).mapToInt(DistributionTestData::parseInt).toArray();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid double: " + key, ex);
        }
    }

    /**
     * Gets the property as a double array, or a default value if the property is missing.
     *
     * @param props Properties
     * @param key Key
     * @param defaultValue Default value
     * @return the value
     * @throws IllegalArgumentException if the parameter is not a double array.
     */
    private static int[] getAsIntArray(Properties props, String key, int[] defaultValue) {
        try {
            final String s = props.getProperty(key);
            return s == null ? defaultValue :
                PATTERN.splitAsStream(s).mapToInt(DistributionTestData::parseInt).toArray();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid double: " + key, ex);
        }
    }

    /**
     * Validate a pair of point-value arrays have the same length if they are both non-zero length.
     *
     * @param p Array 1
     * @param v Array 2
     * @param name Name of the pair
     */
    private static void validatePair(double[] p, double[] v, String name) {
        validatePair(TestUtils.getLength(p), TestUtils.getLength(v), name);
    }

    /**
     * Validate a pair of point-value arrays have the same length if they are both non-zero length.
     *
     * @param p Array 1
     * @param v Array 2
     * @param name Name of the pair
     */
    private static void validatePair(int[] p, double[] v, String name) {
        validatePair(TestUtils.getLength(p), TestUtils.getLength(v), name);
    }

    /**
     * Validate a pair of point-value arrays have the same length if they are both non-zero length.
     *
     * @param p Array 1
     * @param v Array 2
     * @param name Name of the pair
     */
    private static void validatePair(double[] p, int[] v, String name) {
        validatePair(TestUtils.getLength(p), TestUtils.getLength(v), name);
    }

    /**
     * Validate a pair of point-value arrays have the same length if they are both non-zero length.
     *
     * @param l1 Length 1
     * @param l2 Length 2
     * @param name Name of the pair
     */
    private static void validatePair(int l1, int l2, String name) {
        // Arrays are used when non-zero in length. The lengths must match.
        if (l1 != 0 && l2 != 0 && l1 != l2) {
            throw new IllegalArgumentException(
                String.format("Points-Values length mismatch for %s: %d != %d", name, l1, l2));
        }
    }

    /**
     * Gets the parameters used to create the distribution.
     *
     * @return the parameters
     */
    Object[] getParameters() {
        return parameters;
    }

    /**
     * Gets the mean of the distribution.
     *
     * @return the mean
     */
    double getMean() {
        return mean;
    }

    /**
     * Gets the variance of the distribution.
     *
     * @return the variance
     */
    double getVariance() {
        return variance;
    }

    /**
     * Gets the absolute tolerance used when comparing expected and actual results.
     * If no tolerance exists for the named test then the default is returned.
     *
     * @param name Name of the test.
     * @return the absolute tolerance
     */
    double getAbsoluteTolerance(TestName name) {
        return getTolerance(name, INDEX_ABSOLUTE, absoluteTolerance);
    }

    /**
     * Gets the relative tolerance used when comparing expected and actual results.
     * If no tolerance exists for the named test then the default is returned.
     *
     * @param name Name of the test.
     * @return the relative tolerance
     */
    double getRelativeTolerance(TestName name) {
        return getTolerance(name, INDEX_RELATIVE, relativeTolerance);
    }

    /**
     * Gets the specified tolerance for the named test.
     * If no tolerance exists for the named test then the default is returned.
     *
     * @param name Name of the test.
     * @param index Index of the tolerance.
     * @param defaultValue Default value is the tolerance is unset.
     * @return the relative tolerance
     */
    private double getTolerance(TestName name, int index, double defaultValue) {
        final double[] tol = tolerance.get(name);
        if (tol != null && tol[index] != UNSET_TOLERANCE) {
            return tol[index];
        }
        return defaultValue;
    }

    /**
     * Gets the default absolute tolerance used when comparing expected and actual results.
     *
     * @return the absolute tolerance
     */
    double getAbsoluteTolerance() {
        return absoluteTolerance;
    }

    /**
     * Gets the default relative tolerance used when comparing expected and actual results.
     *
     * @return the relative tolerance
     */
    double getRelativeTolerance() {
        return relativeTolerance;
    }

    /**
     * Checks if the named test is disabled.
     *
     * @param name Name of the test.
     * @return true if test is disabled.
     */
    boolean isDisabled(TestName name) {
        return disabled.contains(name);
    }

    /**
     * Checks if the named test is enabled.
     *
     * @param name Name of the test.
     * @return true if test is enabled.
     */
    boolean isEnabled(TestName name) {
        return !isDisabled(name);
    }

    /**
     * Gets the expected cumulative probability values for the CDF test points.
     *
     * @return the CDF values
     */
    double[] getCdfValues() {
        return cdfValues;
    }

    /**
     * Gets the expected survival function values for the survival function test points.
     *
     * @return the SF values
     */
    double[] getSfValues() {
        return sfValues;
    }

    /**
     * Gets the expected cumulative probability values for the CDF high-precision test points.
     *
     * @return the CDF high-precision values
     */
    double[] getCdfHpValues() {
        return cdfHpValues;
    }

    /**
     * Gets the expected survival probability values for the survival function high-precision test points.
     *
     * @return the survival function high-precision values
     */
    double[] getSfHpValues() {
        return sfHpValues;
    }

    /**
     * Gets the points to evaluate the inverse CDF.
     *
     * @return the inverse CDF points
     */
    abstract double[] getIcdfPoints();

    /**
     * Gets the points to evaluate the inverse SF.
     *
     * @return the inverse SF points
     */
    abstract double[] getIsfPoints();
}

