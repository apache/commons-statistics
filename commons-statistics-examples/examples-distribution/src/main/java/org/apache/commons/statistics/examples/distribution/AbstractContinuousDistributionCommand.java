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

import java.util.List;
import java.util.concurrent.Callable;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;

/**
 * Base command for a continuous distribution.
 *
 * <p>Sub-classes are assumed to have a name that corresponds to the command to action.
 * Typically this is a function value in the {@link DistributionFunction} enum. The
 * distribution will be evaluated for this function using the points defined by the
 * {@link DistributionOptions}.
 *
 * <p>Alternatively the sub-class name may indicate a special command to execute.
 *
 * <p>Sub-classes must provide the list of distributions to evaluate and options for the
 * evaluation.
 */
abstract class AbstractContinuousDistributionCommand implements Callable<Void> {
    /** Heading for distribution parameters. */
    static final String HEADING_DISTRIBUTION_PARAMETERS = "Distribution parameters:%n";
    /** Heading for evaluation options. */
    static final String HEADING_EVALUATION_OPTIONS = "Evaluation options:%n";

    /** The standard options. */
    @Mixin
    private StandardOptions standardOptions;

    /** The distribution options. */
    @ArgGroup(validate = false, heading = HEADING_EVALUATION_OPTIONS, order = 2)
    private OutputOptions outputOptions;

    /**
     * Create an instance.
     *
     * @param outputOptions the output options
     */
    AbstractContinuousDistributionCommand(OutputOptions outputOptions) {
        this.outputOptions = outputOptions;
    }

    @Override
    public Void call() {
        final List<Distribution<ContinuousDistribution>> distributions = getDistributions();
        // Set the function based on the class name
        final String name = getClass().getSimpleName();

        // Special handling of sub-classes which do not evaluate a function of the distribution.
        // This is done here to avoid duplicating an overridden 'call()' method for each instance.
        if ("Info".equals(name)) {
            DistributionUtils.infoContinuous(distributions, outputOptions);
            return null;
        }

        // Assume this is an evaluation of the distribution
        final DistributionOptions distributionOptions = (DistributionOptions) outputOptions;

        // Special handling of sub-classes which do not evaluate a function of the distribution.
        // This is done here to avoid duplicating an overridden 'call()' method for each instance.
        // Currently this applies to a 'check' command that performs verification checks
        // on the distribution.
        if ("Check".equals(name)) {
            // This is not an evaluation of a single function.
            DistributionUtils.check(distributions,
                (ContinuousDistributionOptions) distributionOptions);
            return null;
        }

        // Assume an evaluation of a distribution function
        distributionOptions.distributionFunction = DistributionFunction.valueOf(name);
        // Assume there are only two types of options:
        // Forward functions: x (real) -> p-value
        // Inverse functions: p-value -> x (real)
        if (distributionOptions instanceof InverseContinuousDistributionOptions) {
            DistributionUtils.evaluate(distributions,
                (InverseContinuousDistributionOptions) distributionOptions);
        } else {
            DistributionUtils.evaluate(distributions,
                (ContinuousDistributionOptions) distributionOptions);
        }
        return null;
    }

    /**
     * Gets the distributions to evaluate.
     *
     * @return the distributions
     */
    protected abstract List<Distribution<ContinuousDistribution>> getDistributions();

    /**
     * Gets the output options.
     * Sub-classes can define the points to evaluate.
     *
     * @return the output options
     */
    protected OutputOptions getOutputOptions() {
        return outputOptions;
    }
}
