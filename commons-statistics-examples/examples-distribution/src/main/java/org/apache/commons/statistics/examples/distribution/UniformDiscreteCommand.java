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
import org.apache.commons.statistics.distribution.DiscreteDistribution;
import org.apache.commons.statistics.distribution.UniformDiscreteDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link UniformDiscreteDistribution}.
 */
@Command(name = "unid",
         description = "Discrete uniform distribution.",
         subcommands = {
             UniformDiscreteCommand.Info.class,
             UniformDiscreteCommand.Check.class,
             UniformDiscreteCommand.PMF.class,
             UniformDiscreteCommand.LPMF.class,
             UniformDiscreteCommand.CDF.class,
             UniformDiscreteCommand.SF.class,
             UniformDiscreteCommand.ICDF.class,
             UniformDiscreteCommand.ISF.class,
         })
class UniformDiscreteCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractDiscreteDistributionCommand {
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
            private int[] lower = {0, -3};

            /** The distribution upper limit. */
            @Option(names = {"-b", "--upper"},
                    paramLabel = "b",
                    arity = "1..*",
                    split = ",",
                    description = {"upper bound (default: ${DEFAULT-VALUE})."})
            private int[] upper = {5, 4};
        }

        /**
         * Create an instance.
         *
         * @param outputOptions the output options
         */
        BaseCommand(OutputOptions outputOptions) {
            super(outputOptions);
        }

        @Override
        protected List<Distribution<DiscreteDistribution>> getDistributions() {
            int[] lower = params.lower;
            int[] upper = params.upper;
            final int n = DistributionUtils.validateLengths(lower.length, upper.length);

            lower = DistributionUtils.expandToLength(lower, n);
            upper = DistributionUtils.expandToLength(upper, n);

            // Create distributions
            final ArrayList<Distribution<DiscreteDistribution>> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final DiscreteDistribution d = UniformDiscreteDistribution.of(lower[i], upper[i]);
                list.add(new Distribution<>(d, "a=" + lower[i] + ",b=" + upper[i]));
            }
            return list;
        }
    }

    /** Base command for the distribution that defines the parameters for probability functions. */
    private abstract static class ProbabilityCommand extends BaseCommand {
        /** Default constructor. */
        ProbabilityCommand() {
            super(new ContinuousDistributionOptions(-5, 5));
        }
    }

    /** Base command for the distribution that defines the parameters for inverse probability functions. */
    private abstract static class InverseProbabilityCommand extends BaseCommand {
        /** Default constructor. */
        InverseProbabilityCommand() {
            super(new InverseContinuousDistributionOptions());
        }
    }

    /** Information command. */
    @Command(name = "info",
             description = "Discrete uniform distribution information.")
    static class Info extends BaseCommand {
        /** Default constructor. */
        Info() {
            super(new OutputOptions());
        }
    }

    /** Verification checks command. */
    @Command(name = "check",
             hidden = true,
             description = "Discrete uniform distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PMF command. */
    @Command(name = "pmf",
             aliases = {"pdf"},
             description = "Discrete uniform distribution PMF.")
    static class PMF extends ProbabilityCommand {}

    /** LPMF command. */
    @Command(name = "lpmf",
             aliases = {"lpdf"},
             description = "Discrete distribution natural logarithm of the PMF.")
    static class LPMF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Discrete uniform distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** SF command. */
    @Command(name = "sf",
             description = "Discrete uniform distribution survival probability.")
    static class SF extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Discrete uniform distribution inverse CDF.")
    static class ICDF extends InverseProbabilityCommand {}

    /** ISF command. */
    @Command(name = "isf",
             description = "Discrete uniform distribution inverse SF.")
    static class ISF extends InverseProbabilityCommand {}
}
