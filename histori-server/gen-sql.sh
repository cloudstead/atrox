#!/bin/bash

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}
GEN_SQL="${BASE}/../utils/cloudos-lib/gen-sql.sh"

outfile=${BASE}/target/histori.sql

VERBOSE="${1}"

if [ -z "${VERBOSE}" ] ; then
  ${GEN_SQL} histori_test ${outfile} 1> /dev/null 2> /dev/null
else
  ${GEN_SQL} histori_test ${outfile} ${VERBOSE}
fi
