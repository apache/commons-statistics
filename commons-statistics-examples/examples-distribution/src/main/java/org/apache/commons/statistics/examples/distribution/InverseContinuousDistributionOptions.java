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
 * Options for inverse probability functions of continuous distributions.
 * Applies to functions x=real in [0, 1] and f(x)=real.
 */
class InverseContinuousDistributionOptions extends InverseDiscreteDistributionOptions {
    /** The format for the output values. */
    @Option(names = { "--format" },
            description = {"Format for the output values (default: ${DEFAULT-VALUE})."})
    protected String format = "%s";
}
