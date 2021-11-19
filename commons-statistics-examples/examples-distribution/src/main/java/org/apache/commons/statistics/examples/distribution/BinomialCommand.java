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
import org.apache.commons.statistics.distribution.BinomialDistribution;
import org.apache.commons.statistics.distribution.DiscreteDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link BinomialDistribution}.
 */
@Command(name = "binomial",
         aliases = {"bino"},
         description = "Binomial distribution.",
         subcommands = {
             BinomialCommand.Check.class,
             BinomialCommand.PMF.class,
             BinomialCommand.LPMF.class,
             BinomialCommand.CDF.class,
             BinomialCommand.Survival.class,
             BinomialCommand.ICDF.class,
             BinomialCommand.ISF.class,
         })
class BinomialCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractDiscreteDistributionCommand {
        /** Distribution parameters. */
        @ArgGroup(validate = false, heading = "Distribution parameters:%n", order = 1)
        private Params params = new Params();

        /** Parameters class. */
        static class Params {
            /** The distribution trials. */
            @Option(names = {"-n", "--trials"},
                    arity = "1..*",
                    split = ",",
                    description = {"number of trials (default: ${DEFAULT-VALUE})."})
            private int[] trials = {20, 20, 40};

            /** The distribution p. */
            @Option(names = {"-p", "--probability"},
                    arity = "1..*",
                    split = ",",
                    description = {"probability of success (default: ${DEFAULT-VALUE})."})
            private double[] p = {0.5, 0.7, 0.5};
        }

        /** Extend the options to set the default values for this distribution. */
        static final class Options extends DiscreteDistributionOptions {
            /** Set defaults. */
            private Options() {
                min = 0;
                max = 40;
            }
        }

        @Override
        protected List<Distribution<DiscreteDistribution>> getDistributions() {
            int[] trials = params.trials;
            double[] p = params.p;
            final int n = DistributionUtils.validateLengths(trials.length, p.length);

            trials = DistributionUtils.expandToLength(trials, n);
            p = DistributionUtils.expandToLength(p, n);

            // Create distributions
            final ArrayList<Distribution<DiscreteDistribution>> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final DiscreteDistribution d = BinomialDistribution.of(trials[i], p[i]);
                list.add(new Distribution<>(d, "trials=" + trials[i] + ",p=" + p[i]));
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

    /** Base command for the distribution that defines the parameters for inverse probability functions. */
    private abstract static class InverseProbabilityCommand extends BaseCommand {
        /** The distribution options. */
        @ArgGroup(validate = false, heading = "Evaluation options:%n", order = 2)
        private InverseDiscreteDistributionOptions distributionOptions = new InverseDiscreteDistributionOptions();

        @Override
        protected DistributionOptions getDistributionOptions() {
            return distributionOptions;
        }
    }

    /** Verification checks command. */
    @Command(name = "check",
             hidden = true,
             description = "Binomial distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PMF command. */
    @Command(name = "pmf",
             aliases = {"pdf"},
             description = "Binomial distribution PMF.")
    static class PMF extends ProbabilityCommand {}

    /** LPMF command. */
    @Command(name = "lpmf",
             aliases = {"lpdf"},
             description = "Binomial distribution natural logarithm of the PMF.")
    static class LPMF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Binomial distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** Survival command. */
    @Command(name = "survival", aliases = {"sur"},
             description = "Binomial distribution survival probability.")
    static class Survival extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Binomial distribution inverse CDF.")
    static class ICDF extends InverseProbabilityCommand {}

    /** ISF command. */
    @Command(name = "isf",
             description = "Binomial distribution inverse SF.")
    static class ISF extends InverseProbabilityCommand {}
}
