#!/bin/bash

BASE=$(cd $(dirname $0)/../.. && pwd)
GEN_SQL="${BASE}/utils/cloudos-lib/gen-sql.sh"

HISTORI_SERVER="${BASE}/histori-server"
outfile=${HISTORI_SERVER}/src/main/resources/seed/schema.sql

VERBOSE="${1}"
cd ${HISTORI_SERVER}
if [ -z "${VERBOSE}" ] ; then
  ${GEN_SQL} histori_test ${outfile} 1> /dev/null 2> /dev/null
else
  ${GEN_SQL} histori_test ${outfile} ${VERBOSE}
fi
