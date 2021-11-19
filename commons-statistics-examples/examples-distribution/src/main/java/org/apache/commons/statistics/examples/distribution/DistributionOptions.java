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

import java.io.File;
import picocli.CommandLine.Option;

/**
 * Standard options for distribution commands.
 */
class DistributionOptions {

    /** The distribution function. */
    protected DistributionFunction distributionFunction;

    /** The field delimiter. */
    @Option(names = { "--delim" },
            description = {"Output field delimiter (default: \\t)."})
    protected String delim = "\t";

    /** The output file. */
    @Option(names = { "--out" },
            paramLabel = "file",
            description = {"Output file (default: stdout)."})
    protected File outputFile;

    /** The output file. */
    @Option(names = { "--in" },
            paramLabel = "file",
            description = {"Input file containing points to evaluate.",
                           "Overrides configured ranges."})
    protected File inputFile;

    /** Flag indicating if an exception should be suppressed during function evaluation.
     * Exceptions are thrown by the ICDF and ISF functions when the input probability is not in
     * the interval {@code [0, 1]}. */
    @Option(names = { "--no-ex", "--no-exception" },
            description = {"Suppress function evaluation exceptions (returns NaN or integer min value)."})
    protected boolean suppressException;
}
