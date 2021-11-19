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
import org.apache.commons.statistics.distribution.GumbelDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link GumbelDistribution}.
 */
@Command(name = "gumbel",
         description = "Gumbel distribution.",
         subcommands = {
             GumbelCommand.Check.class,
             GumbelCommand.PDF.class,
             GumbelCommand.LPDF.class,
             GumbelCommand.CDF.class,
             GumbelCommand.Survival.class,
             GumbelCommand.ICDF.class,
             GumbelCommand.ISF.class,
         })
class GumbelCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractContinuousDistributionCommand {
        /** Distribution parameters. */
        @ArgGroup(validate = false, heading = "Distribution parameters:%n", order = 1)
        private Params params = new Params();

        /** Parameters class. */
        static class Params {
            /** The distribution location. */
            @Option(names = {"--mu", "--location"},
                    arity = "1..*",
                    split = ",",
                    description = {"location (default: ${DEFAULT-VALUE})."})
            private double[] mu = {0.5, 1, 1.5, 3};

            /** The distribution scale. */
            @Option(names = {"--beta", "--scale"},
                    arity = "1..*",
                    split = ",",
                    description = {"scale (default: ${DEFAULT-VALUE})."})
            private double[] beta = {2, 2, 3, 4};
        }

        /** Extend the options to set the default values for this distribution. */
        static final class Options extends ContinuousDistributionOptions {
            /** Set defaults. */
            private Options() {
                min = -5;
                max = 20;
            }
        }

        @Override
        protected List<Distribution<ContinuousDistribution>> getDistributions() {
            double[] location = params.mu;
            double[] scale = params.beta;
            final int n = DistributionUtils.validateLengths(location.length, scale.length);

            location = DistributionUtils.expandToLength(location, n);
            scale = DistributionUtils.expandToLength(scale, n);

            // Create distributions
            final ArrayList<Distribution<ContinuousDistribution>> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final ContinuousDistribution d = GumbelDistribution.of(location[i], scale[i]);
                list.add(new Distribution<>(d, "mu=" + location[i] + ",beta=" + scale[i]));
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
             description = "Gumbel distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PDF command. */
    @Command(name = "pdf",
             description = "Gumbel distribution PDF.")
    static class PDF extends ProbabilityCommand {}

    /** LPDF command. */
    @Command(name = "lpdf",
             description = "Gumbel distribution natural logarithm of the PDF.")
    static class LPDF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Gumbel distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** Survival command. */
    @Command(name = "survival", aliases = {"sur"},
             description = "Gumbel distribution survival probability.")
    static class Survival extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Gumbel distribution inverse CDF.")
    static class ICDF extends InverseProbabilityCommand {}

    /** ISF command. */
    @Command(name = "isf",
             description = "Gumbel distribution inverse SF.")
    static class ISF extends InverseProbabilityCommand {}
}
