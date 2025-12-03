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

import picocli.CommandLine.Option;

/**
 * Options for functions of discrete distributions.
 * Applies to functions x=integer and f(x)=real.
 */
class DiscreteDistributionOptions extends DistributionOptions {
    /** The minimum point to evaluate. */
    @Option(names = { "--min" },
            description = {"Minimum point to evaluate (default: ${DEFAULT-VALUE})."})
    protected int min;

    /** The maximum point to evaluate. */
    @Option(names = { "--max" },
            description = {"Maximum point to evaluate (default: ${DEFAULT-VALUE})."})
    protected int max = 20;

    /** The increment from minimum to the maximum. */
    @Option(names = { "-i", "--increment" },
            description = {"Increment from the minimum to the maximum (default: ${DEFAULT-VALUE})."})
    protected int increment = 1;

    /** The single point to evaluate. */
    @Option(names = { "-x", "--point" },
            arity = "1..*",
            split = ",",
            description = {"Point to evaluate (overrides input range)."})
    protected int[] x;

    /** The format for the output values. */
    @Option(names = { "--format" },
            description = {"Format for the output values (default: ${DEFAULT-VALUE})."})
    protected String format = "%s";
}
