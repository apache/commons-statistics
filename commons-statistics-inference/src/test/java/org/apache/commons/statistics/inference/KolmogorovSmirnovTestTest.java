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

package org.apache.commons.statistics.inference;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.numbers.combinatorics.BinomialCoefficient;
import org.apache.commons.numbers.combinatorics.Combinations;
import org.apache.commons.numbers.core.ArithmeticUtils;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.NormalDistribution;
import org.apache.commons.statistics.distribution.UniformContinuousDistribution;
import org.apache.commons.statistics.inference.KolmogorovSmirnovTest.TwoResult;
import org.apache.commons.statistics.ranking.NaturalRanking;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link KolmogorovSmirnovTest}.
 */
class KolmogorovSmirnovTestTest {
    /** Machine epsilon. Used for relative error assertions. */
    private static final double EPS = Math.ulp(1.0);

    // Random N(0, 1) values generated using R rnorm (n=102)
    private static final double[] GAUSSIAN = {
        0.26055895, -0.63665233, 1.51221323, 0.61246988, -0.03013003, -1.73025682, -0.51435805, 0.70494168, 0.18242945,
        0.94734336, -0.04286604, -0.37931719, -1.07026403, -2.05861425, 0.11201862, 0.71400136, -0.52122185,
        -0.02478725, -1.86811649, -1.79907688, 0.15046279, 1.32390193, 1.55889719, 1.83149171, -0.03948003,
        -0.98579207, -0.76790540, 0.89080682, 0.19532153, 0.40692841, 0.15047336, -0.58546562, -0.39865469, 0.77604271,
        -0.65188221, -1.80368554, 0.65273365, -0.75283102, -1.91022150, -0.07640869, -1.08681188, -0.89270600,
        2.09017508, 0.43907981, 0.10744033, -0.70961218, 1.15707300, 0.44560525, -2.04593349, 0.53816843, -0.08366640,
        0.24652218, 1.80549401, -0.99220707, -1.14589408, -0.27170290, -0.49696855, 0.00968353, -1.87113545,
        -1.91116529, 0.97151891, -0.73576115, -0.59437029, 0.72148436, 0.01747695, -0.62601157, -1.00971538,
        -1.42691397, 1.03250131, -0.30672627, -0.15353992, -1.19976069, -0.68364218, 0.37525652, -0.46592881,
        -0.52116168, -0.17162202, 1.04679215, 0.25165971, -0.04125231, -0.23756244, -0.93389975, 0.75551407,
        0.08347445, -0.27482228, -0.4717632, -0.1867746, -0.1166976, 0.5763333, 0.1307952, 0.7630584, -0.3616248,
        2.1383790, -0.7946630, 0.0231885, 0.7919195, 1.6057144, -0.3802508, 0.1229078, 1.5252901, -0.8543149, 0.3025040
    };

    // Random N(0, 1.6) values generated using R rnorm (n=100)
    private static final double[] GAUSSIAN2 = {
        2.88041498038308, -0.632349445671017, 0.402121295225571, 0.692626364613243, 1.30693446815426,
        -0.714176317131286, -0.233169206599583, 1.09113298322107, -1.53149079994305, 1.23259966205809,
        1.01389927412503, 0.0143898711497477, -0.512813545447559, 2.79364360835469, 0.662008875538092,
        1.04861546834788, -0.321280099931466, 0.250296656278743, 1.75820367603736, -2.31433523590905,
        -0.462694696086403, 0.187725700950191, -2.24410950019152, 2.83473751105445, 0.252460174391016,
        1.39051945380281, -1.56270144203134, 0.998522814471644, -1.50147469080896, 0.145307533554146,
        0.469089457043406, -0.0914780723809334, -0.123446939266548, -0.610513388160565, -3.71548343891957,
        -0.329577317349478, -0.312973794075871, 2.02051909758923, 2.85214308266271, 0.0193222002327237,
        -0.0322422268266562, 0.514736012106768, 0.231484953375887, -2.22468798953629, 1.42197716075595,
        2.69988043856357, 0.0443757119128293, 0.721536984407798, -0.0445688839903234, -0.294372724550705,
        0.234041580912698, -0.868973119365727, 1.3524893453845, -0.931054600134503, -0.263514296006792,
        0.540949457402918, -0.882544288773685, -0.34148675747989, 1.56664494810034, 2.19850536566584,
        -0.667972122928022, -0.70889669526203, -0.00251758193079668, 2.39527162977682, -2.7559594317269,
        -0.547393502656671, -2.62144031572617, 2.81504147017922, -1.02036850201042, -1.00713927602786,
        -0.520197775122254, 1.00625480138649, 2.46756916531313, 1.64364743727799, 0.704545210648595,
        -0.425885789416992, -1.78387854908546, -0.286783886710481, 0.404183648369076, -0.369324280845769,
        -0.0391185138840443, 2.41257787857293, 2.49744281317859, -0.826964496939021, -0.792555379958975,
        1.81097685787403, -0.475014580016638, 1.23387615291805, 0.646615294802053, 1.88496377454523, 1.20390698380814,
        -0.27812153371728, 2.50149494533101, 0.406964323253817, -1.72253451309982, 1.98432494184332, 2.2223658560333,
        0.393086362404685, -0.504073151377089, -0.0484610869883821
    };

    // Random uniform(0, 1) generated using R runif (n=102)
    private static final double[] UNIFORM = {
        0.7930305, 0.6424382, 0.8747699, 0.7156518, 0.1845909, 0.2022326, 0.4877206, 0.8928752, 0.2293062, 0.4222006,
        0.1610459, 0.2830535, 0.9946345, 0.7329499, 0.26411126, 0.87958133, 0.29827437, 0.39185988, 0.38351185,
        0.36359611, 0.48646472, 0.05577866, 0.56152250, 0.52672013, 0.13171783, 0.95864085, 0.03060207, 0.33514887,
        0.72508148, 0.38901437, 0.9978665, 0.5981300, 0.1065388, 0.7036991, 0.1071584, 0.4423963, 0.1107071, 0.6437221,
        0.58523872, 0.05044634, 0.65999539, 0.37367260, 0.73270024, 0.47473755, 0.74661163, 0.50765549, 0.05377347,
        0.40998009, 0.55235182, 0.21361998, 0.63117971, 0.18109222, 0.89153510, 0.23203248, 0.6177106, 0.6856418,
        0.2158557, 0.9870501, 0.2036914, 0.2100311, 0.9065020, 0.7459159, 0.56631790, 0.06753629, 0.39684629,
        0.52504615, 0.14199103, 0.78551120, 0.90503321, 0.80452362, 0.9960115, 0.8172592, 0.5831134, 0.8794187,
        0.2021501, 0.2923505, 0.9561824, 0.8792248, 0.85201008, 0.02945562, 0.26200374, 0.11382818, 0.17238856,
        0.36449473, 0.69688273, 0.96216330, 0.4859432, 0.4503438, 0.1917656, 0.8357845, 0.9957812, 0.4633570,
        0.8654599, 0.4597996, 0.68190289, 0.58887855, 0.09359396, 0.98081979, 0.73659533, 0.89344777, 0.18903099,
        0.97660425
    };

