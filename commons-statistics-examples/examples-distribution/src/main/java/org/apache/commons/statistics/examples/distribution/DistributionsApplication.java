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

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Construct instances of probability distributions using the Commons Statistics library.
 * Output values from the distribution functions: PDF; CDF; SF Function; and Inverse CDF.
 */
public final class DistributionsApplication {

    // Implementation Notes
    //
    // This application constructs distribution instances of the Commons Statistics library
    // and calls the standard distribution functions:
    //
    // PDF(x)          Probability Density Function (continuous distributions)
    // PMF(x)          Probability Mass Function (discrete distributions)
    // CDF(x)          Cumulative Probability Function
    // SF(x)           Survival Probability Function
    // InverseCDF(p)   Inverse Cumulative Probability Function
    // InverseSF(p)    Inverse Survival Probability Function
    //
    // There is very little logic in the application. All the classes serve to provide
    // commands for annotation with the PicoCLI library. Each command must have an annotated
    // instance. Since the functions (pdf, pmf, icdf) require different arguments
    // (x real, x int, p-value) this implementation
    // avoids using the function as an option to avoid specifying a generic set of parameters
    // covering both input x as real or integer and input probability as real in [0, 1].
    // This creates many classes with the same structure. Maintenance is possible using
    // carefully constructed regex replacements across the Command classes.
    //
    // Note that PicoCLI constructs classes using the no arguments constructor. Thus any
    // defaults for arguments must be declared inline (for primitives) or set in the no
    // arguments constructor for objects. Reusing the same options class instantiated with
    // arguments to the constructor to set default values is ignored when the option is
    // changed by a command line argument. In this case PicoCLI will create
    // a new instance with the no arguments constructor and the overridden values are lost.
    //
    // Discrete distributions have a PMF. For convenience this has an alias
    // as pdf so all distributions can be called using the same syntax.
    // Aliases have been used for the distribution names to shorten commands.

    /** No public constructor. */
    private DistributionsApplication() {}

    /**
     * Specification for the top-level command in the examples distributions application.
     *
     * <p>This command will print the top-level help message.</p>
     */
    @Command(name = "distributions",
             description = "Apache Commons Statistics Distribution Utilities.",
             subcommands = {
                 BetaCommand.class,
                 BinomialCommand.class,
                 CauchyCommand.class,
                 ChiSquaredCommand.class,
                 ExpCommand.class,
                 FCommand.class,
                 FoldedNormalCommand.class,
                 GammaCommand.class,
                 GeometricCommand.class,
                 GumbelCommand.class,
                 HypergeometricCommand.class,
                 LaplaceCommand.class,
                 LevyCommand.class,
                 LogisticCommand.class,
                 LogNormalCommand.class,
                 LogUniformCommand.class,
                 NakagamiCommand.class,
                 NormalCommand.class,
                 ParetoCommand.class,
                 PascalCommand.class,
                 PoissonCommand.class,
                 TCommand.class,
                 TrapezoidalCommand.class,
                 TriangularCommand.class,
                 TruncatedNormalCommand.class,
                 UniformContinuousCommand.class,
                 UniformDiscreteCommand.class,
                 WeibullCommand.class,
                 ZipfCommand.class,
             })
    static class DistributionsCommand extends AbstractDistributionCommand {
        // Nothing to do
    }

    /**
     * Run the Statistics examples command line application.
     *
     * @param args Application's arguments.
     */
    public static void main(String[] args) {
        // The command line is built using annotations
        final CommandLine cmd = new CommandLine(new DistributionsCommand());
        System.exit(cmd.execute(args));
    }
}
