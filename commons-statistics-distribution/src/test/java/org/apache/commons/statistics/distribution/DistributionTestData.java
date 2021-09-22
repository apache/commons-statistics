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
import java.util.Objects;
import java.util.Properties;
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

    /** The key for the support connected value. */
    static final String KEY_CONNECTED = "connected";
    /** The key for the tolerance value. */
    static final String KEY_TOLERANCE = "tolerance";
    /** The key for the high-precision tolerance value. */
    static final String KEY_TOLERANCE_HP = "tolerance.hp";

    /** Regex to split delimited text data (e.g. arrays of numbers). */
    private static final Pattern PATTERN = Pattern.compile("[ ,]+");

    /** Expected probability function values. */
    protected final double[] pfValues;
    /** Expected log probability function values. */
    protected final double[] logPfValues;
    /** Disable tests of the probability function method. */
    protected final boolean disablePf;
    /** Disable tests of the log probability function method. */
    protected final boolean disableLogPf;

    /** Distribution parameters. */
    private final Object[] parameters;
    /** Mean. */
    private final double mean;
    /** Variance. */
    private final double variance;
    /** Support connected flag. */
    private final boolean connected;
    /** Test tolerance for calculations. */
    private final double tolerance;
    /** Test tolerance for high-precision calculations.. */
    private final double hpTolerance;
    /** Expected CDF values. */
    private final double[] cdfValues;
    /** Expected SF values for the survival function test points. */
    private final double[] sfValues;
    /** Expected CDF values for the high-precision CDF test points. */
    private final double[] cdfHpValues;
    /** Expected CDF values for the high-precision survival function test points. */
    private final double[] sfHpValues;

    // Flags to ignore required tests

    /** Disable a {@code x = icdf(cdf(x))} mapping test. */
    private final boolean disableCdfInverse;
    /** Disable tests of the sample method. */
    private final boolean disableSample;
    /** Disable tests of the cumulative probability function method. */
    private final boolean disableCdf;
    /** Disable tests of the survival probability function method. */
    private final boolean disableSf;

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
            // A separate cdf.inverse property controls an inverse mapping test.
            icdfPoints = getAsDoubleArray(props, "icdf.points", null);
            icdfValues = getAsDoubleArray(props, "icdf.values", null);
            // Validation
            validatePair(cdfPoints, getCdfValues(), "cdf");
            validatePair(pdfPoints, getPdfValues(), "pdf");
            validatePair(pdfPoints, getLogPdfValues(), "logpdf");
            validatePair(sfPoints, getSfValues(), "sf");
            validatePair(cdfHpPoints, getCdfHpValues(), "cdf.hp");
            validatePair(sfHpPoints, getSfHpValues(), "sf.hp");
            validatePair(icdfPoints, icdfValues, "icdf");
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

        /**
         * Checks if a test of the PDF method is disabled.
         *
         * @return true if a PDF test is disabled.
         * @see #getPdfValues()
         */
        boolean isDisablePdf() {
            return disablePf;
        }

        /**
         * Checks if a test of the log PDF method is disabled.
         *
         * @return true if a log PDF test is disabled.
         * @see #getLogPdfValues()
         */
        boolean isDisableLogPdf() {
            return disableLogPf;
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
            // A separate cdf.inverse property controls an inverse mapping test.
            icdfPoints = getAsDoubleArray(props, "icdf.points", null);
            icdfValues = getAsIntArray(props, "icdf.values", null);
            // Validation
            validatePair(cdfPoints, getCdfValues(), "cdf");
            validatePair(pmfPoints, getPmfValues(), "pmf");
            validatePair(pmfPoints, getLogPmfValues(), "logpmf");
            validatePair(sfPoints, getSfValues(), "sf");
            validatePair(cdfHpPoints, getCdfHpValues(), "cdf.hp");
            validatePair(sfHpPoints, getSfHpValues(), "sf.hp");
            validatePair(icdfPoints, icdfValues, "icdf");
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

        /**
         * Checks if a test of the PMF method is disabled.
         *
         * @return true if a PMF test is disabled.
         * @see #getPmfValues()
         */
        boolean isDisablePmf() {
            return disablePf;
        }

        /**
         * Checks if a test of the log PMF method is disabled.
         *
         * @return true if a log PMF test is disabled.
         * @see #getLogPmfValues()
         */
        boolean isDisableLogPmf() {
            return disableLogPf;
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
        connected = getAsBoolean(props, KEY_CONNECTED);
        tolerance = getAsDouble(props, KEY_TOLERANCE);
        hpTolerance = getAsDouble(props, KEY_TOLERANCE_HP);
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
        disableCdfInverse = getAsBoolean(props, "disable.cdf.inverse", false);
        disableSample = getAsBoolean(props, "disable.sample", false);
        disablePf = getAsBoolean(props, "disable." + pf, false);
        disableLogPf = getAsBoolean(props, "disable." + pf, false);
        disableCdf = getAsBoolean(props, "disable.cdf", false);
        disableSf = getAsBoolean(props, "disable.sf", false);
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
    private static boolean getAsBoolean(Properties props, String key) {
        try {
            return Boolean.parseBoolean(props.getProperty(key));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid boolean: " + key, ex);
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
     * @param p Length 1
     * @param v Length 2
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
     * Checks if the support is connected (continuous from lower to upper bound).
     *
     * @return true if the support is connected
     */
    boolean isConnected() {
        return connected;
    }

    /**
     * Gets the tolerance used when comparing expected and actual results.
     *
     * @return the tolerance
     */
    double getTolerance() {
        return tolerance;
    }

    /**
     * Gets the tolerance used when comparing expected and actual results
     * of high-precision computations.
     *
     * @return the tolerance
     */
    double getHighPrecisionTolerance() {
        return hpTolerance;
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
     * Checks if a {@code x = icdf(cdf(x))} mapping test is disabled.
     *
     * @return true if a CDF inverse mapping test is disabled.
     */
    boolean isDisableCdfInverse() {
        return disableCdfInverse;
    }

    /**
     * Checks if a test of the sample method is disabled.
     *
     * @return true if a sample test is disabled.
     */
    boolean isDisableSample() {
        return disableSample;
    }

    /**
     * Checks if a test of the CDF method is disabled.
     *
     * @return true if a CDF test is disabled.
     * @see #getCdfValues()
     */
    boolean isDisableCdf() {
        return disableCdf;
    }
    /**
     * Checks if a test of the survival function method is disabled.
     *
     * @return true if a survival function test is disabled.
     * @see #getSfValues()
     */
    boolean isDisableSf() {
        return disableSf;
    }
}

