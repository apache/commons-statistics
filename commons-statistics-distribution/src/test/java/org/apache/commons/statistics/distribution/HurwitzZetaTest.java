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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.numbers.fraction.BigFraction;
import org.apache.commons.statistics.distribution.ExtendedPrecisionTest.RMS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test the {@link HurwitzZeta} function.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HurwitzZetaTest {
    /** Table used to create a histogram of the number of steps to converge the tail series.
     * Used in the {@link #zeta(double, double, int, int)} implementation. */
    private static final int[] M = new int[53];
    /** Optimal N used to test the zeta function. */
    private static final int N = 8;
    /** Minimum N used to test the zeta function.
     * Used for reporting RMS errors with varying N. When MIN_N == MAX_N no report is printed. */
    private static final int MIN_N = N; // e.g. 5
    /** Maximum N used to test the zeta function. Used for reporting RMS errors with varying N. */
    private static final int MAX_N = N; // e.g. 12
    /** Flag set when the JVM version is printed. Used for testing. */
    private static boolean jvm = false;

    /**
     * Numerators of the even Bernoulli numbers {@code B_{2k}}.
     * Taken from:
     * <pre>
     * A000367 Numerators of Bernoulli numbers B_2n.
     * https://oeis.org/A164020/b164020.txt
     * </pre>
     *
     * <p>Contains the sequence up to 2k = 106 required for M=53 in the zeta implementation.
     * Johansson (2015) suggests N ~ M ~ P for P-bits of precision.
     */
    private static final String[] NUM = {
        "0 1",
        "1 1",
        "2 -1",
        "3 1",
        "4 -1",
        "5 5",
        "6 -691",
        "7 7",
        "8 -3617",
        "9 43867",
        "10 -174611",
        "11 854513",
        "12 -236364091",
        "13 8553103",
        "14 -23749461029",
        "15 8615841276005",
        "16 -7709321041217",
        "17 2577687858367",
        "18 -26315271553053477373",
        "19 2929993913841559",
        "20 -261082718496449122051",
        "21 1520097643918070802691",
        "22 -27833269579301024235023",
        "23 596451111593912163277961",
        "24 -5609403368997817686249127547",
        "25 495057205241079648212477525",
        "26 -801165718135489957347924991853",
        "27 29149963634884862421418123812691",
        "28 -2479392929313226753685415739663229",
        "29 84483613348880041862046775994036021",
        "30 -1215233140483755572040304994079820246041491",
        "31 12300585434086858541953039857403386151",
        "32 -106783830147866529886385444979142647942017",
        "33 1472600022126335654051619428551932342241899101",
        "34 -78773130858718728141909149208474606244347001",
        "35 1505381347333367003803076567377857208511438160235",
        "36 -5827954961669944110438277244641067365282488301844260429",
        "37 34152417289221168014330073731472635186688307783087",
        "38 -24655088825935372707687196040585199904365267828865801",
        "39 414846365575400828295179035549542073492199375372400483487",
        "40 -4603784299479457646935574969019046849794257872751288919656867",
        "41 1677014149185145836823154509786269900207736027570253414881613",
        "42 -2024576195935290360231131160111731009989917391198090877281083932477",
        "43 660714619417678653573847847426261496277830686653388931761996983",
        "44 -1311426488674017507995511424019311843345750275572028644296919890574047",
        "45 1179057279021082799884123351249215083775254949669647116231545215727922535",
        "46 -1295585948207537527989427828538576749659341483719435143023316326829946247",
        "47 1220813806579744469607301679413201203958508415202696621436215105284649447",
        "48 -211600449597266513097597728109824233673043954389060234150638733420050668349987259",
        "49 67908260672905495624051117546403605607342195728504487509073961249992947058239",
        "50 -94598037819122125295227433069493721872702841533066936133385696204311395415197247711",
        "51 3204019410860907078243020782116241775491817197152717450679002501086861530836678158791",
        "52 -319533631363830011287103352796174274671189606078272738327103470162849568365549721224053",
        "53 36373903172617414408151820151593427169231298640581690038930816378281879873386202346572901",
    };

    /**
     * Denominators of the even Bernoulli numbers {@code B_{2k}}.
     * Taken from:
     * <pre>
     * A002445 Denominators of Bernoulli numbers B_{2n}.
     * https://oeis.org/A002445/b002445.txt
     * </pre>
     */
    private static final String[] DENOM = {
        "0 1",
        "1 6",
        "2 30",
        "3 42",
        "4 30",
        "5 66",
        "6 2730",
        "7 6",
        "8 510",
        "9 798",
        "10 330",
        "11 138",
        "12 2730",
        "13 6",
        "14 870",
        "15 14322",
        "16 510",
        "17 6",
        "18 1919190",
        "19 6",
        "20 13530",
        "21 1806",
        "22 690",
        "23 282",
        "24 46410",
        "25 66",
        "26 1590",
        "27 798",
        "28 870",
        "29 354",
        "30 56786730",
        "31 6",
        "32 510",
        "33 64722",
        "34 30",
        "35 4686",
        "36 140100870",
        "37 6",
        "38 30",
        "39 3318",
        "40 230010",
        "41 498",
        "42 3404310",
        "43 6",
        "44 61410",
        "45 272118",
        "46 1410",
        "47 6",
        "48 4501770",
        "49 6",
        "50 33330",
        "51 4326",
        "52 1590",
        "53 642",
    };

    /**
     * Precomputed factors for {@code k}-th element of the tail function {@code T}.
     * Uses {@code 2k!} divided by Bernoulli number {@code B_2k}.
     * The table size is suitable for N ~ M ~ P for P-bits of precision (53 entries)
     * as stated in Johansson (2015) section 3.1. In practice the result in double
     * precision requires lower N & M values.
     */
    private static final double[] F = {
        12.0, // 2! / (1 / 6)
        -720.0, // 4! / (-1 / 30)
        30240.0, // 6! / (1 / 42)
        -1209600.0, // 8! / (-1 / 30)
        4.790016E7, // 10! / (5 / 66)
        -1.8924375803183792E9, // 12! / (-691 / 2730)
        7.47242496E10, // 14! / (7 / 6)
        -2.950130727918164E12, // 16! / (-3617 / 510)
        1.1646782814350067E14, // 18! / (43867 / 798)
        -4.597978722407473E15, // 20! / (-174611 / 330)
        1.81521054019435456E17, // 22! / (854513 / 138)
        -7.1661652561756672E18, // 24! / (-236364091 / 2730)
        2.82908877253043E20, // 26! / (8553103 / 6)
        -1.1168794925000445E22, // 28! / (-23749461029 / 870)
        4.4092635141854666E23, // 30! / (8615841276005 / 14322)
        -1.7407074646225822E25, // 32! / (-7709321041217 / 510)
        6.872037622739274E26, // 34! / (2577687858367 / 6)
        -2.7129717107520044E28, // 36! / (-26315271553053477373 / 1919190)
        1.0710383014704457E30, // 38! / (2929993913841559 / 6)
        -4.228289733582729E31, // 40! / (-261082718496449122051 / 13530)
        1.669261878547101E33, // 42! / (1520097643918070802691 / 1806)
        -6.589981753232787E34, // 44! / (-27833269579301024235023 / 690)
        2.6016205165923062E36, // 46! / (596451111593912163277961 / 282)
        -1.0270786120209628E38, // 48! / (-5609403368997817686249127547 / 46410)
        4.054743835786749E39, // 50! / (495057205241079648212477525 / 66)
        -1.6007487042788347E41, // 52! / (-801165718135489957347924991853 / 1590)
        6.319502582715391E42, // 54! / (29149963634884862421418123812691 / 798)
        -2.4948396201225357E44, // 56! / (-2479392929313226753685415739663229 / 870)
        9.849232037909393E45, // 58! / (84483613348880041862046775994036021 / 354)
        -3.888320954748035E47, // 60! / (-1215233140483755572040304994079820246041491 / 56786730)
        1.535047584313167E49, // 62! / (12300585434086858541953039857403386151 / 6)
        -6.060124957607529E50, // 64! / (-106783830147866529886385444979142647942017 / 510)
        2.3924414381101893E52, // 66! / (1472600022126335654051619428551932342241899101 / 64722)
        -9.444980218768351E53, // 68! / (-78773130858718728141909149208474606244347001 / 30)
        3.728728733414322E55, // 70! / (1505381347333367003803076567377857208511438160235 / 4686)
        -1.4720431007109737E57, // 72! / (-5827954961669944110438277244641067365282488301844260429 / 140100870)
        5.811393226148101E58, // 74! / (34152417289221168014330073731472635186688307783087 / 6)
        -2.2942460864500874E60, // 76! / (-24655088825935372707687196040585199904365267828865801 / 30)
        9.057320508803927E61, // 78! / (414846365575400828295179035549542073492199375372400483487 / 3318)
        -3.575686814230726E63, // 80! / (-4603784299479457646935574969019046849794257872751288919656867 / 230010)
        1.4116245727459505E65, // 82! / (1677014149185145836823154509786269900207736027570253414881613 / 498)
        -5.5728704383437275E66, // 84! / (-2024576195935290360231131160111731009989917391198090877281083932477 / 3404310)
        2.2000810641991213E68, // 86! / (660714619417678653573847847426261496277830686653388931761996983 / 6)
        -8.685571901589203E69, // 88! / (-1311426488674017507995511424019311843345750275572028644296919890574047 / 61410)
        3.428926346636115E71, // 90! / (1179057279021082799884123351249215083775254949669647116231545215727922535 / 272118)
        -1.3536858624708422E73, // 92! / (-1295585948207537527989427828538576749659341483719435143023316326829946247 / 1410)
        5.344137578373867E74, // 94! / (1220813806579744469607301679413201203958508415202696621436215105284649447 / 6)
        -2.10978095054183E76, // 96! / (-211600449597266513097597728109824233673043954389060234150638733420050668349987259 / 4501770)
        8.329081341920855E77, // 98! / (67908260672905495624051117546403605607342195728504487509073961249992947058239 / 6)
        -3.288189514770133E79, // 100! / (-94598037819122125295227433069493721872702841533066936133385696204311395415197247711 / 33330)
        1.2981251882636475E81, // 102! / (3204019410860907078243020782116241775491817197152717450679002501086861530836678158791 / 4326)
        -5.124792828500739E82, // 104! / (-319533631363830011287103352796174274671189606078272738327103470162849568365549721224053 / 1590)
        2.023187114193683E84, // 106! / (36373903172617414408151820151593427169231298640581690038930816378281879873386202346572901 / 642)
    };


    /**
     * Precomputed factors for {@code k}-th element of the tail function {@code T}.
     * Uses Bernoulli number {@code B_2k} divided by {@code 2k!}.
     * This is the inverse of table {@link #M}.
     */
    private static final double[] FM = {
        0.08333333333333333,
        -0.001388888888888889,
        3.306878306878307E-5,
        -8.267195767195768E-7,
        2.08767569878681E-8,
        -5.284190138687493E-10,
        1.3382536530684679E-11,
        -3.3896802963225827E-13,
        8.586062056277845E-15,
        -2.174868698558062E-16,
        5.5090028283602295E-18,
        -1.3954464685812522E-19,
        3.534707039629467E-21,
        -8.953517427037546E-23,
        2.267952452337683E-24,
        -5.744790668872202E-26,
        1.455172475614865E-27,
        -3.6859949406653103E-29,
        9.336734257095045E-31,
        -2.36502241570063E-32,
        5.990671762482134E-34,
        -1.5174548844682903E-35,
        3.843758125454189E-37,
        -9.736353072646691E-39,
        2.466247044200681E-40,
        -6.247076741820743E-42,
        1.5824030244644914E-43,
        -4.008273685948936E-45,
        1.0153075855569557E-46,
        -2.5718041582418717E-48,
        6.514456035233815E-50,
        -1.6501309906896525E-51,
        4.179830628539476E-53,
        -1.058763466770291E-54,
        2.6818791912607708E-56,
        -6.793279351107421E-58,
        1.7207577616681404E-59,
        -4.358730329348894E-61,
        1.1040792903684666E-62,
        -2.7966655133781345E-64,
        7.084036501679471E-66,
        -1.794407408289224E-67,
        4.545287063611096E-69,
        -1.1513346631982051E-70,
        2.9163647710923614E-72,
        -7.387238263497337E-74,
        1.8712093117637953E-75,
        -4.739828557761799E-77,
        1.2006125993354507E-78,
        -3.0411872415142924E-80,
        7.703417274705106E-82,
        -1.951298390909883E-83,
        4.942696565159462E-85,
    };

    /** RMS error of the test zeta function using different N. */
    private static final RMS[] RMS_ZETA = IntStream.range(0, 54).mapToObj(x -> new RMS()).toArray(RMS[]::new);
    /** RMS error of the final zeta function. */
    private static final RMS RMS_ZETA_FINAL = new RMS();

    // Test zeta implementation
    //
    // This class contains a parameterized version of the final implementation.
    // See STATISTICS-100 for variations tested during development.
    //
    // Note: The method is sensitive to the initial loop over N to create S.
    // Under certain conditions the N cannot be too high if using an ascending
    // sum of k as the sum does not converge and later terms are added with
    // low precision.
    //
    // Better results are obtained using descending k. However this prevents
    // an early exit if the series is rapidly converging and the term (a+k)^-s
    // drops below machine epsilon of the sum.
    //
    // Summing in extended precision requires a double-double (DD) sum to be used
    // throughout. Use in S and then not in the tail T does not lower the RMS.

    /**
     * Compute the value of the Hurwitz zeta function {@code zeta(s, a)}.
     * See {@link HurwitzZeta} for the formula details.
     *
     * <p><strong>Warning</strong>: No parameter validation is performed.
     *
     * @param s Argument {@code s > 1}
     * @param a Argument {@code a >= 1}
     * @param n Argument {@code N}
     * @param m Argument {@code M}
     * @return zeta(s, a)
     */
    static double zeta(double s, double a, int n, int m) {
        // Asymptotic Behavior as a -> inf
        // https://dlmf.nist.gov/25.11#E43
        // When a is large the series cannot use a+k.
        // This reduces to N=0, the I term and the first term of T.
        if (a > 1e15) {
            return Math.pow(a, 1 - s) / (s - 1) + Math.pow(a, -s) * 0.5;
        }

        final double apn = a + n;
        double p = Math.pow(apn, -s);

        // Initialise sum with the first tail term
        double sum = 0.5 * p;
        // S : k in [0, n-1]
        for (int k = n; --k >= 0;) {
            // Descending k sums in order of magnitude for increased precision.
            // Prevents early exit for large s when the term (a+k)^-s is below
            // machine epsilon of the ascending series sum.
            sum += Math.pow(a + k, -s);
        }

        // I
        // Use of (a+n)^(1-s) = (a+n)^-1 * apn to recycle the power lowers precision.
        sum += Math.pow(apn, 1 - s) / (s - 1);

        // T
        // The following recycles the power term p: (a+n)^-(2k-1+s).
        // This incorporates the factor for T into the sum terms.
        // This sets the first power as (a+n)^-(1+s) not (a+n)^-1.
        // When s is large the loop exits before the rising factorial overflows.

        // Rising factorial term : (s)_{2k-1}
        double f = s;
        // 2k - 1
        double k2 = 1;
        // Sum of an alternating series as each F changes sign.
        // Sum until terms will not impact the result.
        // Note: if the factor is too small (e.g. 0x1p-63) then the series continues
        // further and terms may be less accurate (i.e. add noise to the T sum).
        double tsum = 0;
        final double stop = sum * 0x1p-53;
        int i;
        for (i = 0; i < m; i++) {
            // p = (a+n)^-(2k-1+s)
            p /= apn;
            final double t = f * p / F[i];
            tsum += t;
            if (Math.abs(t) <= stop) {
                break;
            }
            p /= apn;
            // f = s * (s+1) * (s+2) * ... * (s+2k-2)
            f *= s + k2;
            k2 += 1.0;
            f *= s + k2;
            k2 += 1.0;
        }
        // Used to histogram convergence when testing
        if (n == N && MIN_N != MAX_N) {
            M[i]++;
        }
        return sum + tsum;
    }

    /**
     * Test the factors required for the tail sum. These are computed from the numerator
     * and denominator of the Bernoulli numbers, and the factorial of 2k. The test asserts
     * that using 2k! / B_2k is more accurate than B_2k / 2k! when limited to double precision.
     */
    @Test
    void testFactors() {
        // Factorial of 2k. Initialise at k=0.
        BigInteger factorial = BigInteger.ONE;
        double sum1 = 0;
        double sum2 = 0;
        // If this is too small the BigDecimal created by BigFraction is truncated
        final int scale = 200;
        // Check factors
        for (int k = 1; k < NUM.length; k++) {
            factorial = factorial.multiply(BigInteger.valueOf(2 * k - 1)).multiply(BigInteger.valueOf(2 * k));
            final BigInteger num = new BigInteger(NUM[k].substring(NUM[k].indexOf(' ') + 1));
            final BigInteger denom = new BigInteger(DENOM[k].substring(DENOM[k].indexOf(' ') + 1));

            // 2k! / B_2k
            final BigFraction factor1 = BigFraction.of(factorial.multiply(denom), num);
            final double d1 = factor1.doubleValue();
            // Cross verify BigFraction vs BigDecimal
            BigDecimal v = factor1.bigDecimalValue(scale, RoundingMode.HALF_EVEN);
            Assertions.assertEquals(v.doubleValue(), d1);
            // Find ULP precision
            final double e1 = new BigDecimal(d1).subtract(v)
                .divide(new BigDecimal(Math.ulp(d1)), scale, RoundingMode.HALF_EVEN).doubleValue();
            sum1 += Math.abs(e1);

            Assertions.assertEquals(d1, F[k - 1]);

            // Format to print the table:
            // "%s, // %s! / (%s / %s)%n", d2, 2 * k, num, denom

            // B_2k / 2k!
            final BigFraction factor2 = BigFraction.of(num, factorial.multiply(denom));
            final double d2 = factor2.doubleValue();
            // Cross verify BigFraction vs BigDecimal
            v = factor2.bigDecimalValue(scale, RoundingMode.HALF_EVEN);
            Assertions.assertEquals(v.doubleValue(), d2);
            // Find ULP precision
            final double e2 = new BigDecimal(d2).subtract(v)
                .divide(new BigDecimal(Math.ulp(d2)), scale, RoundingMode.HALF_EVEN).doubleValue();
            sum2 += Math.abs(e2);

            Assertions.assertEquals(d2, FM[k - 1]);

            // Format to print the table:
            // "%s, // (%s / %s) / %s!%n", d2, num, denom, 2 * k

            // For any M, the cumulative error is lower using 2k! / B_2k
            // as the first 6/7 factors are exact and errors in the later factors
            // are comparable.
            Assertions.assertTrue(sum1 < sum2, "2k! / B_2k does not have lower combined error");
        }
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "hurwitzzeta.csv")
    void testZeta(double s, double a, BigDecimal expected) {
        // Test with varying N.
        // Changes to M can be made but since the implementation quickly converges in the
        // tail sum M only has to be a small double digit number.
        for (int i = MIN_N; i <= MAX_N; i++) {
            final int n = i;
            assertZeta(s, a, expected, (x, p) -> zeta(x, p, n, 15), n < 6 ? 25 : 4, RMS_ZETA[i]);
        }
    }

    @Test
    void testZetaPrecision() {
        // Output the RMS with varying N.
        for (int i = 0; i < RMS_ZETA.length; i++) {
            reportPrecision(String.format("zeta %-2d", i), RMS_ZETA[i]);
        }
        ExtendedPrecisionTest.assertPrecision(RMS_ZETA[N], 3.5, 0.60);
    }

    @ParameterizedTest
    @Order(1)
    @CsvFileSource(resources = "hurwitzzeta.csv")
    void testZetaFinal(double s, double a, BigDecimal expected) {
        assertZeta(s, a, expected, HurwitzZeta::value, 3, RMS_ZETA_FINAL);
    }

    @Test
    void testZetaFinalPrecision() {
        reportPrecision("zeta final", RMS_ZETA_FINAL);
        ExtendedPrecisionTest.assertPrecision(RMS_ZETA_FINAL, 3.5, 0.60);

        // Test the formal implementation matches the test implementation
        if (!Double.isNaN(RMS_ZETA[N].getRMS())) {
            Assertions.assertEquals(RMS_ZETA[N].getMax(), RMS_ZETA_FINAL.getMax(), "Test max != final max");
            Assertions.assertEquals(RMS_ZETA[N].getRMS(), RMS_ZETA_FINAL.getRMS(), "Test rms != final rms");
        }
    }

    private static double assertZeta(double s, double a, BigDecimal expected,
            DoubleBinaryOperator f, int ulp, RMS rms) {
        final double e = expected.doubleValue();
        final double x = f.applyAsDouble(s, a);
        TestUtils.assertEquals(e, x, DoubleTolerances.ulps(ulp));
        return ExtendedPrecisionTest.addError(x, expected, e, rms);
    }

    private static void reportPrecision(String name, RMS rms) {
        // Note: The RMS changes with the JDK due to the use of Math.pow.

        // Eclipse Adoptium 17.0.6+10
        // zeta final  max  3.463160912553975          rms  0.581703219550096
        // zeta 5      max  20.5732122922785           rms  2.1416222789789
        // zeta 6      max  3.739011012156354          rms  0.605560612860319
        // zeta 7      max  3.698235736226918          rms  0.5978604595147342
        // zeta 8      max  3.463160912553975          rms  0.581703219550096
        // zeta 9      max  3.739011012156354          rms  0.6009469175550126
        // zeta 10     max  3.463160912553975          rms  0.5855515390508779
        // zeta 11     max  3.78356780754505           rms  0.5916447716325028
        // zeta 12     max  3.78356780754505           rms  0.5910072153542633

        // Only report implementation precision when testing different N.
        if (MAX_N <= MIN_N) {
            return;
        }
        final double v = rms.getRMS();
        if (!Double.isNaN(v)) {
            // CHECKSTYLE: stop regex
            if (!jvm) {
                jvm = true;
                System.out.printf("// %s %s%n",
                    System.getProperty("java.vm.vendor"),
                    System.getProperty("java.vm.version")
                );
            }
            System.out.printf("// %-10s  max  %-25s  rms  %s%n", name, rms.getMax(), v);
            // CHECKSTYLE: resume regex
        }
    }

    @Test
    @Order(2)
    void testZetaM() {
        // Check the M used to converge the tail series in the chosen implementation.
        // This depends on the epsilon used to stop the sum.
        int m = 0;
        for (int i = 0; i < M.length; i++) {
            if (M[i] != 0) {
                m = i + 1;
                // This is used for testing.
                // CHECKSTYLE: stop regex
                System.out.printf("// zeta  N=%-2d  M=%-2d  %d%n", N, m, M[i]);
                // CHECKSTYLE: resume regex
            }
        }
        Assertions.assertTrue(m < 15);
    }

    /**
     * Spot tests for the zeta function to check various points in the domain and extreme values.
     */
    @ParameterizedTest
    @MethodSource(value = "testZetaSpot")
    void testZetaSpot(double s, double a, double z, int ulp) {
        final double v = HurwitzZeta.value(s, a);
        TestUtils.assertEquals(z, v, DoubleTolerances.ulps(ulp));
    }

    static Stream<Arguments> testZetaSpot() {
        return Stream.of(
            // Reference values from mpmath version 1.4.1.
            // from mpmath import mp, zeta
            // mp.dps = 30; mp.pretty = True
            // def f(s, a):
            //   print(f'Arguments.of({s}, {a}, {zeta(s, a)}, 0),')
            // f(1.001, 1) etc.
            Arguments.of(1.001, 1, 1000.57728847601162684806668989, 0),
            Arguments.of(1.001, 3, 999.077634929516400548962369402, 0),
            Arguments.of(1.001, 156.78, 994.961087152463264230812967825, 0),
            Arguments.of(1.001, 345600, 987.327939500118316838908716646, 1),
            Arguments.of(1.001, 100000000.0, 981.747943025003254769065166246, 1),
            Arguments.of(1.001, 1000000000.0, 979.48998540929872849188230117, 1),
            Arguments.of(1.001, 10000000000.0, 977.237220955969649930501114761, 0),
            Arguments.of(1.001, 8.374e+19, 955.1620677642774549247109353, 0),
            Arguments.of(1.001, 7.2834e+238, 576.949320020341985622075858479, 0),
            Arguments.of(1.1678, 1, 6.54877176355186372373480299218, 1),
            Arguments.of(1.1678, 2.5, 5.29477174574315816738362779184, 1),
            Arguments.of(1.1678, 5.765, 4.50848224904053531500311915417, 1),
            Arguments.of(1.1678, 87698, 0.882622082916184877125466190512, 1),
            Arguments.of(1.1678, 1098765, 0.577489707637839655591145476126, 1),
            Arguments.of(1.1678, 100000000.0, 0.27089940045667952429754209707, 0),
            Arguments.of(1.1678, 1000000000.0, 0.184080609461973448176589687532, 1),
            Arguments.of(1.1678, 10000000000.0, 0.125085809513774573322512635067, 1),
            Arguments.of(1.1678, 6.786e+70, 7.75645430994177324455600419939e-12, 0),
            Arguments.of(1.3567, 1, 3.40603490577277492861753138946, 1),
            Arguments.of(1.3567, 12, 1.17295098623816176759602458034, 1),
            Arguments.of(1.3567, 267, 0.382340735582659556038655214163, 0),
            Arguments.of(1.3567, 100000000.0, 0.00392732544562110257556960028487, 0),
            Arguments.of(1.3567, 1000000000.0, 0.0017274158124759523689829065186, 0),
            Arguments.of(1.3567, 6780000000.0, 0.000872764873809386121835245990354, 1),
            Arguments.of(1.3567, 10000000000.0, 0.000759795803739606906900648768122, 1),
            Arguments.of(1.3567, 2.394279e+25, 2.48282011395930739748191376465e-09, 0),
            Arguments.of(1.3567, 1.37e+201, 5.03765781298315620677652110278e-72, 0),
            Arguments.of(1.9183, 1.234, 1.31039727875809754102393633608, 0),
            Arguments.of(1.9183, 2.234, 0.642314727500847848717986207886, 1),
            Arguments.of(1.9183, 32, 0.0458231265360237920443268300232, 1),
            Arguments.of(1.9183, 189, 0.00886331331419119681212176796288, 0),
            Arguments.of(1.9183, 26378, 9.48410976427270589834458588242e-05, 0),
            Arguments.of(1.9183, 1484793, 2.34193449523082289840265727697e-06, 1),
            Arguments.of(1.9183, 100000000.0, 4.90473353191884545392736492566e-08, 1),
            Arguments.of(1.9183, 1000000000.0, 5.91991424826323516752379877396e-09, 1),
            Arguments.of(1.9183, 10000000000.0, 7.14521688264220831940065076935e-10, 1),
            Arguments.of(1.9183, 7.18923e+79, 5.06551945929778382002806813887e-74, 1),
            Arguments.of(1.9183, 2.3423e+159, 4.87385597076733868223619309795e-147, 0),
            Arguments.of(1.9183, 3.4535e+303, 1.98532564832533048061338098412e-279, 1),

            // Large s
            Arguments.of(1001, 1, 1.0, 0),
            Arguments.of(1001, 1.3, 8.76403979411431591130552997055e-115, 0),
            Arguments.of(1001, 2, 4.66631809251609439495044772362e-302, 0),
            Arguments.of(1001, 3, 0, 0),

            Arguments.of(26783400000000.0, 1.0000000000000002, 0.994070539579705196633883729806, 0),
            Arguments.of(26783400000000.0, 1.0000000000002, 0.00470868956401873280652247209542, 0),
            Arguments.of(26783400000000.0, 1.0000000002, 0, 0),

            Arguments.of(1e+18, 1, 1.0, 0),
            Arguments.of(1e+18, 1.0000000000000002, 3.69192903582901397705695784205e-97, 0),
            Arguments.of(1e+18, 1.0000000000000004, 1.36303400055980247979692825583e-193, 1),

            Arguments.of(1e+19, 1, 1.0, 0),
            Arguments.of(1e+19, 1.0000000000000002, 0, 0),

            // Requires M=12 when N=9.
            // This is largest M noted during development when the RMS error is close to optimal.
            Arguments.of(31.76, 8.23, 8.6811830191090714059294777379e-30, 1),
            Arguments.of(31.76, 11.23, 4.68180272588528321856530594913e-34, 0),
            Arguments.of(31.76, 12.23, 3.17258072050987981477830566397e-35, 0),
            Arguments.of(31.76, 13.23, 2.66312289027731475813712555339e-36, 1),
            Arguments.of(31.76, 14.23, 2.68377485329827353841985109724e-37, 2),
            Arguments.of(31.76, 15.23, 3.1669792006042583105605159352e-38, 1),
            Arguments.of(31.76, 17.23, 6.55526711653597399706064463689e-40, 0),
            Arguments.of(31.76, 19.23, 2.08909657211945256234757218604e-41, 0),

            Arguments.of(61.76, 30.23, 4.27875492887441652081612955782e-92, 2),

            // s -> large, a == 1
            // Asymptote of Riemann zeta function: 1 + 2^-s
            Arguments.of(25.67, 1, 1.00000001873152459253171691257, 0),
            Arguments.of(29.67, 1, 1.00000000117069191296622655835, 0),
            Arguments.of(39.67, 1, 1.00000000000114324712238181431, 0),
            Arguments.of(59.67, 1, 1.00000000000000000109028530523, 0),
            Arguments.of(69.67, 1, 1.00000000000000000000106473174, 0),
            Arguments.of(89.67, 1, 1.00000000000000000000000000102, 0),

            // -------

            // Reference values using Matlab R2026a Symbolic Math Toolbox
            //   vpa(hurwitzZeta(sym(s, 'f'), sym(a, 'f')))
            // Note: The use of 'f' uses the floating-point conversion as N * 2^e
            // where N is the mantissa and e is the exponent.

            Arguments.of(1.5, 4789, 0.0289021565574206125831622859402, 0),
            Arguments.of(2.345, 12.789, 0.0254390524135780630410689989495, 1),
            Arguments.of(1.345, 12.789, 1.2196695365743183226009917322, 1),
            Arguments.of(1.345, 1278.9, 0.245686258677206272154248922088, 1),
            Arguments.of(4.345, 28697.9, 0.000000000000000366539298049937530342298075842, 1),
            Arguments.of(1.345, 1278562927.9, 0.00209103729002248575358360674936, 0),
            // large s before underflow
            Arguments.of(23.45, 12.789, 0.0000000000000000000000000134520611728481431909082787144, 0),
            Arguments.of(23.45, 1278.9, 8.01879331040682555147782226582e-72, 1),
            Arguments.of(23.45, 1278562927.9, 1.59541010013538002585127908428e-206, 1),
            Arguments.of(234.5, 12.789, 2.7978232305220904641253847499e-260, 0),
            Arguments.of(234.5, 17.89, 1.83179298527375942363255194085e-294, 0),
            // small s -> 1
            Arguments.of(1.00001, 1.789, 99999.7231466483928005870213366, 1),
            Arguments.of(1.00001, 17.89, 99997.1440070978817356297446315, 1),
            Arguments.of(1.00001, 1278562927.89, 99979.0331951153772621341875866, 1),
            Arguments.of(1.0000000000000002, 1.789, 4503599627370495.72314713725233, 0),
            Arguments.of(1.0000000000000002, 1278562927.89, 4503599627370475.03099742881301, 0)
        );
    }
}
