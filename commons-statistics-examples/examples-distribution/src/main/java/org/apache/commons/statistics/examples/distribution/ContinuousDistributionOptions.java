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

import picocli.CommandLine.Option;

/**
 * Options for functions of continuous distributions.
 * Applies to functions x=real and f(x)=real.
 */
class ContinuousDistributionOptions extends DistributionOptions {
    /** The minimum point to evaluate. */
    @Option(names = { "--min" },
            description = {"Minimum point to evaluate (default: ${DEFAULT-VALUE})."})
    protected double min;

    /** The maximum point to evaluate. */
    @Option(names = { "--max" },
            description = {"Maximum point to evaluate (default: ${DEFAULT-VALUE})."})
    protected double max = 5;

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

    /** The format for the evaluated point. */
    @Option(names = { "--xformat" },
            description = {"Format for the evaluated point (default: ${DEFAULT-VALUE})."})
    protected String xformat = "%.6g";

    /** The format for the output values. */
    @Option(names = { "--format" },
            description = {"Format for the output values (default: ${DEFAULT-VALUE})."})
    protected String format = "%s";
}
