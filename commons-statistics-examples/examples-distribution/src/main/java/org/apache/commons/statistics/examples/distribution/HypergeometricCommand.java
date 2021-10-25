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
import org.apache.commons.statistics.distribution.HypergeometricDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link HypergeometricDistribution}.
 */
@Command(name = "hypergeometric",
         aliases = {"hyge"},
         description = "Hypergeometric distribution.",
         subcommands = {
             HypergeometricCommand.Check.class,
             HypergeometricCommand.PMF.class,
             HypergeometricCommand.LPMF.class,
             HypergeometricCommand.CDF.class,
             HypergeometricCommand.Survival.class,
             HypergeometricCommand.ICDF.class,
         })
class HypergeometricCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractDiscreteDistributionCommand {
        /** Distribution parameters. */
        @ArgGroup(validate = false, heading = "Distribution parameters:%n", order = 1)
        private Params params = new Params();

        /** Parameters class. */
        static class Params {
            /** The distribution N. */
            @Option(names = {"-N", "--population-size"},
                    paramLabel = "N",
                    arity = "1..*",
                    split = ",",
                    description = {"population size (default: ${DEFAULT-VALUE})."})
            private int[] popSize = {500};

            /** The distribution successes. */
            @Option(names = {"-K", "--number-of-successes"},
                    paramLabel = "K",
                    arity = "1..*",
                    split = ",",
                    description = {"number of successes (default: ${DEFAULT-VALUE})."})
            private int[] successes = {50, 60, 70};

            /** The distribution n. */
            @Option(names = {"-n", "--sample-size"},
                    arity = "1..*",
                    split = ",",
                    description = {"sample size (default: ${DEFAULT-VALUE})."})
            private int[] n = {100, 200, 300};
        }

        /** Extend the options to set the default values for this distribution. */
        static final class Options extends DiscreteDistributionOptions {
            /** Set defaults. */
            private Options() {
                min = 0;
                max = 60;
            }
        }

        @Override
        protected List<Distribution<DiscreteDistribution>> getDistributions() {
            int[] popSize = params.popSize;
            int[] successes = params.successes;
            int[] n = params.n;
            final int max = DistributionUtils.validateLengths(popSize.length, successes.length);

            popSize = DistributionUtils.expandToLength(popSize, max);
            successes = DistributionUtils.expandToLength(successes, max);
            n = DistributionUtils.expandToLength(n, max);

            // Create distributions
            final ArrayList<Distribution<DiscreteDistribution>> list = new ArrayList<>();
            for (int i = 0; i < max; i++) {
                final DiscreteDistribution d = HypergeometricDistribution.of(popSize[i], successes[i], n[i]);
                list.add(new Distribution<>(d, "N=" + popSize[i] + ",K=" + successes[i] + ",n=" + n[i]));
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
             description = "Hypergeometric distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PMF command. */
    @Command(name = "pmf",
             aliases = {"pdf"},
             description = "Hypergeometric distribution PMF.")
    static class PMF extends ProbabilityCommand {}

    /** LPMF command. */
    @Command(name = "lpmf",
             aliases = {"lpdf"},
             description = "Hypergeometric distribution natural logarithm of the PMF.")
    static class LPMF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Hypergeometric distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** Survival command. */
    @Command(name = "survival", aliases = {"sur"},
             description = "Hypergeometric distribution survival probability.")
    static class Survival extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Hypergeometric distribution inverse CDF.")
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
