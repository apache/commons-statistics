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
package org.apache.commons.statistics.inference;

import java.util.EnumSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.statistics.descriptive.DoubleStatistics;
import org.apache.commons.statistics.descriptive.Statistic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for the {@link TTest}.
 */
class TTestTest {

    private interface DDLConsumer {
        void accept(double a, double b, long c);
    }

    private interface DDLDDLConsumer {
        void accept(double a, double b, long c, double d, double e, long f);
    }

    @Test
    void testInvalidOptionsThrows() {
        final TTest test = TTest.withDefaults();
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((AlternativeHypothesis) null));
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((DataDispersion) null));
        for (final double v : new double[] {Double.POSITIVE_INFINITY, Double.NaN, Double.NEGATIVE_INFINITY}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> test.withMu(v));
        }
    }

    @Test
    void testOneSampleDatasetThrows() {
        assertOneSampleDatasetThrows(TTest.withDefaults()::statistic);
        assertOneSampleDatasetThrows(TTest.withDefaults()::test);
    }

    private static void assertOneSampleDatasetThrows(DDLConsumer action) {
        final double m = 0;
        final double v = 1;
        final long n = 5;
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(m, v, 1), "values", "size");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(m, -1, n), "negative");
    }

    @ParameterizedTest
    @MethodSource
    void testOneSampleDataset(double mu, double m, double v, long n, double statistic, double[] p) {
        TTest test = TTest.withDefaults().withMu(mu);
        final double s = mu == 0 ?
            TTest.withDefaults().statistic(m, v, n) :
            test.statistic(m, v, n);
        TestUtils.assertRelativelyEquals(statistic, s, 1e-14, "statistic");
        int i = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            // Test the default if possible
            final TTest.Result r =
                h == AlternativeHypothesis.TWO_SIDED &&
                mu == 0 ?
                TTest.withDefaults().test(m, v, n) :
                test.test(m, v, n);
            Assertions.assertEquals(s, r.getStatistic(), "statistic");
            Assertions.assertEquals(n - 1, r.getDegreesOfFreedom(), "Degrees of freedom");
            TestUtils.assertProbability(p[i++], r.getPValue(), 1e-14, "p-value");
        }
    }

    static Stream<Arguments> testOneSampleDataset() {
        // Extract the mean, variance, size from the observations
        return testOneSample().map(a -> {
            final Object[] args = a.get();
            final double[] sample = (double[]) args[1];
            final Object[] args2 = new Object[args.length + 2];
            final DoubleStatistics s = DoubleStatistics.of(
                EnumSet.of(Statistic.MEAN, Statistic.VARIANCE), sample);
            final double m = s.getAsDouble(Statistic.MEAN);
            final double v = s.getAsDouble(Statistic.VARIANCE);
            args2[0] = args[0];
            args2[1] = m;
            args2[2] = v;
            args2[3] = sample.length;
            System.arraycopy(args, 2, args2, 4, args.length - 2);
            return Arguments.of(args2);
        });
    }

    @Test
    void testOneSampleThrows() {
        assertOneSampleThrows(TTest.withDefaults()::statistic);
        assertOneSampleThrows(TTest.withDefaults()::test);
    }

    private static void assertOneSampleThrows(Consumer<double[]> action) {
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[1]), "values", "size");
    }

    @ParameterizedTest
    @MethodSource
    void testOneSample(double mu, double[] x, double statistic, double[] p) {
        TTest test = TTest.withDefaults().withMu(mu);
        final double s = mu == 0 ?
            TTest.withDefaults().statistic(x) :
            test.statistic(x);
        TestUtils.assertRelativelyEquals(statistic, s, 1e-14, "statistic");
        int i = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            // Test the default if possible
            final TTest.Result r =
                h == AlternativeHypothesis.TWO_SIDED &&
                mu == 0 ?
                TTest.withDefaults().test(x) :
                test.test(x);
            Assertions.assertEquals(s, r.getStatistic(), "statistic");
            Assertions.assertEquals(x.length - 1, r.getDegreesOfFreedom(), "Degrees of freedom");
            TestUtils.assertProbability(p[i++], r.getPValue(), 1e-14, "p-value");
        }
    }

    static Stream<Arguments> testOneSample() {
        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less
        final Stream.Builder<Arguments> builder = Stream.builder();
        // R 3.4.0 t.test
        builder.add(Arguments.of(100,
            new double[] {93, 103, 95, 101, 91, 105, 96, 94, 101,  88, 98, 94, 101, 92, 95},
            -2.8197644534585268872,
            new double[] {0.013639058587288887, 0.99318047070635562, 0.0068195292936444434}));
        builder.add(Arguments.of(1,
            new double[] {4, 6, 9, -1, 6, 3, 2, 8},
            3.1142442579521292245,
            new double[] {0.016979974684470666, 0.008489987342235333, 0.99151001265776473}));
        builder.add(Arguments.of(0,
            new double[] {2, 0, 6, 6, 3, 3, 2, 3, -6, 6, 6, 6, 3, 0, 1, 1, 0, 2, 3, 3},
            3.864855355409694937,
            new double[] {0.0010432740392736656, 0.00052163701963683278, 0.99947836298036319}));
        return builder.build();
    }

    @Test
    void testPairedSampleThrows() {
        assertPairedSampleThrows(TTest.withDefaults()::pairedStatistic);
        assertPairedSampleThrows(TTest.withDefaults()::pairedTest);
    }

    private static void assertPairedSampleThrows(BiConsumer<double[], double[]> action) {
        final double[] sample = new double[5];
        final double[] tooSmall = {1.0};
        final double[] unequalSize = new double[4];
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(sample, tooSmall), "values", "size");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(tooSmall, sample), "values", "size");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(sample, unequalSize), "values", "size", "mismatch");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(unequalSize, sample), "values", "size", "mismatch");
    }

    @ParameterizedTest
    @MethodSource
    void testPairedSample(double mu, double[] x, double[] y, double statistic, double[] p) {
        TTest test = TTest.withDefaults().withMu(mu);
        final double s = mu == 0 ?
            TTest.withDefaults().pairedStatistic(x, y) :
            test.pairedStatistic(x, y);
        TestUtils.assertRelativelyEquals(statistic, s, 1e-14, "statistic");
        int i = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            // Test the default if possible
            final TTest.Result r =
                h == AlternativeHypothesis.TWO_SIDED &&
                mu == 0 ?
                TTest.withDefaults().pairedTest(x, y) :
                test.pairedTest(x, y);
            Assertions.assertEquals(s, r.getStatistic(), "statistic");
            Assertions.assertEquals(x.length - 1, r.getDegreesOfFreedom(), "Degrees of freedom");
            TestUtils.assertProbability(p[i++], r.getPValue(), 1e-14, "p-value");
        }
    }

    static Stream<Arguments> testPairedSample() {
        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less
        final Stream.Builder<Arguments> builder = Stream.builder();
        final double[] s1 = {1, 3, 5, 7};
        final double[] s2 = {0, 6, 11, 2};
        final double[] s3 = {5, 7, 8, 10};
        // R 3.4.0 t.test(s1, s2, paired=TRUE, alternative=['t', 'g', 'l'])
        builder.add(Arguments.of(0, s1, s2,
            -0.31333978072025608919,
            new double[] {0.77454429581922446, 0.61272785209038783, 0.38727214790961223}));
        builder.add(Arguments.of(0, s1, s3,
            -12.124355652982142573,
            new double[] {0.0012077024702717076, 0.99939614876486416, 0.0006038512351358538}));
        builder.add(Arguments.of(0, s3, s2,
            1.1489125293076056789,
            new double[] {0.33388698610855666, 0.16694349305427833, 0.83305650694572164}));
        // with mu
        builder.add(Arguments.of(-0.5, s1, s2,
            -0.10444659357341870565,
            new double[] {0.92340629099907356, 0.53829685450046316, 0.46170314549953678}));
        builder.add(Arguments.of(2, s1, s3,
                -19.052558883257649569,
            new double[] {0.00031573334241719503, 0.99984213332879146, 0.00015786667120859751}));
        builder.add(Arguments.of(5, s3, s2,
            -0.94001934216076843409,
            new double[] {0.41655492054205062, 0.79172253972897466, 0.20827746027102531}));
        return builder.build();
    }

    @Test
    void testTwoSampleDatasetThrows() {
        assertTwoSampleDatasetThrows(TTest.withDefaults()::statistic);
        assertTwoSampleDatasetThrows(TTest.withDefaults()::test);
    }

    private static void assertTwoSampleDatasetThrows(DDLDDLConsumer action) {
        final double m = 0;
        final double v = 1;
        final long n = 5;
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(m, v, n, m, v, 1), "values", "size");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(m, v, 1, m, v, n), "values", "size");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(m, -1, n, m, v, n), "negative");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(m, v, n, m, -1, n), "negative");
    }

    @ParameterizedTest
    @MethodSource(value = {"testTwoSampleDataset", "testTwoSampleDataset2"})
    void testTwoSampleDataset(double mu, double m1, double v1, long n1, double m2, double v2, long n2,
            boolean equal, double statistic, double df, double[] p) {
        TTest test = TTest.withDefaults().withMu(mu)
            .with(equal ? DataDispersion.HOMOSCEDASTIC : DataDispersion.HETEROSCEDASTIC);
        final double s = mu == 0 && !equal ?
            TTest.withDefaults().statistic(m1, v1, n1, m2, v2, n2) :
            test.statistic(m1, v1, n1, m2, v2, n2);
        TestUtils.assertRelativelyEquals(statistic, s, 1e-14, "statistic");
        int i = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            // Test the default if possible
            final TTest.Result r =
                h == AlternativeHypothesis.TWO_SIDED &&
                mu == 0 &&
                !equal ?
                TTest.withDefaults().test(m1, v1, n1, m2, v2, n2) :
                test.test(m1, v1, n1, m2, v2, n2);
            Assertions.assertEquals(s, r.getStatistic(), "statistic");
            TestUtils.assertRelativelyEquals(df, r.getDegreesOfFreedom(), 1e-15, "Degrees of freedom");
            TestUtils.assertProbability(p[i++], r.getPValue(), 1e-14, "p-value");
        }
    }

    static Stream<Arguments> testTwoSampleDataset() {
        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less
        // scipy.stats (version 1.9.3)
        // Note: scipy uses the standard deviation:
        // Examples are from the scipy documentation.
        // import numpy as np
        // from scipy.stats import ttest_ind_from_stats as ttest
        // ttest(15, np.sqrt(87.5), 13, 12, np.sqrt(39), 11,
        //       equal_var=True, alternative=<'two-sided', 'less', 'greater'>)
        final double mu = 0;
        final Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of(mu, 15, 87.5, 13, 12, 39, 11, true,
            0.9051358093310269, 22,
            new double[] {0.3751996797581487, 0.18759983987907436, 0.8124001601209256}));
        builder.add(Arguments.of(mu, 15, 87.5, 13, 12, 39, 11, false,
            0.9358461935556048, 20.984611233429924,
            new double[] {0.35999818693244245, 0.17999909346622123, 0.8200009065337788}));
        builder.add(Arguments.of(mu, 0.2, 0.161073, 150, 0.225, 0.175251, 200, true,
            -0.5627187905196761, 348,
            new double[] {0.5739887114209541, 0.713005644289523, 0.28699435571047704}));
        builder.add(Arguments.of(mu, 0.2, 0.161073, 150, 0.225, 0.175251, 200, false,
            -0.5661276301071694, 327.90436123021186,
            new double[] {0.5716942537704801, 0.71415287311476, 0.28584712688524005}));
        return builder.build();
    }

    static Stream<Arguments> testTwoSampleDataset2() {
        // Extract the mean, variance, length from each sample.
        // This provides data with an expected mean (mu) for the two sample dataset method.
        return testTwoSample().map(a -> {
            final Object[] args = a.get();
            final double[] x = (double[]) args[1];
            final double[] y = (double[]) args[2];
            final Object[] args2 = new Object[args.length + 4];
            final DoubleStatistics.Builder b = DoubleStatistics.builder(Statistic.MEAN, Statistic.VARIANCE);
            final DoubleStatistics s1 = b.build(x);
            final DoubleStatistics s2 = b.build(y);
            args2[0] = args[0];
            args2[1] = s1.getAsDouble(Statistic.MEAN);
            args2[2] = s1.getAsDouble(Statistic.VARIANCE);
            args2[3] = x.length;
            args2[4] = s2.getAsDouble(Statistic.MEAN);
            args2[5] = s2.getAsDouble(Statistic.VARIANCE);
            args2[6] = y.length;
            System.arraycopy(args, 3, args2, 7, args.length - 3);
            return Arguments.of(args2);
        });
    }

    @Test
    void testTwoSampleThrows() {
        assertTwoSampleThrows(TTest.withDefaults()::statistic);
        assertTwoSampleThrows(TTest.withDefaults()::test);
    }

    private static void assertTwoSampleThrows(BiConsumer<double[], double[]> action) {
        final double[] sample = new double[5];
        final double[] tooSmall = {1.0};
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(sample, tooSmall), "values", "size");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(tooSmall, sample), "values", "size");
    }

    @ParameterizedTest
    @MethodSource
    void testTwoSample(double mu, double[] x, double[] y, boolean equal, double statistic, double df, double[] p) {
        TTest test = TTest.withDefaults().withMu(mu)
            .with(equal ? DataDispersion.HOMOSCEDASTIC : DataDispersion.HETEROSCEDASTIC);
        final double s = mu == 0 && !equal ?
            TTest.withDefaults().statistic(x, y) :
            test.statistic(x, y);
        TestUtils.assertRelativelyEquals(statistic, s, 1e-14, "statistic");
        int i = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            // Test the default if possible
            final TTest.Result r =
                h == AlternativeHypothesis.TWO_SIDED &&
                mu == 0 &&
                !equal ?
                TTest.withDefaults().test(x, y) :
                test.test(x, y);
            Assertions.assertEquals(s, r.getStatistic(), "statistic");
            TestUtils.assertRelativelyEquals(df, r.getDegreesOfFreedom(), 1e-15, "Degrees of freedom");
            TestUtils.assertProbability(p[i++], r.getPValue(), 1e-14, "p-value");
        }
    }

    static Stream<Arguments> testTwoSample() {
        // p-values are in the AlternativeHypothesis enum order: two-sided, greater, less
        final Stream.Builder<Arguments> builder = Stream.builder();
        // R version 3.4.0
        // t.test(s1, s2, alternative='t', var.equal=TRUE)
        final double[] s1 = {7, -4, 18, 17, -3, -5, 1, 10, 11, -2};
        final double[] s2 = {-1, 12, -1, -3, 3, -5, 5, 2, -11, -1, -3};
        builder.add(Arguments.of(0, s1, s2, true,
            1.6341082415908594339, 19,
            new double[] {0.11869682666685942, 0.05934841333342971, 0.94065158666657034}));
        builder.add(Arguments.of(0, s1, s2, false,
            1.6037172876755148021, 15.590512968733776233,
            new double[] {0.12883936962193396, 0.064419684810966979, 0.93558031518903295}));
        final double[] s3 = {2, 4, 6, 8, 10, 97};
        final double[] s4 = {4, 6, 8, 10, 16};
        builder.add(Arguments.of(0, s3, s4, true,
            0.7309631008575527833, 9,
            new double[] {0.4833963785800246, 0.2416981892900123, 0.7583018107099877}));
        builder.add(Arguments.of(0, s3, s4, false,
            0.80568260610405273425, 5.1827667502020595691,
            new double[] {0.45578652613550491, 0.22789326306775246, 0.77210673693224752}));
        // small samples (t is the same for both variance options)
        final double[] s5 = {1, 3};
        final double[] s6 = {4, 5};
        builder.add(Arguments.of(0, s5, s6, true,
            -2.2360679774997898051, 2,
            new double[] {0.1548457452714834, 0.92257712736425823, 0.077422872635741699}));
        builder.add(Arguments.of(0, s5, s6, false,
            -2.2360679774997898051, 1.4705882352941177516,
            new double[] {0.19872738893452604, 0.90063630553273699, 0.099363694467263022}));
        // with expected mean (mu)
        builder.add(Arguments.of(-3, s1, s2, true,
            2.5638594824960039276, 19,
            new double[] {0.018991928157561552, 0.0094959640787807761, 0.99050403592121916}));
        builder.add(Arguments.of(-3, s1, s2, false,
            2.5161771237667562851, 15.590512968733776233,
            new double[] {0.023237365545493296, 0.011618682772746648, 0.98838131722725331}));
        builder.add(Arguments.of(1.5, s3, s4, true,
            0.64230180830070682241, 9,
            new double[] {0.53669320069936366, 0.26834660034968183, 0.73165339965031817}));
        builder.add(Arguments.of(1.5, s3, s4, false,
            0.70795830078145871589, 5.1827667502020595691,
            new double[] {0.50953294711549812, 0.25476647355774906, 0.74523352644225094}));
        return builder.build();
    }
}
