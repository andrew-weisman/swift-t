#!/bin/bash
set -eu

MPICC=$( which mpicc )
MPI=$( dirname $( dirname ${MPICC} ) )

set -x
echo configscript
CONFIG_SCRIPT=$( turbine -C )
source $CONFIG_SCRIPT

TURBINE_INCLUDE=${TURBINE_HOME}/include
TURBINE_LIB=${TURBINE_HOME}/lib

INCLUDES="-I . ${TURBINE_INCLUDES}"

set -x

stc -r ${PWD} -r ${TURBINE_LIB} test-f.swift test-f.tic

${MPICC} -c -Wall ${INCLUDES} controller.c
${MPICC} -o controller.x controller.o -L ${MPI_LIB_DIR} \
         ${TURBINE_LIBS} ${TURBINE_RPATH}
