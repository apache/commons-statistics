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
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.ExponentialDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link ExponentialDistribution}.
 */
@Command(name = "exp",
         description = "Exponential distribution.",
         subcommands = {
             ExpCommand.Info.class,
             ExpCommand.Check.class,
             ExpCommand.PDF.class,
             ExpCommand.LPDF.class,
             ExpCommand.CDF.class,
             ExpCommand.SF.class,
             ExpCommand.ICDF.class,
             ExpCommand.ISF.class,
         })
class ExpCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractContinuousDistributionCommand {
        /** Distribution parameters. */
        @ArgGroup(validate = false, heading = "Distribution parameters:%n", order = 1)
        private Params params = new Params();

        /** Parameters class. */
        static class Params {
            /** The distribution mean. */
            @Option(names = {"-m", "--mean", "--scale"},
                    arity = "1..*",
                    split = ",",
                    description = {"mean (scale) (default: ${DEFAULT-VALUE})."})
            private double[] mean = {2, 1, 2.0 / 3};
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
        protected List<Distribution<ContinuousDistribution>> getDistributions() {
            // Create distributions
            final ArrayList<Distribution<ContinuousDistribution>> list = new ArrayList<>();
            for (final double m : params.mean) {
                final ContinuousDistribution d = ExponentialDistribution.of(m);
                list.add(new Distribution<>(d, "mean=" + m));
            }
            return list;
        }
    }

    /** Base command for the distribution that defines the parameters for probability functions. */
    private abstract static class ProbabilityCommand extends BaseCommand {
        /** Default constructor. */
        ProbabilityCommand() {
            super(new ContinuousDistributionOptions(0, 5));
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
             description = "Exponential distribution information.")
    static class Info extends BaseCommand {
        /** Default constructor. */
        Info() {
            super(new OutputOptions());
        }
    }

    /** Verification checks command. */
    @Command(name = "check",
             hidden = true,
             description = "Exponential distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PDF command. */
    @Command(name = "pdf",
             description = "Exponential distribution PDF.")
    static class PDF extends ProbabilityCommand {}

    /** LPDF command. */
    @Command(name = "lpdf",
             description = "Exponential distribution natural logarithm of the PDF.")
    static class LPDF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Exponential distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** SF command. */
    @Command(name = "sf",
             description = "Exponential distribution survival probability.")
    static class SF extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Exponential distribution inverse CDF.")
    static class ICDF extends InverseProbabilityCommand {}

    /** ISF command. */
    @Command(name = "isf",
             description = "Exponential distribution inverse SF.")
    static class ISF extends InverseProbabilityCommand {}
}
