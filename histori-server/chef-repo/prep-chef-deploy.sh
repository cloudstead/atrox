#!/bin/bash

function die {
  echo 1>&2 "${1}"
  exit 1
}

BASE=$(cd $(dirname $0) && pwd)
HISTORI_SERVER_DIR=$(cd ${BASE}/.. && pwd)
ASSETS_DIR="${HISTORI_SERVER_DIR}/chef-repo/cookbooks/histori/files/default/assets"
mkdir -p ${ASSETS_DIR}

# Server API jar file
JAR_FILE=$(find ${HISTORI_SERVER_DIR}/target -maxdepth 1 -type f -name "histori-server*.jar")

MVN="mvn -DskipTests=true -Dcheckstyle.skip=true"
if [ ! -f ${JAR_FILE} ] ; then
  cd ${HISTORI_SERVER_DIR} && ${MVN} package || die "Error building server jar"
  JAR_FILE=$(find ${HISTORI_SERVER_DIR}/target -maxdepth 1 -type f -name "histori-server*.jar")
  if [ ! -f ${JAR_FILE} ] ; then
    die "no histori-server.jar file found"
  fi
fi
cp ${JAR_FILE} ${ASSETS_DIR}/histori-server.jar

# Database schema and indexes
SCHEMA_SQL=${HISTORI_SERVER_DIR}/src/main/resources/seed/schema.sql
INDEX_SQL=${HISTORI_SERVER_DIR}/src/main/resources/seed/index.sql
if [ ! -f ${SCHEMA_SQL} ] ; then
  cd ${HISTORI_SERVER_DIR} && ./gen-sql.sh -v || die "Error generating schema.sql file"
fi
cp ${SCHEMA_SQL} ${INDEX_SQL} ${ASSETS_DIR}

# Email templates, site static content, run script
cd ${HISTORI_SERVER_DIR} && tar czf ${ASSETS_DIR}/histori-email-templates.tar.gz email
cd ${HISTORI_SERVER_DIR}/src/main/resources && tar czf ${ASSETS_DIR}/histori-site.tar.gz site
cp ${HISTORI_SERVER_DIR}/run.sh ${ASSETS_DIR}

# Legal stuff
cd ${HISTORI_SERVER_DIR}/.. && tar czf ${ASSETS_DIR}/legal.tar.gz legal

# Other scripts
mkdir -p ${ASSETS_DIR}/scripts
cp ${HISTORI_SERVER_DIR}/scripts/* ${ASSETS_DIR}/scripts
