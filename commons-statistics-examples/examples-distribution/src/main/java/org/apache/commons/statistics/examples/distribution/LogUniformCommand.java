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
import org.apache.commons.statistics.distribution.LogUniformDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link LogUniformDistribution}.
 */
@Command(name = "logunif",
         aliases = {"reciprocal"},
         description = "Log uniform distribution.",
         subcommands = {
             LogUniformCommand.Check.class,
             LogUniformCommand.PDF.class,
             LogUniformCommand.LPDF.class,
             LogUniformCommand.CDF.class,
             LogUniformCommand.SF.class,
             LogUniformCommand.ICDF.class,
             LogUniformCommand.ISF.class,
         })
class LogUniformCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractContinuousDistributionCommand {
        /** Distribution parameters. */
        @ArgGroup(validate = false, heading = "Distribution parameters:%n", order = 1)
        private Params params = new Params();

        /** Parameters class. */
        static class Params {
            /** The distribution lower limit. */
            @Option(names = {"-a", "--lower"},
                    paramLabel = "a",
                    arity = "1..*",
                    split = ",",
                    description = {"lower bound (default: ${DEFAULT-VALUE})."})
            private double[] lower = {1, 2};

            /** The distribution upper limit. */
            @Option(names = {"-b", "--upper"},
                    paramLabel = "b",
                    arity = "1..*",
                    split = ",",
                    description = {"upper bound (default: ${DEFAULT-VALUE})."})
            private double[] upper = {4, 6};
        }

        /** Extend the options to set the default values for this distribution. */
        static final class Options extends ContinuousDistributionOptions {
            /** Set defaults. */
            private Options() {
                min = -5;
                max = 5;
            }
        }

        @Override
        protected List<Distribution<ContinuousDistribution>> getDistributions() {
            double[] lower = params.lower;
            double[] upper = params.upper;
            final int n = DistributionUtils.validateLengths(lower.length, upper.length);

            lower = DistributionUtils.expandToLength(lower, n);
            upper = DistributionUtils.expandToLength(upper, n);

            // Create distributions
            final ArrayList<Distribution<ContinuousDistribution>> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final ContinuousDistribution d = LogUniformDistribution.of(lower[i], upper[i]);
                list.add(new Distribution<>(d, "a=" + lower[i] + ",b=" + upper[i]));
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
             description = "Log uniform distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PDF command. */
    @Command(name = "pdf",
             description = "Log uniform distribution PDF.")
    static class PDF extends ProbabilityCommand {}

    /** LPDF command. */
    @Command(name = "lpdf",
             description = "Log uniform distribution natural logarithm of the PDF.")
    static class LPDF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Log uniform distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** SF command. */
    @Command(name = "sf",
             description = "Log uniform distribution survival probability.")
    static class SF extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Log uniform distribution inverse CDF.")
    static class ICDF extends InverseProbabilityCommand {}

    /** ISF command. */
    @Command(name = "isf",
             description = "Log uniform distribution inverse SF.")
    static class ISF extends InverseProbabilityCommand {}
}
