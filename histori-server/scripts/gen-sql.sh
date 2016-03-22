#!/bin/bash

HISTORI_SERVER=$(cd $(dirname $0)/../.. && pwd)
cd ${HISTORI_SERVER}
GEN_SQL="${HISTORI_SERVER}/utils/cloudos-lib/gen-sql.sh"

outfile=${HISTORI_SERVER}/src/main/resources/seed/schema.sql

VERBOSE="${1}"

if [ -z "${VERBOSE}" ] ; then
  ${GEN_SQL} histori_test ${outfile} 1> /dev/null 2> /dev/null
else
  ${GEN_SQL} histori_test ${outfile} ${VERBOSE}
fi