    @Test
    void testInvalidOptionsThrows() {
        final KolmogorovSmirnovTest test = KolmogorovSmirnovTest.withDefaults();
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((AlternativeHypothesis) null));
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((PValueMethod) null));
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((Inequality) null));
        Assertions.assertThrows(NullPointerException.class, () ->
            test.with((UniformRandomProvider) null));
        // Not strictly positive iterations
        for (final int i : new int[] {-1, 0, Integer.MIN_VALUE}) {
            TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
                () -> test.withIterations(i), Integer.toString(i));
        }
    }

    @Test
    void testOneSampleThrows() {
        final DoubleUnaryOperator cdf = UniformContinuousDistribution.of(0, 5)::cumulativeProbability;
        final KolmogorovSmirnovTest test = KolmogorovSmirnovTest.withDefaults();
        testOneSampleThrows(x -> test.statistic(x, cdf));
        testOneSampleThrows(x -> test.test(x, cdf));
    }

    private static void testOneSampleThrows(Consumer<double[]> action) {
        // Samples must be present, i.e. length > 1
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1}), "values", "size");
        // NaN
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
                () -> action.accept(new double[] {2, Double.NaN}), "nan");
        // x is null
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null));
    }

    @ParameterizedTest
    @MethodSource
    void testOneSample(double[] x, DoubleUnaryOperator cdf,
            double[] statistic, double[] p, double eps,
            PValueMethod method) {
        KolmogorovSmirnovTest test = KolmogorovSmirnovTest.withDefaults().with(method);
        int i = 0;
        final double[] s = new double[3];
        int sign = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            // Test the default if possible
            final KolmogorovSmirnovTest.OneResult r =
                h == AlternativeHypothesis.TWO_SIDED &&
                method == PValueMethod.AUTO ?
                KolmogorovSmirnovTest.withDefaults().test(x, cdf) :
                test.test(x, cdf);
            final double d = h == AlternativeHypothesis.TWO_SIDED ?
                KolmogorovSmirnovTest.withDefaults().statistic(x, cdf) :
                test.statistic(x, cdf);
            s[i] = d;
            if (i == 0) {
                sign = r.getSign();
            } else if (i == 1) {
                Assertions.assertEquals(1, r.getSign(), "greater should be D+");
            } else {
                Assertions.assertEquals(-1, r.getSign(), "less should be D-");
            }
            Assertions.assertEquals(d, r.getStatistic(), () -> h + " test statistic");
            // Note: The asymptotic p-value computation only applies to two-sided so some
            // test cases have no data for the one-sided alternatives
            if (i < p.length) {
                TestUtils.assertRelativelyEquals(statistic[i], d, eps, () -> h + " statistic");
                TestUtils.assertProbability(p[i], r.getPValue(), eps, () -> h + " p-value");
            }
            i++;
        }
        // two-sided statistic should be one of the one-sided statistics
        if (sign > 0) {
            Assertions.assertEquals(s[0], s[1], "D != D+");
        } else if (sign < 0) {
            Assertions.assertEquals(s[0], s[2], "D != D-");
        }
    }

    static Stream<Arguments> testOneSample() {
        final DoubleUnaryOperator unitNormal = NormalDistribution.of(0, 1)::cumulativeProbability;
        final DoubleUnaryOperator unif = UniformContinuousDistribution.of(0, 1)::cumulativeProbability;
        final DoubleUnaryOperator unif05 = UniformContinuousDistribution.of(-0.5, 0.5)::cumulativeProbability;
        final Stream.Builder<Arguments> builder = Stream.builder();
        // scipy.stats 1.9.3:
        // import scipy.stats as stats
        // Normal distribution, unit normal dataset
        // stats.kstest(GAUSSIAN, stats.norm.cdf)
        builder.add(Arguments.of(GAUSSIAN, unitNormal,
                new double[] {0.0932947561266756, 0.0932947561266756, 0.020912919370449506},
                new double[] {0.3172069207622401, 0.15922206670976852, 0.9021705542981606}, 2e-15, PValueMethod.EXACT));
        // Normal distribution, unit normal small dataset
        // stats.kstest(GAUSSIAN[0:50], stats.norm.cdf)
        builder.add(Arguments.of(Arrays.copyOf(GAUSSIAN, 50), unitNormal,
                new double[] {0.0982077996946327, 0.0982077996946327, 0.04954697809945069},
                new double[] {0.6837364637283481, 0.35780875044101135, 0.757709884643873}, 2e-15, PValueMethod.EXACT));
        // Normal distribution, uniform dataset
        // stats.kstest(UNIFORM, stats.norm.cdf)
        builder.add(Arguments.of(UNIFORM, unitNormal,
                new double[] {0.5117493931609258, 0.15917204917566452, 0.5117493931609258},
                new double[] {2.6003915104391943e-25, 0.005024598348883288, 1.3001957552195971e-25}, 2e-15, PValueMethod.EXACT));
        // Uniform distribution, uniform dataset
        // stats.kstest(UNIFORM, stats.uniform.cdf)
        builder.add(Arguments.of(UNIFORM, unif,
                new double[] {0.06153833137254894, 0.03267340235294117, 0.06153833137254894},
                new double[] {0.8117325066019292, 0.7873241964635717, 0.4437336795223161}, 2e-15, PValueMethod.EXACT));
        // Uniform distribution, uniform small dataset
        // stats.kstest(UNIFORM[0:20], stats.uniform.cdf)
        builder.add(Arguments.of(Arrays.copyOf(UNIFORM, 20), unif,
                new double[] {0.1610459, 0.12779940000000006, 0.1610459},
                new double[] {0.6205703200955432, 0.4802328329414194, 0.3200426771613803}, 2e-15, PValueMethod.EXACT));
        // Offset uniform distribution, uniform dataset
        // stats.kstest(UNIFORM, stats.uniform(-0.5).cdf)
        builder.add(Arguments.of(UNIFORM, unif05,
                new double[] {0.5400666982352942, 0, 0.5400666982352942},
                new double[] {2.24649011643408e-28, 1, 1.12324505821704e-28}, 2e-15, PValueMethod.EXACT));
        // Offset uniform distribution, uniform small dataset
        // stats.kstest(UNIFORM[0:20], stats.uniform(-0.5).cdf)
        builder.add(Arguments.of(Arrays.copyOf(UNIFORM, 20), unif05,
                new double[] {0.6610459, 0, 0.6610459},
                new double[] {4.117594713484523e-09, 1, 2.0587973567422614e-09}, 2e-15, PValueMethod.EXACT));
        // Offset uniform distribution, unit normal dataset
        // stats.kstest(GAUSSIAN, stats.uniform(-0.5).cdf)
        builder.add(Arguments.of(GAUSSIAN, unif05,
                new double[] {0.3401058049019608, 0.3401058049019608, 0.2549019607843137},
                new double[] {4.940576402189508e-11, 2.470288201094754e-11, 1.2448809950376141e-06}, 2e-15, PValueMethod.EXACT));
        // Auto - always chooses exact
        builder.add(Arguments.of(GAUSSIAN, unif05,
                new double[] {0.3401058049019608, 0.3401058049019608, 0.2549019607843137},
                new double[] {4.940576402189508e-11, 2.470288201094754e-11, 1.2448809950376141e-06}, 2e-15, PValueMethod.AUTO));
        // Asymptotic
        // - two-sided return kstwobign.sf(sqrt(n) * d)
        // stats.kstest(GAUSSIAN, stats.uniform(-0.5).cdf, method='asymp')
        builder.add(Arguments.of(GAUSSIAN, unif05,
                new double[] {0.3401058049019608},
                new double[] {1.1296621105375066e-10}, 2e-15, PValueMethod.ASYMPTOTIC));
        // stats.kstest(UNIFORM, stats.uniform.cdf, method='asymp')
        builder.add(Arguments.of(UNIFORM, unif,
                new double[] {0.06153833137254894},
                new double[] {0.8345902600679251}, 2e-15, PValueMethod.ASYMPTOTIC));
        // stats.kstest(UNIFORM, stats.uniform(-0.5).cdf, method='asymp')
        builder.add(Arguments.of(UNIFORM, unif05,
                new double[] {0.5400666982352942},
                new double[] {2.8842466394305567e-26}, 2e-15, PValueMethod.ASYMPTOTIC));
        return builder.build();
    }

    @Test
    void testTwoSampleThrows() {
        final KolmogorovSmirnovTest test = KolmogorovSmirnovTest.withDefaults();
        testTwoSampleThrows(test::statistic);
        testTwoSampleThrows(test::test);
    }

    private static void testTwoSampleThrows(BiConsumer<double[], double[]> action) {
        // Samples must be present, i.e. length > 1
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1, 3}, new double[] {2}), "values", "size");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1}, new double[] {2, 4}), "values", "size");

        // NaN - small sample
        // Message current does not contain the sample name
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
                () -> action.accept(new double[] {1, 3}, new double[] {2, Double.NaN}), "nan");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(new double[] {1, Double.NaN}, new double[] {2, 4}), "nan");
        // NaN - large sample
        final double[] x = new double[10000];
        final double[] y = new double[10000];
        x[0] = Double.NaN;
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
                () -> action.accept(x, y), "sample 1", "nan");
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(y, x), "sample 2", "nan");

            // x and y is null
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null, null));

        // x or y is null
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null, new double[] {2, 4}));
        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(new double[] {1, 2}, null));
    }

    @ParameterizedTest
    @MethodSource
    void testTwoSample(double[] x, double[] y,
                       double[] statistic, double[] p, double eps,
                       PValueMethod method, Inequality inequality) {
        KolmogorovSmirnovTest test = KolmogorovSmirnovTest.withDefaults().with(method).with(inequality);
        final double[] sy = y.clone();
        Arrays.sort(sy);
        final boolean hasTies =
            Arrays.stream(x).filter(v -> Arrays.binarySearch(sy, v) >= 0).findAny().isPresent();
        int i = 0;
        final double[] s = new double[3];
        int sign = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            // Test the default if possible
            final KolmogorovSmirnovTest.TwoResult r =
                h == AlternativeHypothesis.TWO_SIDED &&
                method == PValueMethod.AUTO &&
                inequality == Inequality.NON_STRICT ?
                KolmogorovSmirnovTest.withDefaults().test(x, y) :
                test.test(x, y);

            if (!hasTies) {
                // No ties; the upper bound should be the same
                Assertions.assertEquals(r.getStatistic(), r.getUpperD(), () -> h + " upperD");
                Assertions.assertEquals(r.getPValue(), r.getUpperPValue(), () -> h + " upperPValue");
            } else if (i == 0 && statistic[0] == 0) {
                // Ties are always significant if the two-sided statistic is 0
                Assertions.assertEquals(true, r.hasSignificantTies(), () -> h + " has significant ties");
            }

            final double d = h == AlternativeHypothesis.TWO_SIDED ?
                KolmogorovSmirnovTest.withDefaults().statistic(x, y) :
                test.statistic(x, y);
            s[i] = d;
            if (i == 0) {
                sign = r.getSign();
            } else if (i == 1) {
                Assertions.assertEquals(1, r.getSign(), "greater should be D+");
            } else {
                Assertions.assertEquals(-1, r.getSign(), "less should be D-");
            }
            TestUtils.assertRelativelyEquals(statistic[i], d, eps, () -> h + " statistic");
            Assertions.assertEquals(d, r.getStatistic(), () -> h + " test statistic");
            TestUtils.assertProbability(p[i], r.getPValue(), eps, () -> h + " p-value");
            i++;
        }
        // two-sided statistic should be one of the one-sided statistics
        if (sign > 0) {
            Assertions.assertEquals(s[0], s[1], "D != D+");
        } else if (sign < 0) {
            Assertions.assertEquals(s[0], s[2], "D != D-");
        }
    }

    static Stream<Arguments> testTwoSample() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // scipy.stats 1.9.3 (uses non-strict inequality in null hypothesis)
        // stats.kstest(x, y)
        builder.add(Arguments.of(
            new double[] {6, 7, 9, 13, 19, 21, 22, 23, 24},
            new double[] {10, 11, 12, 16, 20, 27, 28, 32, 44, 54},
            new double[] {0.5, 0.5, 0},
            new double[] {0.10557708545324647, 0.05279395527073546, 1}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        builder.add(Arguments.of(
            new double[] {6, 7, 9, 13, 19, 21, 22, 23, 24, 29, 30, 34, 36, 41, 45, 47, 51, 63, 33, 91},
            new double[] {10, 11, 12, 16, 20, 27, 28, 32, 44, 54, 56, 57, 64, 69, 71, 80, 81, 88, 90},
            new double[] {0.4263157894736842, 0.4263157894736842, 0.05},
            new double[] {0.0462986609429517, 0.02314933691342261, 0.9304029304029304}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        builder.add(Arguments.of(
            new double[] {-10, -5, 17, 21, 22, 23, 24, 30, 44, 50, 56, 57, 59, 67, 73, 75, 77, 78,
                79, 80, 81, 83, 84, 85, 88, 90, 92, 93, 94, 95, 98, 100, 101, 103, 105, 110},
            new double[] {-2, -1, 0, 10, 14, 15, 16, 20, 25, 26, 27, 31, 32, 33, 34, 45, 47, 48,
                51, 52, 53, 54, 60, 61, 62, 63, 74, 82, 106, 107, 109, 11, 112, 113, 114},
            new double[] {0.4103174603174603, 0.14365079365079364, 0.4103174603174603},
            new double[] {0.0030074360223340544, 0.43092369163123856, 0.0015037180115200998}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        // MATH-1245: Cases where the D value is not viewed as distinct when they are mathematically equal.
        // Note: This test is no longer relevant as the public method to compute the p-value from
        // a d statistic (in [0, 1]) has been removed. The cases have been moved here to preserve the data.
        // Note: The one-sided results from R 3.4.0 do not match. For D+ it can return -2^-53 which
        // is an error when the result should be 0.0. Results are from scipy.stats.
        builder.add(Arguments.of(
            new double[] {0, 2, 3, 4, 5, 6, 7, 8, 9, 12},
            new double[] {1, 10, 11, 13, 14, 15, 16, 17, 18},
            new double[] {0.7888888888888889, 0.7888888888888889, 0.011111111111111112},
            new double[] {0.002749572409015134, 0.0013747862045075667, 0.9473684210526315}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 9, 10, 11, 12, 13},
            new double[] {0, 1, 3, 5, 7},
            new double[] {0.6666666666666666, 0, 0.6666666666666666},
            new double[] {0.08591408591408592, 1, 0.04295704295704296}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        builder.add(Arguments.of(
            new double[] {4, 6, 7, 8, 9, 10, 11},
            new double[] {0, 1, 2, 3, 5},
            // bug in R 3.4.0 for D+ which returns as -2^-53
            new double[] {0.8571428571428571, 0, 0.8571428571428571},
            new double[] {0.01515151515151515, 1, 0.007575757575757576}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        // Square input
        builder.add(Arguments.of(
                new double[] {0, 6, 9, 5, 2},
                new double[] {1, 8, 3, 4, 7},
                new double[] {0.2, 0.2, 0.2},
                new double[] {1, 0.83333333333333333, 0.83333333333333333}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        builder.add(Arguments.of(
                new double[] {6, 10, 11, 9, 8, 12, 19, 5, 14, 7},
                new double[] {3, 0, 2, 15, 13, 16, 4, 17, 18, 1},
                new double[] {0.5, 0.3, 0.5},
                new double[] {0.16782134274394334, 0.4195804195804196, 0.08391608391608392}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        // Identical input.
        // Note the method uses the exact P computation even though there are ties.
        builder.add(Arguments.of(
                new double[] {1, 2, 3},
                new double[] {1, 2, 3},
                new double[] {0, 0, 0},
                new double[] {1, 1, 1}, 0, PValueMethod.EXACT, Inequality.NON_STRICT));
        builder.add(Arguments.of(
                new double[] {1, 2},
                new double[] {1, 2},
                new double[] {0, 0, 0},
                new double[] {1, 0.6666666666666666, 0.6666666666666666}, 0, PValueMethod.EXACT, Inequality.STRICT));
        // Auto (chooses exact due to small sample)
        builder.add(Arguments.of(GAUSSIAN, GAUSSIAN2,
                new double[] {0.2023529411764706, 0.2023529411764706, 0.06},
                new double[] {0.026098802349515924, 0.013049427737810542, 0.6597327117836644}, 1e-13, PValueMethod.AUTO, Inequality.NON_STRICT));
        // Asymptotic
        // - compute en = n*m/(n+m)
        // - two-sided return kstwo.sf(d, np.round(en))
        // - one-sided: Hodges Eq 5.3
        // -- z = np.sqrt(en) * d
        // -- return np.exp(-2 * z**2 - 2 * z * (m + 2*n)/np.sqrt(m*n*(m+n))/3.0)
        builder.add(Arguments.of(GAUSSIAN, GAUSSIAN2,
                new double[] {0.2023529411764706, 0.2023529411764706, 0.06},
                new double[] {0.028514899820936468, 0.013077034357100743, 0.6548386977618049}, 1e-13, PValueMethod.ASYMPTOTIC, Inequality.NON_STRICT));
        // Note scipy does not switch the asymptotic so it must explicitly be requested.
        // stats.kstest([1,2,2,2,3], [0,2,4], method='asymp')
        builder.add(Arguments.of(
                new double[] {1, 2, 2, 2, 3},
                new double[] {0, 2, 4},
                new double[] {1 / 3.0, 1 / 3.0, 1 / 3.0},
                new double[] {0.9444444444444444, 0.48567178524771226, 0.4856717852477124}, 1e-15, PValueMethod.ASYMPTOTIC, Inequality.NON_STRICT));
        builder.add(Arguments.of(
                new double[] {1, 2, 2, 2, 3},
                new double[] {2, 2, 4},
                new double[] {1 / 3.0, 1 / 3.0, 0},
                new double[] {0.9444444444444444, 0.48567178524771226, 1}, 1e-15, PValueMethod.ASYMPTOTIC, Inequality.NON_STRICT));
        builder.add(Arguments.of(
                new double[] {1, 2, 2, 5, 6},
                new double[] {0, 2, 2, 3, 3, 3},
                new double[] {0.4, 0.1, 0.4},
                new double[] {0.5946666666666665, 0.8594048608885093, 0.28348216705243917}, 1e-15, PValueMethod.ASYMPTOTIC, Inequality.NON_STRICT));
        // Auto chooses exact even in the presence of ties.
        builder.add(Arguments.of(
                new double[] {1, 2, 2, 2, 3},
                new double[] {0, 2, 4},
                new double[] {1 / 3.0, 1 / 3.0, 1 / 3.0},
                new double[] {0.9642857142857143, 0.5892857142857143, 0.5892857142857143}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        builder.add(Arguments.of(
                new double[] {1, 2, 2, 2, 3},
                new double[] {2, 2, 4},
                new double[] {1 / 3.0, 1 / 3.0, 0},
                new double[] {0.9642857142857143, 0.5892857142857143, 1}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        builder.add(Arguments.of(
                new double[] {1, 2, 2, 5, 6},
                new double[] {0, 2, 2, 3, 3, 3},
                new double[] {0.4, 0.1, 0.4},
                new double[] {0.6883116883116883, 0.8571428571428571, 0.35714285714285715}, 1e-15, PValueMethod.EXACT, Inequality.NON_STRICT));
        return builder.build();
    }

    /**
     * Checks exact p-value computations using critical values from Table 9 in V.K Rohatgi, An
     * Introduction to Probability and Mathematical Statistics, Wiley, 1976, ISBN 0-471-73135-8.
     * Verifies the inequality exactP(criticalValue, n, m, true) < alpha < exactP(criticalValue, n,
     * m, false).
     *
     * <p>Note that the validity of this check depends on the fact that alpha lies strictly between two
     * attained values of the distribution and that criticalValue is one of the attained values. The
     * critical value table (reference below) uses attained values. This test therefore also
     * verifies that criticalValue is attained.
     *
     * @param n first sample size
     * @param m second sample size
     * @param criticalValue critical value (D * n * m)
     * @param alpha significance level
     */
    @ParameterizedTest
    @MethodSource
    void testTwoSampleTwoSidedExactPCritical(int n, int m, long criticalValue, double alpha) {
        final int gcd = ArithmeticUtils.gcd(n, m);
        Assertions.assertTrue(KolmogorovSmirnovTest.twoSampleExactP(criticalValue, n, m, gcd, true, true) < alpha);
        Assertions.assertTrue(KolmogorovSmirnovTest.twoSampleExactP(criticalValue, n, m, gcd, false, true) > alpha);
    }

    static Stream<Arguments> testTwoSampleTwoSidedExactPCritical() {
        return Stream.of(
            Arguments.of(4, 6, 20, 0.01),  // d = 20 / 24 =  5 /  6
            Arguments.of(4, 7, 17, 0.2),   // d = 17 / 28
            Arguments.of(6, 7, 29, 0.05),  // d = 29 / 42
            Arguments.of(4, 10, 28, 0.05), // d = 28 / 40 =  7 / 10
            Arguments.of(5, 15, 55, 0.02), // d = 55 / 75 = 11 / 15
            Arguments.of(9, 10, 62, 0.01), // d = 62 / 90 = 31 / 45
            Arguments.of(7, 10, 43, 0.05)  // d = 43 / 70
        );
    }

    @ParameterizedTest
    @MethodSource
    void testTwoSampleTwoSidedExactP(int n, int m, long dnm, boolean strict, double p, double eps) {
        final int gcd = ArithmeticUtils.gcd(n, m);
        final double p1 = KolmogorovSmirnovTest.twoSampleExactP(dnm, n, m, gcd, strict, true);
        if (p < 0) {
            Assertions.assertEquals(p, p1);
        } else {
            TestUtils.assertProbability(p, p1, eps, "p-value");
        }
    }

    static Stream<Arguments> testTwoSampleTwoSidedExactP() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Hodges (1958) Table 1
        // Note that tabulated value of d is (dnm / gcd) so here we multiply by gcd
        builder.add(Arguments.of(6, 7, 36, false, 0.00816, 1e-3));
        builder.add(Arguments.of(6, 8, 21 * 2, false, 0.00466, 1e-3));
        builder.add(Arguments.of(7, 8, 26, false, 0.31313, 1e-5));
        builder.add(Arguments.of(7, 8, 27, false, 0.25221, 2e-5));
        builder.add(Arguments.of(7, 8, 28, false, 0.19254, 1e-5));
        builder.add(Arguments.of(7, 8, 35, false, 0.05594, 1e-4));
        builder.add(Arguments.of(7, 10, 50, false, 0.01399, 3e-4));
        builder.add(Arguments.of(7, 10, 53, false, 0.00946, 1e-3));
        builder.add(Arguments.of(7, 10, 56, false, 0.00452, 2e-3));
        builder.add(Arguments.of(8, 10, 22 * 2, false, 0.09511, 1e-4));
        builder.add(Arguments.of(8, 10, 23 * 2, false, 0.07043, 1e-4));
        builder.add(Arguments.of(9, 10, 54, false, 0.03027, 2e-4));
        builder.add(Arguments.of(9, 10, 63, false, 0.00704, 1e-3));
        builder.add(Arguments.of(12, 16, 30 * 4, false, 0.00525, 1e-3));
        builder.add(Arguments.of(12, 16, 31 * 4, false, 0.00331, 1e-3));
        builder.add(Arguments.of(15, 20, 31 * 5, false, 0.01363, 1e-3));

        // Test limits
        // Note the computation is possible very far above the limit
        // of binom(n+m, m) @ n=m=515.
        // Compare with the approximateP. Cases chosen so p is > 1e-16
        // since the approximateP is not very close when p is small.
        // gcd(516, 515) = 1; n*m = 265740 (2^18.0)
        addTwoSampleTwoSidedExactPArguments(builder, 516, 515, 30000, 2e-2);
        addTwoSampleTwoSidedExactPArguments(builder, 516, 515, 50000, 1e-2);
        // gcd(1234, 1112) = 2; n*m/gcd = 686104 (2^19.4)
        addTwoSampleTwoSidedExactPArguments(builder, 1234, 1112, 50000, 1.5e-2);
        addTwoSampleTwoSidedExactPArguments(builder, 1234, 1112, 100000, 2e-3);
        // gcd(12345, 11112) = 3; n*m/gcd = 45725880 (2^25.4)
        addTwoSampleTwoSidedExactPArguments(builder, 12345, 11112, 1000000, 5e-3);
        addTwoSampleTwoSidedExactPArguments(builder, 12345, 11112, 2000000, 8e-3);
        // gcd(123456, 111112) = 8; n*m/gcd = 1714680384 (2^30.7)
        addTwoSampleTwoSidedExactPArguments(builder, 123456, 111112, 10000000, 2e-3);
        addTwoSampleTwoSidedExactPArguments(builder, 123456, 111112, 20000000, 3e-3);

        // Case where the non-square computation is not attempted
        // gcd(2^31-1, 37) = 1; n*m/gcd = 79456894939 (2^36.2)
        // binom overflows when any min(n, m) >= 37 when max(n, m) == 2^31 -1.
        builder.add(Arguments.of(Integer.MAX_VALUE, 37, 3L * Integer.MAX_VALUE, false, -1, 0));
        builder.add(Arguments.of(Integer.MAX_VALUE, 37, 3L * Integer.MAX_VALUE, true, -1, 0));

        // Square underflow. These match the 2 * p1 where p1 is the one-sided p-value
        builder.add(Arguments.of(538, 538, 538 * 538, false, 2 * 4.9E-323, 0));
        builder.add(Arguments.of(539, 539, 539 * 539, false, 2 * 1.5E-323, 0));
        builder.add(Arguments.of(540, 540, 540 * 540, false, 2 * 4.9E-324, 0));
        builder.add(Arguments.of(541, 541, 541 * 541, false, 0, 0));
        builder.add(Arguments.of(1000, 1000, 1000 * 1000, false, 0, 0));
        return builder.build();
    }

    /**
     * Add arguments for the two-sample two-sided exact P comutation using
     * the approximate p computation for a reference. The input d is the integral
     * statistic in [0, lcm].
     */
    private static void addTwoSampleTwoSidedExactPArguments(
            Stream.Builder<Arguments> builder, int n, int m, int d, double eps) {
        final int gcd = ArithmeticUtils.gcd(n, m);
        final long dnm = d * gcd;
        final double x = dnm / ((double) n * m);
        final double p = KolmogorovSmirnovTest.twoSampleApproximateP(x, n, m, true);
        //TestUtils.printf("%d %d %d %s%n", n, m, d, p);
        builder.add(Arguments.of(n, m, dnm, false, p, eps));
    }

    /**
     * Test two-sample two-sided exact P vs data generated using the Commons Math implementation.
     * This supports both strict and non-strict options.
     *
     * <p>Note: The CM implementation computed 1 - CDF. Thus the relative error is higher
     * as p approaches 0 as the new implementation directly computes the SF (more accurate).
     * However the small sample sizes ensure the p-value is never too small and the test
     * checks the boundary conditions for strict and non-strict function identically in the
     * two versions.
     */
    @ParameterizedTest
    @CsvFileSource(resources = {"ks.twosample.twosided.small.cm.txt"}, delimiter = ' ')
    void testTwoSampleTwoSidedExactPSmall(int m, int n, long dmn, double p, double pStrict) {
        final int gcd = ArithmeticUtils.gcd(n, m);
        final double p2 = KolmogorovSmirnovTest.twoSampleExactP(dmn, n, m, gcd, false, true);
        TestUtils.assertProbability(p, p2, p > 0.5 ? 2 * EPS : 8e-14, "two-sided");
        final double p3 = KolmogorovSmirnovTest.twoSampleExactP(dmn, n, m, gcd, true, true);
        TestUtils.assertProbability(pStrict, p3, p > 0.5 ? 2 * EPS : 8e-14, "two-sided strict");
    }

    @ParameterizedTest
    @MethodSource(value = {"testTwoSampleTwoSidedApproximateP"})
    void testTwoSampleTwoSidedApproximateP(int n, int m, double criticalValue, double alpha, double epsilon, double ignored) {
        TestUtils.assertProbability(alpha, KolmogorovSmirnovTest.twoSampleApproximateP(criticalValue, n, m, true), epsilon, "approximateP");
    }

    @ParameterizedTest
    @MethodSource(value = {"testTwoSampleTwoSidedApproximateP"})
    void testTwoSampleTwoSidedApproximatePKSsum(int n, int m, double criticalValue, double alpha, double ignored, double epsilon) {
        final double x = criticalValue * Math.sqrt(((double) m * n) / ((double) m + n));
        TestUtils.assertProbability(alpha, KolmogorovSmirnovDistribution.ksSum(x), epsilon, "KS sum");
    }

    static Stream<Arguments> testTwoSampleTwoSidedApproximateP() {
        final Stream.Builder<Arguments> builder = Stream.builder();

        // This method is used to test the approximate p-value
        // using the KolmogorovSmirnovDistribution.Two.sf
        // and KolmogorovSmirnovDistribution.ksSum.
        // The KS sum is close to the asymptotic limit. The Two.sf
        // is closer to the exact p-value computed using
        // KolmogorovSmirnovTest.exactP for a range of integral d-statistic
        // where p is in the range 0.001 to 0.1.

        // From Wikipedia KS article.
        // This data is for very large n where the asymptotic series reduces to a sum of a
        // single term: 2 exp(-2 z^2); z = d * sqrt(n*m / (n+m))
        final double tol = 1.5e-2;
        final double tolKSsum = 1.5e-4;
        final double[] alpha = {
            0.10, 0.05, 0.025, 0.01, 0.005, 0.001
        };
        final double[] c = {
            // sqrt(-log(alpha/2) / 2)
            1.2238734153404083, 1.3581015157406195, 1.4802071873007983,
            1.6276236307187293, 1.7308183826022854, 1.9494746035204051
        };
        final int[] k = {
            // Very large n
            10000, 50000, 100000
        };
        int n;
        int m;
        for (int i = 0; i < k.length; i++) {
            n = k[i];
            for (int j = 0; j < i; j++) {
                m = k[j];
                for (int l = 0; l < alpha.length; l++) {
                    final double dCrit = c[l] * Math.sqrt((n + m) / ((double) n * m));
                    builder.add(Arguments.of(n, m, dCrit, alpha[l], tol, tolKSsum));
                }
            }
        }

        // Edge case with d=0
        builder.add(Arguments.of(10, 10, 0.0, 1, 0, 0));
        builder.add(Arguments.of(100, 100, 0.0, 1, 0, 0));

        // scipy uses the two-sided one sample for an approximate p:
        // from scipy.stats import distributions
        // import numpy as np
        // stats.kstwo.sf(d, np.round(m * n / (m + n)))
        // This is more accurate as p << 1e-16.
        builder.add(Arguments.of(10000, 10000, 0.02, 0.03613941395325637, 8e-15, 0.02));
        builder.add(Arguments.of(10000, 10000, 0.01, 0.6954557253913853, 1e-15, 6e-3));
        builder.add(Arguments.of(10000, 10000, 0.008, 0.9035907876780276, 1e-15, 3e-3));
        builder.add(Arguments.of(10000, 10000, 0.007, 0.9656426076715481, 1e-15, 2e-3));
        builder.add(Arguments.of(10000, 10000, 0.006, 0.9933190457003288, 1e-15, 5e-4));
        builder.add(Arguments.of(10000, 10000, 0.005, 0.9995858441477996, 1e-15, 5e-5));
        builder.add(Arguments.of(10000, 11000, 0.008, 0.8880643485565741, 1e-15, 4e-3));
        builder.add(Arguments.of(10000, 11000, 0.007, 0.9579679709076735, 1e-15, 2e-3));
        builder.add(Arguments.of(10000, 11000, 0.006, 0.9911437365335491, 1e-15, 6e-4));

        return builder.build();
    }

    /**
     * Test the two-sample two-sided approximations.
     * The p-value of the approximations are compared to the exact p-value over a range
     * of p typical for critical alpha values. The root mean square error is computed
     * and the used to check the current approximation is the best choice.
     */
    @ParameterizedTest
    @CsvSource({
        // Note: The Two.sf approximation is worse on the square and generally
        // better on rectangles. See testApprox.
        //"16, 16, 0.001, 0.1, 500",
        //"16, 32, 0.001, 0.1, 500",
        //"2048, 2048, 0.001, 0.1, 500",
        "2048, 2047, 0.001, 0.1, 500",
        "1024, 1023, 0.001, 0.1, 500",
        "512, 511, 0.001, 0.1, 500",
    })
    void testTwoSampleTwoSidedApproximations(int n, int m, double pMin, double pMax, int samples) {
        // Note: the integral statistic is lower for a larger p-value
        // D increments should be a factor of the gcd
        final int gcd = ArithmeticUtils.gcd(n, m);
        final long upper = findTwoSampleD(pMin, n, m, 0, Long.MAX_VALUE, false, gcd);
        final long lower = findTwoSampleD(pMax, n, m, 0, upper, true, gcd);
        int c = 0;
        double m1 = 0;
        double m2 = 0;
        // Compute desired step size and round to nearest factor of gcd
        long step = gcd * Math.round((double) (upper - lower) / samples / gcd);
        step = step == 0 ? gcd : step;
        for (long dnm = lower; dnm <= upper; dnm += step) {
            final double p = KolmogorovSmirnovTest.twoSampleExactP(dnm, n, m, gcd, false, true);
            // Ignore p-values outside the range of interest
            if (p < pMin || p > pMax) {
                continue;
            }
            final double x = dnm / ((double) n * m);
            final double en = ((double) m * n) / ((double) m + n);
            final double p1 = KolmogorovSmirnovDistribution.Two.sf(x, (int) Math.round(en));
            final double e1 = Math.abs(p - p1) / Math.max(p, p1);
            final double p2 = KolmogorovSmirnovDistribution.ksSum(x * Math.sqrt(en));
            final double e2 = Math.abs(p - p2) / Math.max(p, p2);
            m1 += e1 * e1;
            m2 += e2 * e2;
            c++;
        }
        final int count = c;
        Assertions.assertTrue(c > 30, () -> "Not enough samples: " + count);
        final double rmsd1 = Math.sqrt(m1 / c);
        final double rmsd2 = Math.sqrt(m2 / c);
        Assertions.assertTrue(rmsd1 <= rmsd2, () -> String.format("RMSD Two.sf %s > KS sum %s", rmsd1, rmsd2));
    }

    @Disabled("Not a test. It is used to explore the RMSD of different asymptotic two-sample p-values.")
    @ParameterizedTest
    @CsvSource({
        //"2048, 2048",
        //"2048, 2047",
        //"153, 235",
        //"159, 235",
        "239, 235",
        "238, 235",
        "237, 235",
        "236, 235",
        "235, 235",
        "234, 235",
        "233, 235",
        "232, 235",
        "256, 256",
        "256, 255",
        "256, 254",
        "256, 253",
        "256, 252",
        "256, 251",
        "256, 250",
        "256, 249",
        "256, 248",
        "256, 128",
        "256, 64",
        "256, 32",
    })
    void testApprox(int n, int m) {
        final int max = 500;
        final double lowerP = 0.001;
        final double upperP = 0.1;
        final int gcd = ArithmeticUtils.gcd(n, m);
        final long z = findTwoSampleD(lowerP, n, m, 0, Long.MAX_VALUE, false, gcd);
        final long z2 = findTwoSampleD(upperP, n, m, 0, z, true, gcd);
        TestUtils.printf("%5d %5d %5d : %s : %7d (%8.3g) : %s : %7d (%8.3g)", n, m, gcd,
                lowerP, z, KolmogorovSmirnovTest.twoSampleExactP(z, n, m, gcd, false, true),
                upperP, z2, KolmogorovSmirnovTest.twoSampleExactP(z2, n, m, gcd, false, true));
        int t = 0;
        int c = 0;
        double m1 = 0;
        double m2 = 0;
        double maxe1 = 0;
        double maxe2 = 0;
        long maxd1 = 0;
        long maxd2 = 0;
        // Compute desired step size and round to nearest factor of gcd
        long step = gcd * Math.round((double) (z - z2) / max / gcd);
        step = step == 0 ? gcd : step;
        for (long dnm = z2; dnm <= z; dnm += step) {
            final double p = KolmogorovSmirnovTest.twoSampleExactP(dnm, n, m, gcd, false, true);
            if (p == 0 || p < Double.MIN_NORMAL || p < 1e-16) {
                continue;
            }
            if (p == 1) {
                break;
            }
            //TestUtils.printf("%s", p);
            double x = dnm / ((double) n * m);
            final double en = ((double) m * n) / ((double) m + n);
            final double p1 = KolmogorovSmirnovDistribution.Two.sf(x, (int) Math.round(en));
            final double e1 = Math.abs(p - p1) / Math.max(p, p1);
            x *= Math.sqrt(en);
            // From wikipedia:
            //x = x + 1.0 / (6 * Math.sqrt(en)) + (x - 1) / (4 * en);
            final double p2 = KolmogorovSmirnovDistribution.ksSum(x);
            final double e2 = Math.abs(p - p2) / Math.max(p, p2);
            if (e1 > maxe1) {
                maxe1 = e1;
                maxd1 = dnm;
            }
            if (e2 > maxe2) {
                maxe2 = e2;
                maxd2 = dnm;
            }
            m1 += e1 * e1;
            m2 += e2 * e2;
            t++;
            if (e2 <= e1) {
                c++;
            }
        }
        TestUtils.printf(" : %3d/%3d = %8.3g  : %12.6g (%12.6g @ %7d) : %12.6g (%12.6g @ %7d)%n",
            c, t, (double) c / t,
            Math.sqrt(m1 / t), maxe1, maxd1,
            Math.sqrt(m2 / t), maxe2, maxd2);
    }

    /**
     * Search for the integral D statistic (two-sided) for the given p value.
     *
     * @param p Target probability.
     * @param n First sample size.
     * @param m Second sample size.
     * @param a Lower bound on the search range.
     * @param b Upper bound on the search range.
     * @param above True to return the d value for the closest p-value above the
     * target
     * @param gcd Greatest common divisor of n and m. Defines the minimum step size.
     * @return Integral D statistic d*n*m
     */
    private static long findTwoSampleD(double p, int n, int m, long a, long b, boolean above, int gcd) {
        long l = (Math.max(0, a) / gcd);
        // ceil((b, n*m)/gcd)
        long h = ((Math.min(b, (long) n * m) + gcd - 1) / gcd);
        while (l + 1 < h) {
            final long mid = (l + h) >>> 1;
            final double x = KolmogorovSmirnovTest.twoSampleExactP(mid * gcd, n, m, gcd, false, true);
            // Higher values of d give a lower p
            if (x < p) {
                h = mid;
            } else {
                l = mid;
            }
        }
        return (above ? l : h) * gcd;
    }

    /**
     * MATH-1181
     * Verify that large sample method is selected for sample product > Integer.MAX_VALUE
     * (integer overflow in sample product)
     */
    @Test
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    void testTwoSampleProductSizeOverflow() {
        final int n = 46341;
        Assertions.assertTrue(n * n < 0);
        final double[] x = new double[n];
        final double[] y = new double[n];
        Assertions.assertFalse(Double.isNaN(KolmogorovSmirnovTest.withDefaults().test(x, y).getPValue()));
    }

    /**
     * Checks that ties in the data are resolved deterministically (i.e. repeatable).
     * Note: The implementation in o.a.c.math4 resolved ties using random jitter of values;
     * jitter was applied to all values, even those that were not tied thus could corrupting
     * the data order if values were very close.
     * This is no longer performed. The count of ties is returned and a method to estimate
     * p is provided separately. This test uses all known cases where jitter was
     * previously an issue (small delta between values; infinities; all values are equal).
     * The test now asserts that the behaviour is deterministic and the tie count is
     * correct.
     */
    @ParameterizedTest
    @MethodSource
    void testTwoSampleWithTies(double[] x, double[] y) {
        final TwoResult r = KolmogorovSmirnovTest.withDefaults().test(x, y);
        Arrays.sort(y);
        final boolean hasTies =
            Arrays.stream(x).filter(v -> Arrays.binarySearch(y, v) >= 0).findAny().isPresent();
        if (hasTies && r.getStatistic() == 0) {
            // Ties are always significant if the two-sided statistic is 0
            Assertions.assertEquals(true, r.hasSignificantTies(), "has significant ties");
        }

        // Should be the same if the ranking order is the same.
        final double[] combined = DoubleStream.concat(Arrays.stream(x), Arrays.stream(y)).toArray();
        new NaturalRanking().apply(combined);
        // Extract the ranks for x and y
        final double[] rx = Arrays.copyOf(combined, x.length);
        final double[] ry = Arrays.copyOfRange(combined, x.length, combined.length);
        final TwoResult r2 = KolmogorovSmirnovTest.withDefaults().test(rx, ry);
        Assertions.assertEquals(r.getStatistic(), r2.getStatistic(), "statistic");
        Assertions.assertEquals(r.getPValue(), r2.getPValue(), "p-value");
        Assertions.assertEquals(r.getUpperD(), r2.getUpperD(), "upper D");
        Assertions.assertEquals(r.getUpperPValue(), r2.getUpperPValue(), "upper P");
        Assertions.assertEquals(r.hasSignificantTies(), r2.hasSignificantTies(), "has significant ties");

        // Should be deterministic
        Assertions.assertEquals(r.getPValue(), KolmogorovSmirnovTest.withDefaults().test(y, x).getPValue(), "Sensitive to array input order");
    }

    static Stream<Arguments> testTwoSampleWithTies() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Identical
        builder.add(Arguments.of(new double[] {1, 2, 3}, new double[] {1, 2, 3}));
        // Tied for their entire length, but different lengths
        builder.add(Arguments.of(new double[] {1, 1, 1, 1, 1}, new double[] {1, 1}));
        builder.add(Arguments.of(new double[] {1, 1, 1}, new double[] {1, 1, 1, 1}));
        // Some ties
        builder.add(Arguments.of(new double[] {0, 1, 2, 3, 4, 2}, new double[] {5, 6, 7, 8, 1, 2}));
        builder.add(Arguments.of(new double[] {0, 1, 1, 4, 0}, new double[] {0, 5, 0.5, 0.55, 7}));
        builder.add(Arguments.of(new double[] {1, 1, 0, 1, 0}, new double[] {0, 0, 0}));
        // Ties to the end
        builder.add(Arguments.of(new double[] {-1, 1, 1, 1}, new double[] {-2, 1, 1, 1}));
        // Ties from the start
        builder.add(Arguments.of(new double[] {1, 1, 1, 2}, new double[] {1, 1, 1, 3}));
        // Ties at the start/end respectively
        builder.add(Arguments.of(new double[] {1, 1, 1, 2}, new double[] {0, 1, 1, 1}));
        // This cannot be resolved by random jitter
        builder.add(Arguments.of(
            DoubleStream.of(0, 1, 2, 3, 4, 2).map(i -> Double.MIN_VALUE * i).toArray(),
            DoubleStream.of(5, 6, 7, 8, 1, 2).map(i -> Double.MIN_VALUE * i).toArray()));
        // MATH-1405. very small minDeleta between values
        builder.add(Arguments.of(new double[] {
            0.0, 0.0,
            1.0, 1.0,
            1.5,
            1.6,
            1.7,
            1.8,
            1.9,
            2.0,
            2.000000000000001
        }, new double[] {
            0.0, 0.0,
            10.0, 10.0,
            11.0, 11.0, 11.0,
            15.0,
            16.0,
            17.0,
            18.0,
            19.0,
            20.0,
            20.000000000000001
        }));
        // MATH-1405. minDelta too small for random jitter to have an effect
        builder.add(Arguments.of(new double[] {
            0.0, 0.0,
            1.0, 1.0,
            1.5,
            1.6,
            1.7,
            1.8,
            1.9,
            2.0,
            2.000000000000001
        }, new double[] {
            0.0, 0.0,
            10.0, 10.0,
            11.0, 11.0, 11.0,
            15.0,
            16.0,
            17.0,
            18.0,
            19.0,
            20.0,
            20.000000000000001
        }));
        // Finites and infinities
        builder.add(Arguments.of(new double[] {
            1, 1,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY
        }, new double[] {
            1, 1,
            3, 3,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        }));
        // Only infinities
        builder.add(Arguments.of(new double[] {
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY
        }, new double[] {
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        }));
        return builder.build();
    }

    @Test
    void testTwoSampleWithManyTies() {
        // MATH-1197
        // Computation of the incorrect d statistic in the event of ties
        final double[] x = {
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 2.202653, 2.202653, 2.202653, 2.202653, 2.202653,
            2.202653, 2.202653, 2.202653, 2.202653, 2.202653, 2.202653,
            2.202653, 2.202653, 2.202653, 2.202653, 2.202653, 2.202653,
            2.202653, 2.202653, 2.202653, 2.202653, 2.202653, 2.202653,
            2.202653, 2.202653, 2.202653, 2.202653, 2.202653, 2.202653,
            2.202653, 2.202653, 2.202653, 2.202653, 2.202653, 2.202653,
            3.181199, 3.181199, 3.181199, 3.181199, 3.181199, 3.181199,
            3.723539, 3.723539, 3.723539, 3.723539, 4.383482, 4.383482,
            4.383482, 4.383482, 5.320671, 5.320671, 5.320671, 5.717284,
            6.964001, 7.352165, 8.710510, 8.710510, 8.710510, 8.710510,
            8.710510, 8.710510, 9.539004, 9.539004, 10.720619, 17.726077,
            17.726077, 17.726077, 17.726077, 22.053875, 23.799144, 27.355308,
            30.584960, 30.584960, 30.584960, 30.584960, 30.751808
        };

        final double[] y = {
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
            0.000000, 0.000000, 0.000000, 2.202653, 2.202653, 2.202653,
            2.202653, 2.202653, 2.202653, 2.202653, 2.202653, 3.061758,
            3.723539, 5.628420, 5.628420, 5.628420, 5.628420, 5.628420,
            6.916982, 6.916982, 6.916982, 10.178538, 10.178538, 10.178538,
            10.178538, 10.178538
        };


        final TwoResult r = KolmogorovSmirnovTest.withDefaults().test(x, y);
        // Computed using R version 3.4.0: ks.test(x, y)
        // D = 0.064039408866995120584, p-value = 0.97927772901338495
        // Warning: 'p-value will be approximate in the presence of ties'
        // Computed using scipy.stats.kstest
        // kstest(x, y, method='asymp')
        // KstestResult(statistic=0.0640394088669951, pvalue=0.969490633213205)
        // kstest(x, y, method='exact')
        // KstestResult(statistic=0.0640394088669951, pvalue=0.9659836534406034)
        // R uses the ksSum for asymptotic two-sided
        // scipy uses the two-sided one-sample kstwo.sf
        // Here we return the exact P: it is the statistic that is invalidated by the ties
        TestUtils.assertRelativelyEquals(0.0640394088669951, r.getStatistic(), 1e-15, "statistic");
        TestUtils.assertRelativelyEquals(0.9659836534406034, r.getPValue(), 1e-15, "p-value");

        Assertions.assertEquals(true, r.hasSignificantTies(), "has significant ties");

        // upper D == 0.6891891891891891
        // upper P == 2.1202536288385873E-25
        Assertions.assertTrue(r.getUpperD() > r.getStatistic(), "upper D");
        Assertions.assertTrue(r.getUpperPValue() < r.getPValue(), "upper D");

        // Test a p-value estimation:
        // o.a.c.m.stat.inference.KolmogorovSmirnovTest.bootstrap = 0.558364 (with 1000000 iterations)
        //
        // R 3.4.0, ks.boot implemented in Matching (Version 4.10.8).
        // require('Matching')
        // ks.boot(x, y, nboots=1000000)
        // ks.boot.pvalue: 0.55919
        //
        // The p-value is very variable so relative epsilon is for 2 significant digits.
        // Even with such low tolerance the test is sensitive to the RNG and a fixed seed
        // RNG is used for each case. This method is a general estimate of the p-value.
        final TwoResult r2 = KolmogorovSmirnovTest.withDefaults()
            .with(PValueMethod.ESTIMATE)
            .with(RandomSource.SPLIT_MIX_64.create(12345))
            .test(x, y);
        Assertions.assertEquals(r.getStatistic(), r2.getStatistic(), "statistic mismatch");
        TestUtils.assertRelativelyEquals(0.558364, r2.getPValue(), 1e-2, "estimated p-value");
        TestUtils.assertRelativelyEquals(0.55919, r2.getPValue(), 1e-2, "estimated p-value");
    }

    @Test
    void testTwoSampleWithTiesAndNaN() {
        // Cf. MATH-1405
        final double[] x = {1, Double.NaN, 3, 4};
        final double[] y = {1, 2, 3, 4};
        assertThrowsIllegalArgumentException(x, y);
        assertThrowsIllegalArgumentException(y, x);
    }

    private static void assertThrowsIllegalArgumentException(double[] x, double[] y) {
        final KolmogorovSmirnovTest test = KolmogorovSmirnovTest.withDefaults();
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> test.statistic(x, y), "statistic");
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> test.test(x, y), "test");
    }

    @Test
    void testTwoSamplesAllEqual() {
        final boolean twoSided = true;
        for (int i = 2; i < 30; i++) {
            // testing values with ties
            final double[] values = new double[i];
            Arrays.fill(values, i);
            // testing values without ties (within samples; there are ties between samples)
            final double[] ascendingValues = new double[i];
            for (int j = 0; j < ascendingValues.length; j++) {
                ascendingValues[j] = j;
            }

            Assertions.assertEquals(0.0, KolmogorovSmirnovTest.withDefaults().statistic(values, values), "statistic");
            Assertions.assertEquals(0.0, KolmogorovSmirnovTest.withDefaults().statistic(ascendingValues, ascendingValues), "statistic");

            Assertions.assertEquals(1.0, KolmogorovSmirnovTest.twoSampleExactP(0, i, i, i, false, twoSided), "exact p");
            Assertions.assertEquals(1.0, KolmogorovSmirnovTest.twoSampleExactP(0, i, i, i, false, twoSided), "exact p");
            Assertions.assertEquals(1.0, KolmogorovSmirnovTest.twoSampleExactP(0, i, i, i, true, twoSided), "exact p (strict)");
            Assertions.assertEquals(1.0, KolmogorovSmirnovTest.twoSampleExactP(0, i, i, i, true, twoSided), "exact p (strict)");

            Assertions.assertEquals(1.0, KolmogorovSmirnovTest.twoSampleApproximateP(0, i, i, true));
            Assertions.assertEquals(1.0, KolmogorovSmirnovTest.twoSampleApproximateP(0, i, i, true));
        }
    }

    @Test
    void testTwoSampleEstimatePThrows() {
        final KolmogorovSmirnovTest test = KolmogorovSmirnovTest.withDefaults().with(PValueMethod.ESTIMATE);
        // Must have a RNG
        TestUtils.assertThrowsWithMessage(IllegalStateException.class,
            () -> test.test(new double[] {1, 3, 5}, new double[] {1, 2, 3}), "random");
    }

    /**
     * Edge case where all possible D values are equal to or greater than the sample D.
     * Applies to the two-sided case.
     */
    @Test
    void testTwoSampleEstimatePWithEqualArrays() {
        final double[] x = {1, 2, 3, 7, 42};
        final UniformRandomProvider rng = RandomSource.SPLIT_MIX_64.create(999);
        final KolmogorovSmirnovTest test = KolmogorovSmirnovTest.withDefaults().with(PValueMethod.ESTIMATE).with(rng);
        // Default to non-strict inequality where all samples are D >= 0
        for (final int iterations : new int[] {1, 10, 10000}) {
            for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
                Assertions.assertEquals(1, test.with(h).withIterations(iterations).test(x, x).getPValue());
            }
        }
        // Using the strict inequality the sampling has to create some samples and none
        // can possibly be above D=0 when the sample is all the same value.
        final double[] y = {1, 1, 1};
        Assertions.assertEquals(0,
            test.withIterations(50).with(Inequality.STRICT).test(y, y).getPValue());
    }

    @ParameterizedTest
    @MethodSource
    void testTwoSampleEstimateP(double[] x, double[] y, UniformRandomProvider rng, int iterations, double[] p, double eps) {
        KolmogorovSmirnovTest test = KolmogorovSmirnovTest.withDefaults()
            .with(PValueMethod.ESTIMATE).with(rng).withIterations(iterations);
        int i = 0;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            final TwoResult r = test.test(x, y);
            TestUtils.assertProbability(p[i++], r.getPValue(), eps, () -> h + " estimateP");
            Assertions.assertEquals(Double.NaN, r.getUpperPValue());
        }
    }

    static Stream<Arguments> testTwoSampleEstimateP() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        final int iterations = 10000;
        // Reference data is R 3.4.0, ks.boot implemented in Matching (Version 4.10.8).
        // The p-value is very variable from Matching so relative epsilon is for 1 significant digit.
        // Even with such low tolerance the test is sensitive to the RNG and a fixed seed
        // RNG is used for each case. This method is a general estimate of the p-value.
        final double eps = 0.1;
        // Ties in the data
        // D = 0.63636363636363646457, p-value = 0.016040942624787169
        // D^+ = 0.63636363636363646457, p-value = 0.0080204754504885518
        // D^- = -2.7755575615628913511e-17, p-value = 1
        builder.add(Arguments.of(
            new double[] {0, 2, 4, 6, 8, 8, 10, 15, 22, 30, 33, 36, 38},
            new double[] {9, 17, 20, 33, 40, 51, 60, 60, 72, 90, 101},
            RandomSource.XO_RO_SHI_RO_128_PP.create(1234), iterations, new double[] {0.0063, 0.0027999, 1}, eps));
        // Example where D-values are close (subject to rounding).
        // D = 0.66666666666666674068, p-value = 0.085914085914085891
        // D^+ = -1.1102230246251565404e-16, p-value = 1                 (Error in D+)
        // D^- = 0.66666666666666674068, p-value = 0.057432619267617321
        builder.add(Arguments.of(
            new double[] {2, 4, 6, 8, 9, 10, 11, 12, 13},
            new double[] {0, 1, 3, 5, 7},
            RandomSource.XO_RO_SHI_RO_128_PP.create(12345), iterations, new double[] {0.0626, 1, 0.0307}, eps));
        // Larger data:
        // D = 0.20235294117647056877, p-value = 0.031998396239163229
        // D^+ = 0.20235294117647056877, p-value = 0.015999263643518027
        // D^- = 0.05999999999999999778, p-value = 0.69519398556947698
        builder.add(Arguments.of(
            GAUSSIAN, GAUSSIAN2,
            RandomSource.XO_RO_SHI_RO_128_PP.create(12345), iterations, new double[] {0.0234, 0.013899, 0.6451}, eps));
        return builder.build();
    }

    /**
     * Test the sampler can be created if the two samples are larger than the maximum array size.
     */
    @ParameterizedTest
    @CsvSource({
        "5, 6, 10000",
        "5, 6, 10",
    })
    void testCreateSampler(int n, int m, int maxArraySize) {
        final double[] x = IntStream.range(0, n).asDoubleStream().toArray();
        final double[] y = IntStream.range(n, n + m).asDoubleStream().toArray();
        final long[] obs = new long[n + m];
        final int samples = 10000;
        final UniformRandomProvider rng = RandomSource.XO_RO_SHI_RO_128_PP.create(123);
        final DoubleSupplier s = KolmogorovSmirnovTest.createSampler(x, y, rng, maxArraySize);
        for (int i = 0; i < samples; i++) {
            obs[(int) s.getAsDouble()]++;
        }
        Assertions.assertFalse(ChiSquareTest.withDefaults().test(obs).reject(0.001));
    }

    @ParameterizedTest
    @MethodSource
    void testTwoSampleOneSidedExactP(int n, int m, long dnm, boolean strict, double p, double eps) {
        final int gcd = ArithmeticUtils.gcd(n, m);
        final double p1 = KolmogorovSmirnovTest.twoSampleExactP(dnm, n, m, gcd, strict, false);
        if (p < 0) {
            Assertions.assertEquals(p, p1);
        } else {
            TestUtils.assertProbability(p, p1, eps, "p-value");
        }
    }

    static Stream<Arguments> testTwoSampleOneSidedExactP() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Hodges (1958) example from Figure 2
        // Distance d = 2/6 : dnm = 2/6 * 4*6
        builder.add(Arguments.of(4, 6, 8, false, 111 / 210.0, 0));
        // Using a strict inequality we require:
        // B(3, 0) -> B(4, 1) -> B(6, 2)
        // y    x    n(x+y, y)            B(x, y)           n(m+n-x-y, n-y)
        // 0    3    1          2  10     1                 35
        // 1    4    5              3     3 (5-2*1)         10
        // 2    6    28                   9 (28-10*1-3*3)   1
        builder.add(Arguments.of(4, 6, 8, true, (35 + 30 + 9) / 210.0, 0));
        // Values from Table 1 are for D, not D+
        // When D is large Pr(D) = 2*Pr(D+)
        builder.add(Arguments.of(6, 7, 36, false, 0.5 * 0.00816, 1e-3));
        builder.add(Arguments.of(7, 10, 56, false, 0.5 * 0.00452, 2e-3));

        // Overflow of binom(n+m, m) && n != m
        builder.add(Arguments.of(516, 515, 123, false, -1, 0));
        builder.add(Arguments.of(516, 515, 123, true, -1, 0));
        // Any min(n, m) >= 37 when max(n, m) == 2^31 -1.
        builder.add(Arguments.of(Integer.MAX_VALUE, 37, 3L * Integer.MAX_VALUE, false, -1, 0));
        builder.add(Arguments.of(Integer.MAX_VALUE, 37, 3L * Integer.MAX_VALUE, true, -1, 0));
        // Square underflow
        builder.add(Arguments.of(538, 538, 538 * 538, false, 4.9E-323, 0));
        builder.add(Arguments.of(539, 539, 539 * 539, false, 1.5E-323, 0));
        builder.add(Arguments.of(540, 540, 540 * 540, false, 4.9E-324, 0));
        builder.add(Arguments.of(541, 541, 541 * 541, false, 0, 0));
        builder.add(Arguments.of(1000, 1000, 1000 * 1000, false, 0, 0));
        return builder.build();
    }

    @Test
    @MethodSource
    void testTwoSampleExactPDistribution() {
        final boolean strict = true;
        final boolean nonStrict = false;
        double ps;
        double pn;
        double lps;
        double lpn;
        for (final int n : new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10}) {
            for (int i = 2; i <= n; i++) {
                final int m = i;
                // Not every d value is possible so assessing d to d+1 is not
                // relevant if either d is not actually possible.
                // Values change by at least the greatest common divisor.
                final int gcd = ArithmeticUtils.gcd(n, m);
                for (final boolean twoSided : new boolean[] {false, true}) {
                    // When p changes the computation has crossed a boundary.
                    // The transition for strict should have been the d value before:
                    // p(non-strict) D >=  d
                    // p(strict)     D >   d == p(non-strict)  D >= (d+gcd)
                    lps = KolmogorovSmirnovTest.twoSampleExactP(0, n, m, gcd, strict, twoSided);
                    lpn = KolmogorovSmirnovTest.twoSampleExactP(0, n, m, gcd, nonStrict, twoSided);
                    // For a true integral D statistic the two-sided value D=0 is not possible:
                    // in the absence of ties there is always a +/- difference.
                    if (twoSided) {
                        Assertions.assertEquals(1, lps, () -> String.format("%d,%d strict p(D=0)", n, m));
                    }
                    Assertions.assertEquals(1, lpn, () -> String.format("%d,%d non-strict p(D=0)", n, m));
                    // Sum the PMF using successive p differences.
                    double sn = 1 - lpn;
                    double ss = 1 - lps;
                    final int max = n * m / gcd;
                    for (long j = 1; j <= max; j++) {
                        final long d = j * gcd;
                        ps = KolmogorovSmirnovTest.twoSampleExactP(d, n, m, gcd, strict, twoSided);
                        pn = KolmogorovSmirnovTest.twoSampleExactP(d, n, m, gcd, nonStrict, twoSided);
                        Assertions.assertEquals(lps, pn,
                            () -> String.format("%d,%d %d : p(D > d) != p(D >= d+gcd)", n, m, d));
                        sn += lpn - pn;
                        ss += lps - ps;
                        lps = ps;
                        lpn = pn;
                    }
                    sn += lpn;
                    // No need for ss += lps. It should be zero.
                    Assertions.assertEquals(0, lps);
                    // The distribution should just be shifted. The sum is the same.
                    Assertions.assertEquals(sn, ss, "sum(non-strict) != sum(strict)");
                    // The sum should be 1.
                    TestUtils.assertRelativelyEquals(1.0, sn, 1e-6,
                            () -> String.format("%d,%d : sum(PMF)", n, m));
                }
            }
        }
    }

    /**
     * Output the exact p-values for the two-sample statistic.
     *
     * <p>This is not a test. It is used to generate test data for resource files
     * as larger number of combinations is slow.
     *
     * <p>The code will walk all possible paths through a 2D n-by-m grid where each
     * grid node represents a choice between the two samples. The D statistic is
     * computed for the path and used to generate the exact distribution.
     *
     * <p>Note: The number of possible D values is limited to {@code n*m/gcd(n, m)}. This
     * excludes zero, but can be included for D+ as a zero value is possible if the
     * D stays equal or below zero for the entire path (|D-| == D).
     */
    @Disabled("Generates resource files")
    @ParameterizedTest
    @CsvSource({
        // Number of combinations (binom(n+m, n)) : n*m/gcd
        // small
        "2, 2", // 6 : 4/2 = 2
        "3, 2", // 10 : 6/1 = 6
        "3, 3", // 20 : 9/3 = 3
        "4, 3", // 35 : 12/1 = 12
        "4, 4", // 70 : 16/4 = 4
        "5, 4", // 126 : 20/1 = 20
        "6, 4", // 210 : 24/4 = 6
        "7, 4", // 330 : 28/1 = 28
        "7, 7", // 3432 : 39/7 = 7
        "10, 4", // 1001 : 40/2 = 20
        "10, 9", // 92378 : 90/1 = 90
        "10, 10", // 184756 : 100/10 = 10
        "39, 4", // 123410 : 156/1 = 156
        // medium (some take minutes to generate)
        "15, 14", // 77558760 : 210/1 = 210
        "16, 16", // 601080390 : 256/16 = 16
        "15, 12", // 17383860 : 180/3 = 60
        "18, 12", // 86493225 : 216/6 = 36
        // Close to limit of 2^31
        "19, 15", // 1855967520 : 285/1 = 285
        "102, 6", // 1913554188 : 612/6 = 102
    })
    void testTwoSampleExactPUsingCombinations(int m, int n) {
        // The total combinations is used at the end.
        // Obtain it now and check it is an integer.
        final double total = Math.toIntExact(BinomialCoefficient.value(n + m, m));
        // Counts of D in [0, (n*m/gcd)]
        final int gcd = ArithmeticUtils.gcd(m, n);
        final int[] d = new int[(m * n) / gcd + 1];
        final int[] dp = new int[d.length];
        // Trivial tests show iteration of the smaller combination is faster.
        // If the combination iterates over b * gcd the curD must be incremented
        // or decremented by the opposite factor.
        final long start = System.nanoTime();
        final int a = Math.min(n, m) / gcd;
        final int b = Math.max(n, m) / gcd;
        final Combinations comb = Combinations.of(m + n, a * gcd);
        comb.iterator().forEachRemaining(c -> {
            // Count steps along the 2D grid
            // Combination number correspond to the nth step taking x, else y:
            // [0, 1, 2] : x, x, x
            // [1, 2, 4] : y, x, x, y, x
            int curD = 0;
            int plus = 0;
            int minus = 0;
            // This holds the lowest expected step for x
            int step = 0;
            // Note: The loop can be broken when either x or y is at the maximum
            // as remaining steps in the other direction do not change the extreme D.
            // If the last step is an x-step we can break on y.
            // This optimisation is omitted as it does not change
            // the result as the remaining single x-steps have no effect.
            // Intuitively if x steps < y steps then more combinations
            // end early on an x step than a y step.
            for (final int ci : c) {
                // y step(s)
                if (step < ci) {
                    // (ci - step) = number of y-steps
                    curD -= (ci - step) * a;
                    minus = curD < minus ? curD : minus;
                }
                // x step
                step = ci + 1;
                curD += b;
                plus = curD > plus ? curD : plus;
            }
            // All x-steps have been taken; remaining y-steps cannot make the D more extreme
            curD = -minus > plus ? -minus : plus;
            d[curD]++;
            dp[plus]++;
        });
        TestUtils.printf("# %.6fs%n", (System.nanoTime() - start) * 1e-9);
        // Print the distribution:
        // n, m, d, p(D >= d), p(D > d), p(D+ >= d), p(D+ > d)
        // p = remaining / total
        int rD = (int) total;
        int rDp = rD;
        for (int i = 0; i < d.length; i++) {
            if (d[i] == 0 && dp[i] == 0) {
                continue;
            }
            // non-strict
            final double p2 = rD / total;
            final double p1 = rDp / total;
            rD -= d[i];
            rDp -= dp[i];
            // strict
            final double p2s = rD / total;
            final double p1s = rDp / total;
            TestUtils.printf("%d %d %d %s %s %s %s%n", n, m, i * gcd, p2, p2s, p1, p1s);
        }
        TestUtils.printf("%n");
    }

    /**
     * Test the exact enumerated distribution computed by {@link #testTwoSampleExactPUsingCombinations(int, int)}.
     */
    @ParameterizedTest
    @CsvFileSource(resources = {"ks.twosample.small.txt"}, delimiter = ' ')
    void testTwoSampleExactPSmall(int n, int m, long dmn, double p2, double p2s, double p1, double p1s) {
        assertTwoSampleExactP(n, m, dmn, p2, p2s, p1, p1s, 3 * EPS);
    }

    /**
     * Test the exact enumerated distribution computed by {@link #testTwoSampleExactPUsingCombinations(int, int)}.
     */
    @ParameterizedTest
    @CsvFileSource(resources = {"ks.twosample.medium.txt"}, delimiter = ' ')
    void testTwoSampleExactPMedium(int n, int m, long dmn, double p2, double p2s, double p1, double p1s) {
        assertTwoSampleExactP(n, m, dmn, p2, p2s, p1, p1s, 5 * EPS);
    }

    private static void assertTwoSampleExactP(int n, int m, long dmn,
            double p2, double p2s, double p1, double p1s, double eps) {
        final int gcd = ArithmeticUtils.gcd(n, m);
        final double pa = KolmogorovSmirnovTest.twoSampleExactP(dmn, n, m, gcd, false, true);
        TestUtils.assertProbability(p2, pa, eps, "two-sided");
        final double pb = KolmogorovSmirnovTest.twoSampleExactP(dmn, n, m, gcd, true, true);
        TestUtils.assertProbability(p2s, pb, eps, "two-sided strict");
        final double pc = KolmogorovSmirnovTest.twoSampleExactP(dmn, n, m, gcd, false, false);
        TestUtils.assertProbability(p1, pc, eps, "one-sided");
        final double pd = KolmogorovSmirnovTest.twoSampleExactP(dmn, n, m, gcd, true, false);
        TestUtils.assertProbability(p1s, pd, eps, "one-sided strict");
    }

    @ParameterizedTest
    @CsvFileSource(resources = {"ks.twosample.large.txt"}, delimiter = ' ')
    void testTwoSampleExactPLarge(int n, int m, long dmn, double p2, double p1) {
        final int gcd = ArithmeticUtils.gcd(n, m);
        final double eps = m + n > 100 ? 8e-13 : 5e-14;
        final double pa = KolmogorovSmirnovTest.twoSampleExactP(dmn, n, m, gcd, false, true);
        TestUtils.assertProbability(p2, pa, eps, "two-sided");
        final double pc = KolmogorovSmirnovTest.twoSampleExactP(dmn, n, m, gcd, false, false);
        TestUtils.assertProbability(p1, pc, eps, "one-sided");
    }

    @Test
    void testMath1475() {
        // MATH-1475
        final double[] x = new double[] {
            0.12350159883499146, -0.2601194679737091, -1.322849988937378, 0.379696249961853,
            0.3987586498260498, -0.06924121081829071, -0.13951236009597778, 0.3213207423686981,
            0.7949811816215515, -0.15811105072498322, 0.19912190735340118, -0.46363770961761475,
            -0.20019817352294922, 0.3062838613986969, -0.3872813880443573, 0.10733723640441895,
            0.10910066962242126, 0.625770092010498, 0.2824835777282715, 0.3107619881629944,
            0.1432388722896576, -0.08056988567113876, -0.5816712379455566, -0.09488576650619507,
            -0.2154506891965866, 0.2509046196937561, -0.06600788980722427, -0.01133995596319437,
            -0.22642627358436584, -0.12150175869464874, -0.21109570562839508, -0.17732949554920197,
            -0.2769380807876587, -0.3607368767261505, -0.07842907309532166, -0.2518743574619293,
            0.035517483949661255, -0.6556509137153625, -0.360045850276947, -0.09371964633464813,
            -0.7284095883369446, -0.22719840705394745, -1.5540679693222046, -0.008972732350230217,
            -0.09106933325529099, -0.6465389132499695, 0.036245591938495636, 0.657580554485321,
            0.32453101873397827, 0.6105462908744812, 0.25256943702697754, -0.194427490234375,
            0.6238796710968018, 0.5203511118888855, -0.2708645761013031, 0.07761227339506149,
            0.5315862894058228, 0.44320303201675415, 0.6283767819404602, 0.2618369162082672,
            0.47253096103668213, 0.3889777660369873, 0.6856100559234619, 0.3007083833217621,
            0.4963226914405823, 0.08229698985815048, 0.6170856952667236, 0.7501978874206543,
            0.5744063258171082, 0.5233180522918701, 0.32654184103012085, 0.3014495372772217,
            0.4082445800304413, -0.1075737327337265, -0.018864337354898453, 0.34642550349235535,
            0.6414541602134705, 0.16678297519683838, 0.46028634905815125, 0.4151197075843811,
            0.14407725632190704, 0.41751566529273987, -0.054958608001470566, 0.4995657801628113,
            0.4485369324684143, 0.5600396990776062, 0.4098612368106842, 0.2748555839061737,
            0.2562614381313324, 0.4324824810028076 };
        final double[] y = new double[] {
            2.6881366763426717, 2.685469965655465, 2.261888917462379, -2.1933598759641226,
            -2.4279488152810145, -3.159389495849609, -2.3150004548153444, 2.468029206047388,
            2.9442494682288953, 2.653360013462529, -2.1189940659194835, -2.121635289903703,
            -2.103092459792032, -2.737034221468073, -2.203389332350286, 2.1985949039005512,
            -2.5021604073154737, 2.2732754920764533, -2.3867025598454346, 2.135919387338413,
            2.338120776050672, 2.2579794509726874, 2.083329059799027, -2.209733724709957,
            2.297192240399189, -2.201703830825843, -3.460208691996806, 2.428839296615834,
            -3.2944259224581574, 2.0654875493620883, -2.743948930837782, -2.2240674680805212,
            -3.646366778182357, -2.12513198437294, 2.979166188824589, -2.6275491570089033,
            -2.3818176136461338, 2.882096356968376, -2.2147229261558334, -3.159389495849609,
            2.312428759406432, 2.3313864098846477, -2.72802504046371, -2.4216068225364245,
            3.0119599306499123, 2.5753099009496783, -2.9200121783556843, -2.519352725437922,
            -4.133932580227538, -2.30496316762808, 2.5381353678521363, 2.4818233632136697,
            2.5277451177925685, -2.166465445816232, -2.1193897819471563, -2.109654332722425,
            3.260211545834851, -3.9527673876059013, -2.199885089466947, 2.152573429747697,
            -3.1593894958496094, 2.5479522823226795, 3.342810742466116, -2.8197184957304007,
            -2.3407900299253765, -2.3303967152728537, 2.1760131201015565, 2.143930552944634,
            2.33336231754409, 2.9126278362420575, -2.121169134387265, -2.2980208408109095,
            -2.285400411434817, -2.0742764640932903, 2.304178664095016, -2.2893825538911634,
            -3.7714771984158806, -2.7153698816026886, 2.8995011276220226, -2.158787087333056,
            -2.1045987952052547, 2.8478762016468147, -2.694578565956955, -2.696014432856399,
            -2.3190122657403496, -2.48225194403028, 3.3393947563371764, 2.7775468034263517,
            -3.396526561479875, -2.699967947404961};
        final double p = KolmogorovSmirnovTest.withDefaults().test(x, y).getPValue();
        Assertions.assertTrue(0 <= p && p <= 1, () -> "Invalid p-value: " + p);
    }

    @Test
    void testMath1535() {
        // MATH-1535
        // Internal error in case of ties with many similar surrounding values
        // that cannot be resolved with random jitter.
        // This is no longer an issue as random jitter is not used to resolve ties.
        final double[] x = new double[] {
            0.8767630865438496, 0.9998809418147052, 0.9999999715463531, 0.9999985849345421,
            0.973584315883326, 0.9999999875782982, 0.999999999999994, 0.9999999999908233,
            1.0, 0.9999999890925574, 0.9999998345734327, 0.9999999350772448,
            0.999999999999426, 0.9999147040688201, 0.9999999999999922, 1.0,
            1.0, 0.9919050954798272, 0.8649014770687263, 0.9990869497973084,
            0.9993222540990464, 0.999999999998189, 0.9999999999999365, 0.9790934801762917,
            0.9999578695006303, 0.9999999999999998, 0.999999999996166, 0.9999999999995546,
            0.9999999999908036, 0.99999999999744, 0.9999998802655555, 0.9079334221214075,
            0.9794398308007372, 0.9999044231134367, 0.9999999999999813, 0.9999957841707683,
            0.9277678892094009, 0.999948269893843, 0.9999999886132888, 0.9999998909699096,
            0.9999099536620326, 0.9999999962217623, 0.9138936987350447, 0.9999999999779976,
            0.999999999998822, 0.999979247207911, 0.9926904388316407, 1.0,
            0.9999999999998814, 1.0, 0.9892505696426215, 0.9999996514123723,
            0.9999999999999429, 0.9999999995399116, 0.999999999948221, 0.7358264887843119,
            0.9999999994098534, 1.0, 0.9999986456748472, 1.0,
            0.9999999999921501, 0.9999999999999996, 0.9999999999999944, 0.9473070068606853,
            0.9993714060209042, 0.9999999409098718, 0.9999999592791519, 0.9999999999999805};
        final double[] y = new double[x.length];
        Arrays.fill(y, 1);
        final double p = KolmogorovSmirnovTest.withDefaults().test(x, y).getPValue();
        Assertions.assertTrue(0 <= p && p <= 1, () -> "Invalid p-value: " + p);
    }

    @Test
    void testMath1246() {
        final double[] x = {4, 5, 6, 7};
        final double[] y = {1, 2, 3, 4};
        KolmogorovSmirnovTest test = KolmogorovSmirnovTest.withDefaults();
        // Default is non-strict inequality
        final boolean strict = false;
        boolean significantTies = false;
        for (final AlternativeHypothesis h : AlternativeHypothesis.values()) {
            test = test.with(h);
            final boolean twoSided = h == AlternativeHypothesis.TWO_SIDED;
            final double d1 = test.statistic(x, y);
            if (twoSided) {
                final double d2 = test.statistic(y, x);
                Assertions.assertEquals(d1, d2, "statistic should be stable to argument order");
            }
            final int gcd = 4;
            final double p1 = KolmogorovSmirnovTest.twoSampleExactP(
                (long) (d1 * x.length * y.length), x.length, y.length, gcd, strict, twoSided);

            final TwoResult r1 = test.test(x, y);
            Assertions.assertEquals(p1, r1.getPValue(), "unexpected p-value");
            if (twoSided) {
                final TwoResult r2 = test.test(y, x);
                Assertions.assertEquals(r1.getPValue(), r2.getPValue(), "p-value should be stable to argument order");
            }

            // Possible variations for the tie
            final double v = y[3];
            y[3] = 4.1;
            final double s4 = test.statistic(x, y);
            final double p4 = test.test(x, y).getPValue();
            y[3] = 3.9;
            final double s5 = test.statistic(x, y);
            final double p5 = test.test(x, y).getPValue();
            // reset
            y[3] = v;

            significantTies |= s4 != s5;
            Assertions.assertEquals(s4 != s5, r1.hasSignificantTies());

            Assertions.assertEquals(Math.max(s4, s5), r1.getUpperD(), "upper D");
            Assertions.assertEquals(Math.max(p4, p5), r1.getPValue(), "statistic p-value");
            Assertions.assertEquals(Math.min(p4, p5), r1.getUpperPValue(), "upper D p-value");
        }
        Assertions.assertTrue(significantTies, "Test never created a significant tie");
    }
}
