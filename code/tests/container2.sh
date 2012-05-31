#!/bin/bash

THIS=$0
SCRIPT=${THIS%.sh}.tcl
OUTPUT=${THIS%.sh}.out

source $( dirname $0 )/setup.sh > ${OUTPUT} 2>&1

set -x

bin/turbine -l -n ${PROCS} ${SCRIPT} >> ${OUTPUT} 2>&1
[[ ${?} == 0 ]] || exit 1

grep -q "enumeration: 0 1"    ${OUTPUT} || exit 1
grep -q "retrieve:.*string2" ${OUTPUT} || exit 1
grep -q "retrieve:.*string3" ${OUTPUT} || exit 1

exit 0
