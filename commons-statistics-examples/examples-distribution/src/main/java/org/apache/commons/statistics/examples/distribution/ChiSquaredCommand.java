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
package org.apache.commons.statistics.examples.distribution;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.statistics.distribution.ChiSquaredDistribution;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link ChiSquaredDistribution}.
 */
@Command(name = "chisq",
         aliases = {"chi2"},
         description = "Chi-squared distribution.",
         subcommands = {
             ChiSquaredCommand.Check.class,
             ChiSquaredCommand.PDF.class,
             ChiSquaredCommand.LPDF.class,
             ChiSquaredCommand.CDF.class,
             ChiSquaredCommand.SF.class,
             ChiSquaredCommand.ICDF.class,
             ChiSquaredCommand.ISF.class,
         })
class ChiSquaredCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractContinuousDistributionCommand {
        /** Distribution parameters. */
        @ArgGroup(validate = false, heading = "Distribution parameters:%n", order = 1)
        private Params params = new Params();

        /** Parameters class. */
        static class Params {
            /** The distribution degrees of freedom. */
            @Option(names = {"--df", "--degrees-of-freedom"},
                    arity = "1..*",
                    split = ",",
                    description = {"degrees of freedom (default: ${DEFAULT-VALUE})."})
            private double[] df = {1, 2, 3, 4, 6, 9};
        }

        /** Extend the options to set the default values for this distribution. */
        static final class Options extends ContinuousDistributionOptions {
            /** Set defaults. */
            private Options() {
                min = 0;
                max = 8;
            }
        }

        @Override
        protected List<Distribution<ContinuousDistribution>> getDistributions() {
            // Create distributions
            final ArrayList<Distribution<ContinuousDistribution>> list = new ArrayList<>();
            for (final double degreesOfFreedom : params.df) {
                final ContinuousDistribution d = ChiSquaredDistribution.of(degreesOfFreedom);
                list.add(new Distribution<>(d, "df=" + degreesOfFreedom));
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
             description = "Chi-squared distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PDF command. */
    @Command(name = "pdf",
             description = "Chi-squared distribution PDF.")
    static class PDF extends ProbabilityCommand {}

    /** LPDF command. */
    @Command(name = "lpdf",
             description = "Chi-squared distribution natural logarithm of the PDF.")
    static class LPDF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Chi-squared distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** SF command. */
    @Command(name = "sf",
             description = "Chi-squared distribution survival probability.")
    static class SF extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Chi-squared distribution inverse CDF.")
    static class ICDF extends InverseProbabilityCommand {}

    /** ISF command. */
    @Command(name = "isf",
             description = "Chi-squared distribution inverse SF.")
    static class ISF extends InverseProbabilityCommand {}
}
