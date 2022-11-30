<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

Apache Commons Statistics Examples Distribution  Utilities
==========================================================

Application for calling distributions defined in Commons Statistics.

Build the package using:

    mvn package -Pexamples-distributions

The application contains help information describing the usage. Obtain the help
by running the application with the `-h` flag to obtain the available commands
and to obtain information for the commands:

    java -jar target/examples-distributions.jar -h
    java -jar target/examples-distributions.jar exp -h

Each command of the application will create an instance of the distribution
named by the command. Each distribution command has the following functions:

| Function | Description |
| -------- | ----------- |
| pdf | Probability Density Function (continuous distributions) |
| pmf | Probability Mass Function (discrete distributions) |
| cdf | Cumulative Probability Density Function |
| sf | Survival Probability Function |
| icdf | Inverse Cumulative Probability Density Function |
| isf | Inverse Survival Probability Function |
| lpdf | Natural logarithm of the PDF (continuous distributions) |
| lpmf | Natural logarithm of the PMF (discrete distributions) |

For convenicence the discrete distribution pmf and lpmf commands are aliased to pdf and lpdf.

The pdf, cdf and survival functions accept an input value and output a probability or
probability density. The icdf and isf functions accepts an input probability and outputs a value.
The distribution is configured with suitable defaults for the distribution parameters
and the range of values to evaluate. Executing the command will output a delimited
table that can be plotted for visual inspection. The output will default to stdout and
can optionally be written to file.

Distributions for input values that are real numbers will be evaluated between a
minimum and maximum with a specified number of steps used to move from min to max.
For example 1 to 5 in 10 steps would use a difference of 0.4 between values.

Distributions for input values that are integers will be evaluated between a
minimum and maximum with a specified increment used to move from min to max.
For example 1 to 20 with an increment of 2 would output 11 values. The default
increment is 1 so caution must be taken when using a large range to avoid excess
evaluations.

Points to evaluate can be overridden by: using individual values; updating the
enumerated range; or using an input file.

Distributions that require multiple parameters must specify either a single parameter
value or ``n`` values where ``n`` is the same for each parameter. Any parameter
with 1 value will be expanded to size ``n``.

Examples
--------

To create a sample of the PDF for the exponential distribution:

    java -jar target/examples-distribution.jar exp pdf
    java -jar target/examples-distribution.jar exp pdf --mean 3.45
    java -jar target/examples-distribution.jar exp pdf \
        --mean 3.45 --min 3 --max 5 --steps 50

To create a sample of the CDF for the Poisson distribution:

    java -jar target/examples-distribution.jar poisson cdf
    java -jar target/examples-distribution.jar poisson cdf --mean 3.45
    java -jar target/examples-distribution.jar poisson cdf \
        --mean 12.34 --min 0 --max 50 --increment 2

To create a sample of the PDF for the Gamma distribution with different shape and
a fixed scale:

    java -jar target/examples-distribution.jar gamma pdf \
        --shape 0.5,1,2 --scale 1
