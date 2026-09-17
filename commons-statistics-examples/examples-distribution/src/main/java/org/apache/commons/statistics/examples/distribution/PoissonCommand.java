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
import org.apache.commons.statistics.distribution.PoissonDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link PoissonDistribution}.
 */
@Command(name = "poisson",
         aliases = {"poiss"},
         description = "Poisson distribution.",
         subcommands = {
             PoissonCommand.Info.class,
             PoissonCommand.Check.class,
             PoissonCommand.PMF.class,
             PoissonCommand.LPMF.class,
             PoissonCommand.CDF.class,
             PoissonCommand.SF.class,
             PoissonCommand.ICDF.class,
             PoissonCommand.ISF.class,
         })
class PoissonCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractDiscreteDistributionCommand {
        /** Distribution parameters. */
        @ArgGroup(validate = false, heading = "Distribution parameters:%n", order = 1)
        private Params params = new Params();

        /** Parameters class. */
        static class Params {
            /** The distribution mean. */
            @Option(names = {"-m", "--mean"},
                    arity = "1..*",
                    split = ",",
                    description = {"mean (default: ${DEFAULT-VALUE})."})
            private double[] mean = {1, 4, 10};
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
            // Create distributions
            final ArrayList<Distribution<DiscreteDistribution>> list = new ArrayList<>();
            for (final double m : params.mean) {
                final DiscreteDistribution d = PoissonDistribution.of(m);
                list.add(new Distribution<>(d, "mean=" + m));
            }
            return list;
        }
    }

    /** Base command for the distribution that defines the parameters for probability functions. */
    private abstract static class ProbabilityCommand extends BaseCommand {
        /** Default constructor. */
        ProbabilityCommand() {
            super(new DiscreteDistributionOptions(0, 20));
        }
    }

    /** Base command for the distribution that defines the parameters for inverse probability functions. */
    private abstract static class InverseProbabilityCommand extends BaseCommand {
        /** Default constructor. */
        InverseProbabilityCommand() {
            super(new InverseDiscreteDistributionOptions());
        }
    }

    /** Information command. */
    @Command(name = "info",
             description = "Poisson distribution information.")
    static class Info extends BaseCommand {
        /** Default constructor. */
        Info() {
            super(new OutputOptions());
        }
    }

    /** Verification checks command. */
    @Command(name = "check",
             hidden = true,
             description = "Poisson distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PMF command. */
    @Command(name = "pmf",
             aliases = {"pdf"},
             description = "Poisson distribution PMF.")
    static class PMF extends ProbabilityCommand {}

    /** LPMF command. */
    @Command(name = "lpmf",
             aliases = {"lpdf"},
             description = "Poisson distribution natural logarithm of the PMF.")
    static class LPMF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Poisson distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** SF command. */
    @Command(name = "sf",
             description = "Poisson distribution survival probability.")
    static class SF extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Poisson distribution inverse CDF.")
    static class ICDF extends InverseProbabilityCommand {}

    /** ISF command. */
    @Command(name = "isf",
             description = "Poisson distribution inverse SF.")
    static class ISF extends InverseProbabilityCommand {}
}
