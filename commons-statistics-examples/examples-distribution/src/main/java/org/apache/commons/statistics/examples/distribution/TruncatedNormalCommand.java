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
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.TruncatedNormalDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link TruncatedNormalDistribution}.
 */
@Command(name = "truncatednormal",
         aliases = {"tnorm"},
         description = "Truncated normal distribution.",
         subcommands = {
             TruncatedNormalCommand.Check.class,
             TruncatedNormalCommand.PDF.class,
             TruncatedNormalCommand.LPDF.class,
             TruncatedNormalCommand.CDF.class,
             TruncatedNormalCommand.Survival.class,
             TruncatedNormalCommand.ICDF.class,
             TruncatedNormalCommand.ISF.class,
         })
class TruncatedNormalCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractContinuousDistributionCommand {
        /** The default minimum for x. */
        static final double MIN = -10;
        /** The default maximum for x. */
        static final double MAX = 10;

        /** Distribution parameters. */
        @ArgGroup(validate = false, heading = "Distribution parameters:%n", order = 1)
        private Params params = new Params();

        /** Parameters class. */
        static class Params {
            /** The distribution mean. */
            @Option(names = {"-m", "--mu", "--mean"},
                    arity = "1..*",
                    split = ",",
                    description = {"mean (default: ${DEFAULT-VALUE})."})
            private double[] mu = {-8, 0, 9, 0};

            /** The distribution sigma. */
            @Option(names = {"-s", "--sigma"},
                    arity = "1..*",
                    split = ",",
                    description = {"standard deviation (default: ${DEFAULT-VALUE})."})
            private double[] sigma = {2, 2, 10, 10};

            /** The distribution lower limit. */
            @Option(names = {"-l", "--lower"},
                    arity = "1..*",
                    split = ",",
                    description = {"lower bound (default: ${DEFAULT-VALUE})."})
            private double[] lower = {MIN};

            /** The distribution upper limit. */
            @Option(names = {"-u", "--upper"},
                    arity = "1..*",
                    split = ",",
                    description = {"upper bound (default: ${DEFAULT-VALUE})."})
            private double[] upper = {MAX};
        }

        /** Extend the options to set the default values for this distribution. */
        static final class Options extends ContinuousDistributionOptions {
            /** Set defaults. */
            private Options() {
                min = MIN;
                max = MAX;
            }
        }

        @Override
        protected List<Distribution<ContinuousDistribution>> getDistributions() {
            double[] mean = params.mu;
            double[] sigma = params.sigma;
            double[] lower = params.lower;
            double[] upper = params.upper;
            final int n = DistributionUtils.validateLengths(mean.length, sigma.length, lower.length, upper.length);

            mean = DistributionUtils.expandToLength(mean, n);
            sigma = DistributionUtils.expandToLength(sigma, n);
            lower = DistributionUtils.expandToLength(lower, n);
            upper = DistributionUtils.expandToLength(upper, n);

            // Create distributions
            final ArrayList<Distribution<ContinuousDistribution>> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final ContinuousDistribution d = TruncatedNormalDistribution.of(mean[i], sigma[i], lower[i], upper[i]);
                list.add(new Distribution<>(d, "mu=" + mean[i] + ",sigma=" + sigma[i] +
                                               ",lower=" + lower[i] + ",upper=" + upper[i]));
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
        private InverseContinuousDistributionOptions distributionOptions = new InverseContinuousDistributionOptions();

        @Override
        protected DistributionOptions getDistributionOptions() {
            return distributionOptions;
        }
    }

    /** Verification checks command. */
    @Command(name = "check",
             hidden = true,
             description = "Truncated normal distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PDF command. */
    @Command(name = "pdf",
             description = "Truncated normal distribution PDF.")
    static class PDF extends ProbabilityCommand {}

    /** LPDF command. */
    @Command(name = "lpdf",
             description = "Truncated normal distribution natural logarithm of the PDF.")
    static class LPDF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Truncated normal distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** Survival command. */
    @Command(name = "survival", aliases = {"sur"},
             description = "Truncated normal distribution survival probability.")
    static class Survival extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Truncated normal distribution inverse CDF.")
    static class ICDF extends InverseProbabilityCommand {}

    /** ISF command. */
    @Command(name = "isf",
             description = "Truncated normal distribution inverse SF.")
    static class ISF extends InverseProbabilityCommand {}
}
