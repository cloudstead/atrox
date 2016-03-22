#!/bin/bash

function die {
  echo 1>&2 "${1}"
  exit 1
}

BASE=$(cd $(dirname $0) && pwd)

if [ -f ~/.histori.env ] ; then
  . ~/.histori.env
fi

debug="${1}"
if [ "x${debug}" = "xdebug" ] ; then
  shift
  ARG_LEN=$(echo -n "${1}" | wc -c)
  ARG_NUMERIC_LEN=$(echo -n "${1}" | tr -dc [:digit:] | wc -c)  # strip all non-digits
  if [ ${ARG_LEN} -eq ${ARG_NUMERIC_LEN} ] ; then
    # Second arg is the debug port
    DEBUG_PORT="${1}"
    shift
  fi
  if [ -z "${DEBUG_PORT}" ] ; then
    DEBUG_PORT=6005
  fi
  debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=${DEBUG_PORT}"
else
  debug=""
fi

command="${1}"
if [ -z "${command}" ] ; then
  CLASS=histori.server.HistoriServer
else
  CLASS=histori.main.HistoriMain
  shift
fi

# In a production environment, the jar may be lacking a version number
JAR="${BASE}/target/histori-server-1.0.0-SNAPSHOT.jar"
if [ ! -f ${JAR} ] ; then
  JAR=$(find ${BASE}/target -type f -name "histori-server*.jar")
  if [ -z "${JAR}" ] ; then
    die "No histori jar found in ${BASE}/target"
  elif [ $(echo -n "${JAR}" | wc -l | tr -d ' ') -gt 1 ] ; then
    die "Multiple histori jars found: ${JAR}"
  fi
fi

java ${debug} -Xmx1900m -Xms1900m -Djava.net.preferIPv4Stack=true -server -cp ${JAR} ${CLASS} ${command} "${@}"
