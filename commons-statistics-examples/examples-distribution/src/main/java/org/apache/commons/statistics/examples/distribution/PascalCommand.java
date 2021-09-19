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
package org.apache.commons.statistics.examples.distribution;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.statistics.distribution.DiscreteDistribution;
import org.apache.commons.statistics.distribution.PascalDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link PascalDistribution}.
 */
@Command(name = "pascal",
         aliases = {"negativebinomial", "nbin"},
         description = "Pascal distribution.",
         subcommands = {
             PascalCommand.Check.class,
             PascalCommand.PMF.class,
             PascalCommand.LPMF.class,
             PascalCommand.CDF.class,
             PascalCommand.Survival.class,
             PascalCommand.ICDF.class,
         })
class PascalCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractDiscreteDistributionCommand {
        /** Distribution parameters. */
        @ArgGroup(validate = false, heading = "Distribution parameters:%n", order = 1)
        private Params params = new Params();

        /** Parameters class. */
        static class Params {
            /** The distribution trials. */
            @Option(names = {"-r", "--number-of-successes"},
                    arity = "1..*",
                    split = ",",
                    description = {"number of successes (default: ${DEFAULT-VALUE})."})
            private int[] r = {1, 2, 3, 4, 5, 10, 20, 40};

            /** The distribution p. */
            @Option(names = {"-p", "--probability"},
                    arity = "1..*",
                    split = ",",
                    description = {"probability of success (default: ${DEFAULT-VALUE})."})
            // mean = (r * (1 - p)) / p = (r / p) - r
            // p = r / (mean + r)
            // Compute for a mean=10
            private double[] p = {1.0 / 11, 2.0 / 12, 3.0 / 13, 4.0 / 14, 5.0 / 15, 10.0 / 20, 20.0 / 30, 40.0 / 50};
        }

        /** Extend the options to set the default values for this distribution. */
        static final class Options extends DiscreteDistributionOptions {
            /** Set defaults. */
            private Options() {
                min = 0;
                max = 25;
            }
        }

        @Override
        protected List<Distribution<DiscreteDistribution>> getDistributions() {
            int[] r = params.r;
            double[] p = params.p;
            final int n = DistributionUtils.validateLengths(r.length, p.length);

            r = DistributionUtils.expandToLength(r, n);
            p = DistributionUtils.expandToLength(p, n);

            // Create distributions
            final ArrayList<Distribution<DiscreteDistribution>> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final DiscreteDistribution d = new PascalDistribution(r[i], p[i]);
                list.add(new Distribution<>(d, "r=" + r[i] + ",p=" + p[i]));
            }
            return list;
        }
    }

    /** Base command for the distribution that defines the parameters. */
    private abstract static class ProbabilityCommand extends BaseCommand {
        /** The distribution options. */
        @ArgGroup(validate = false, heading = "Evaluation options:%n", order = 2)
        private Options distributionOptions = new Options();

        @Override
        protected DistributionOptions getDistributionOptions() {
            return distributionOptions;
        }
    }

    /** Verification checks command. */
    @Command(name = "check",
             hidden = true,
             description = "Pascal distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PMF command. */
    @Command(name = "pmf",
             aliases = {"pdf"},
             description = "Pascal distribution PMF.")
    static class PMF extends ProbabilityCommand {}

    /** LPMF command. */
    @Command(name = "lpmf",
             aliases = {"lpdf"},
             description = "Pascal distribution natural logarithm of the PMF.")
    static class LPMF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Pascal distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** Survival command. */
    @Command(name = "survival", aliases = {"sur"},
             description = "Pascal distribution survival probability.")
    static class Survival extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Pascal distribution inverse CDF.")
    static class ICDF extends BaseCommand {
        /** The distribution options. */
        @ArgGroup(validate = false, heading = "Evaluation options:%n", order = 2)
        private InverseDiscreteDistributionOptions distributionOptions = new InverseDiscreteDistributionOptions();

        @Override
        protected DistributionOptions getDistributionOptions() {
            return distributionOptions;
        }
    }
}
