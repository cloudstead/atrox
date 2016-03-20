#!/bin/bash

function die {
  echo 1>&2 "${1}"
  exit 1
}

SCHEMA_SQL=/home/histori/histori/histori-server/src/main/resources/seed/schema.sql
INDEX_SQL=/home/histori/histori/histori-server/src/main/resources/seed/index.sql

if [ $(whoami) != "histori" ] ; then
  die "Must run $0 as histori user"
fi

MVN="mvn -DskipTests=true"

git clone git@github.com:cloudstead/histori.git && \
cd histori && \
git submodule init && git submodule update && \
cd /home/histori/histori/utils/cobbzilla-parent && ${MVN} install && \
cd /home/histori/histori/utils/ && ${MVN} install && \
cd /home/histori/histori/ && ${MVN} package && \
for db in histori histori_test histori_0 histori_1 histori_2 histori_3 ; do
  cat ${SCHEMA_SQL} ${INDEX_SQL} | psql -U histori ${db} || die "error initializing schema/indexes on ${db}"
done && \
echo "setup complete!" || die "error setting up histori"
