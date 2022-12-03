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
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.statistics.inference.OneWayAnova.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for {@link OneWayAnova}.
 */
class OneWayAnovaTest {

    @Test
    void testAnovaTestThrows() {
        assertAnovaTestThrows(OneWayAnova.withDefaults()::test);
    }

    private static void assertAnovaTestThrows(Consumer<Collection<double[]>> action) {
        final List<double[]> emptyContents = Arrays.asList(new double[] {1, 2, 3}, new double[] {});
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(emptyContents), "values", "size");

        final List<double[]> tooFew = Arrays.asList(new double[] {1, 2, 3});
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
                () -> action.accept(tooFew), "categories");

        TestUtils.assertThrowsWithMessage(NullPointerException.class,
            () -> action.accept(null));

        final List<double[]> allLength1 = Arrays.asList(new double[] {1}, new double[] {2}, new double[] {3});
        TestUtils.assertThrowsWithMessage(IllegalArgumentException.class,
            () -> action.accept(allLength1), "degrees", "freedom", "within", "group", "zero");
    }

    @ParameterizedTest
    @MethodSource
    void testAnova(Collection<double[]> data, int dfbg, long dfwg, double msbg, double mswg,
                   double f, double p, double vcbg, double vcwg, double statEps, double pEps) {
        final Result r = OneWayAnova.withDefaults().test(data);
        Assertions.assertEquals(dfbg, r.getDFBG(), "dfbg");
        Assertions.assertEquals(dfwg, r.getDFWG(), "dfwg");
        TestUtils.assertRelativelyEquals(msbg, r.getMSBG(), statEps, "msbg");
        TestUtils.assertRelativelyEquals(mswg, r.getMSWG(), statEps, "mswg");
        TestUtils.assertRelativelyEquals(f, r.getStatistic(), statEps, "statistic");
        if (Double.isNaN(p)) {
            Assertions.assertEquals(p, r.getPValue(), "p-value");
        } else {
            TestUtils.assertProbability(p, r.getPValue(), pEps, "p-value");
        }
        TestUtils.assertRelativelyEquals(vcbg, r.getVCBG(), statEps, "vcbg");
        TestUtils.assertRelativelyEquals(vcwg, r.getVCWG(), statEps, "vcwg");
    }

    static Stream<Arguments> testAnova() {
        final double[] classA = {93.0, 103.0, 95.0, 101.0, 91.0, 105.0, 96.0, 94.0, 101.0};
        final double[] classB = {99.0, 92.0, 102.0, 100.0, 102.0, 89.0};
        final double[] classC = {110.0, 115.0, 111.0, 117.0, 128.0, 117.0};
        final Stream.Builder<Arguments> builder = Stream.builder();
        // Target values computed using R version 3.4.0
        // A = c(93.0, 103.0, 95.0, 101.0, 91.0, 105.0, 96.0, 94.0, 101.0)
        // B = c(99.0, 92.0, 102.0, 100.0, 102.0, 89.0)
        // C = c(110.0, 115.0, 111.0, 117.0, 128.0, 117.0)
        // cA = rep("a", length(A))
        // cB = rep("b", length(B))
        // cC = rep("c", length(C))
        // d = summary(aov(value ~ cat, data = data.frame(cat=c(cA, cB, cC), value=c(A, B, C))))
        // d[[1]][["F value"]]
        // d[[1]][["Pr(>F)"]]
        //
        // See also:
        // - scipy.stats.f_oneway (does not output mean square values)
        // - http://www.biostathandbook.com/onewayanova.html
        // Provides a spreadsheet to compute the values. This was used to obtain the
        // partitioning of the variance.

        builder.add(Arguments.of(
            Arrays.asList(classA, classB, classC),
            2, 18,
            757.571428571428442, 30.703703703703681,
            24.67361709460625363, 6.9594458853833384454e-06,
            0.775402156121239000, 0.22459784387876100000,
            1e-14, 1e-13));
        // d <- summary(aov(value ~ cat, data = data.frame(cat=c(cA, cB), value=c(A, B))))
        builder.add(Arguments.of(
            Arrays.asList(classA, classB),
            1, 13,
            0.39999999999999569, 26.56410256410257986,
            0.01505791505791488627, 0.90421296046434118665,
            // Values clipped to [0, 1] : -0.158476734795303000, 1.15847673479530000000
            0, 1,
            5e-12, 3e-13));
        // d <- summary(aov(value ~ cat, data = data.frame(cat=c(cA, cC), value=c(A, C))))
        builder.add(Arguments.of(
            Arrays.asList(classA, classC),
            1, 13,
            1254.40000000000146, 30.87179487179494,
            40.632558139534836528, 2.439935448222361878e-05,
            0.846260800476711000, 0.15373919952328900000,
            3e-14, 2e-13));
        // d <- summary(aov(value ~ cat, data = data.frame(cat=c(cB, cC), value=c(B, C))))
        builder.add(Arguments.of(
            Arrays.asList(classB, classC),
            1, 10,
            1082.999999999998408, 35.866666666666688,
            30.195167286245290228, 0.00026362644003193318621,
            0.829522049115395000, 0.17047795088460500000,
            5e-15, 1e-14));
        // Example data from McDonald et al. (1991)
        final double[] tillamook = {0.0571, 0.0813, 0.0831, 0.0976, 0.0817, 0.0859, 0.0735, 0.0659, 0.0923, 0.0836};
        final double[] newport = {0.0873, 0.0662, 0.0672, 0.0819, 0.0749, 0.0649, 0.0835, 0.0725};
        final double[] petersburg = {0.0974, 0.1352, 0.0817, 0.1016, 0.0968, 0.1064, 0.105};
        final double[] magadan = {0.1033, 0.0915, 0.0781, 0.0685, 0.0677, 0.0697, 0.0764, 0.0689};
        final double[] tvarminne = {0.0703, 0.1026, 0.0956, 0.0973, 0.1039, 0.1045};
        builder.add(Arguments.of(
            Arrays.asList(tillamook, newport, petersburg, magadan, tvarminne),
            4, 34,
            0.00112991852678572, 0.000158673702731092,
            7.121019471642447, 0.0002812242314534544,
            0.441485178314125000, 0.55851482168587500000,
            6e-15, 1e-14));

        // Edge cases.
        // R allows data samples of size 1 for all samples but does not output an ANOVA table.
        // When all samples are size 1 except a sample of size 2 then R and scipy output a result.
        // In this case DF within groups = 1 (the lowest possible)
        // summary(aov(value ~ cat, data = data.frame(cat=c('a','b','c','c'),value=c(1, 2, 3, 4))))
        builder.add(Arguments.of(
            Arrays.asList(new double[] {1}, new double[] {2}, new double[] {3, 4}),
            2, 1,
            2.25, 0.5,
            4.5, 0.31622776601683783015,
            0.736842105263158000, 0.26315789473684200000,
            1e-15, 1e-15));
        // No variation within groups:
        // scipy.stats.f_oneway([1,1,1,1], [2,2,2], [3,3,3,3,3,3])
        builder.add(Arguments.of(
            Arrays.asList(new double[] {1, 1, 1, 1}, new double[] {2, 2, 2}, new double[] {3, 3, 3, 3, 3, 3}),
            2, 10,
            4.84615384615385, 0,
            Double.POSITIVE_INFINITY, 0,
            1, 0,
            1e-15, 1e-15));
        // No variation at all:
        // scipy.stats.f_oneway([1,1,1,1], [1,1,1], [1,1,1,1,1,1])
        builder.add(Arguments.of(
            Arrays.asList(new double[] {1, 1, 1, 1}, new double[] {1, 1, 1}, new double[] {1, 1, 1, 1, 1, 1}),
            2, 10,
            0, 0,
            Double.NaN, Double.NaN,
            0, 1,
            1e-15, 1e-15));

        return builder.build();
    }
}
