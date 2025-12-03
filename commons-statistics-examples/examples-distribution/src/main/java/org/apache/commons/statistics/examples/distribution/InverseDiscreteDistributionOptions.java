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
 * Options for inverse probability functions of discrete distributions.
 * Applies to functions x=real in [0, 1] and f(x)=integer.
 */
class InverseDiscreteDistributionOptions extends DistributionOptions {
    /** The minimum probability to evaluate. */
    @Option(names = { "--min" },
            description = {"Minimum probability to evaluate (default: ${DEFAULT-VALUE})."})
    protected double min;

    /** The maximum probability to evaluate. */
    @Option(names = { "--max" },
            description = {"Maximum probability to evaluate (default: ${DEFAULT-VALUE})."})
    protected double max = 1;

    /** The number of steps from minimum to the maximum. */
    @Option(names = { "--steps" },
            description = {"Number of steps from the minimum to the maximum (default: ${DEFAULT-VALUE})."})
    protected int steps = 100;

    /** The single point to evaluate. */
    @Option(names = { "-x", "--point" },
            arity = "1..*",
            split = ",",
            description = {"Point to evaluate (overrides input range)."})
    protected double[] x;

    /** The format for the evaluated probability. */
    @Option(names = { "--pformat" },
            description = {"Format for the evaluated probability (default: ${DEFAULT-VALUE})."})
    protected String pformat = "%.6g";
}
