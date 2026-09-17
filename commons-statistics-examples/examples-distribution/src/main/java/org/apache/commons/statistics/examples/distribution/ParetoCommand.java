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
import org.apache.commons.statistics.distribution.ParetoDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command for the {@link ParetoDistribution}.
 */
@Command(name = "pareto",
         description = "Pareto distribution.",
         subcommands = {
             ParetoCommand.Info.class,
             ParetoCommand.Check.class,
             ParetoCommand.PDF.class,
             ParetoCommand.LPDF.class,
             ParetoCommand.CDF.class,
             ParetoCommand.SF.class,
             ParetoCommand.ICDF.class,
             ParetoCommand.ISF.class,
         })
class ParetoCommand extends AbstractDistributionCommand {

    /** Base command for the distribution that defines the parameters. */
    private abstract static class BaseCommand extends AbstractContinuousDistributionCommand {
        /** Distribution parameters. */
        @ArgGroup(validate = false, heading = "Distribution parameters:%n", order = 1)
        private Params params = new Params();

        /** Parameters class. */
        static class Params {
            /** The distribution scale. */
            @Option(names = {"--xm", "--scale"},
                    paramLabel = "xm",
                    arity = "1..*",
                    split = ",",
                    description = {"scale (default: ${DEFAULT-VALUE})."})
            private double[] scale = {1};

            /** The distribution shape. */
            @Option(names = {"--alpha", "--shape"},
                    arity = "1..*",
                    split = ",",
                    description = {"shape (default: ${DEFAULT-VALUE})."})
            private double[] shape = {3, 2, 1};
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
            double[] scale = params.scale;
            double[] shape = params.shape;
            final int n = DistributionUtils.validateLengths(shape.length, scale.length);

            scale = DistributionUtils.expandToLength(scale, n);
            shape = DistributionUtils.expandToLength(shape, n);

            // Create distributions
            final ArrayList<Distribution<ContinuousDistribution>> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final ContinuousDistribution d = ParetoDistribution.of(scale[i], shape[i]);
                list.add(new Distribution<>(d, "xm=" + scale[i] + ",alpha=" + shape[i]));
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
             description = "Pareto distribution information.")
    static class Info extends BaseCommand {
        /** Default constructor. */
        Info() {
            super(new OutputOptions());
        }
    }

    /** Verification checks command. */
    @Command(name = "check",
             hidden = true,
             description = "Pareto distribution verification checks.")
    static class Check extends ProbabilityCommand {}

    /** PDF command. */
    @Command(name = "pdf",
             description = "Pareto distribution PDF.")
    static class PDF extends ProbabilityCommand {}

    /** LPDF command. */
    @Command(name = "lpdf",
             description = "Pareto distribution natural logarithm of the PDF.")
    static class LPDF extends ProbabilityCommand {}

    /** CDF command. */
    @Command(name = "cdf",
             description = "Pareto distribution CDF.")
    static class CDF extends ProbabilityCommand {}

    /** SF command. */
    @Command(name = "sf",
             description = "Pareto distribution survival probability.")
    static class SF extends ProbabilityCommand {}

    /** ICDF command. */
    @Command(name = "icdf",
             description = "Pareto distribution inverse CDF.")
    static class ICDF extends InverseProbabilityCommand {}

    /** ISF command. */
    @Command(name = "isf",
             description = "Pareto distribution inverse SF.")
    static class ISF extends InverseProbabilityCommand {}
}
