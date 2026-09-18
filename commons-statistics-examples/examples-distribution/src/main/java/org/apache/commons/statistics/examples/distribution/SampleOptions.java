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

import org.apache.commons.rng.simple.RandomSource;
//import org.apache.commons.rng.simple.RandomSource;
import picocli.CommandLine.Option;

/**
 * Standard options for distribution commands.
 */
class SampleOptions extends OutputOptions {

    /** The number of samples. */
    @Option(names = { "--samples" },
            paramLabel = "n",
            description = {"Number of samples."})
    protected int samples = 10;

    /** The source of randomness. */
    @Option(names = { "--rng" },
            description = {"Source of randomness."})
    protected RandomSource source = RandomSource.L128_X256_MIX;
}
