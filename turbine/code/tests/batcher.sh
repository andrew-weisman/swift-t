#!/bin/bash
# Copyright 2013 University of Chicago and Argonne National Laboratory
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

source tests/test-helpers.sh

set -x

THIS=$0
SCRIPT=${THIS%.sh}.tcl
OUTPUT=${THIS%.sh}.out

mkdir tests/data

#bin/turbine -l -n 4 ${SCRIPT} \
turbine -l -n 4 ${SCRIPT} \  #Testing Azza's system
                    tests/batcher.txt >& ${OUTPUT}
[[ ${?} == 0 ]] || test_result 1

LINES=$( ls tests/data/{1..4}.txt | wc -l )
(( ${LINES} == 4 )) || test_result 1

rm tests/data/{1..4}.txt || test_result 1

test_result 0
