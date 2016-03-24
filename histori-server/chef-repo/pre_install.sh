#!/bin/bash

function die {
  echo 1>&2 "${1}"
  exit 1
}

if [[ $(whoami) != "root" ]] ; then
  die "Must run as root"
fi

CHEF_USER=$(cat /etc/chef-user)
if [ -z ${CHEF_USER} ] ; then
  die "No chef user found in /etc/chef-user"
fi

JAR="cookbooks/histori/files/default/assets/histori-server.jar"
JAR_URL="http://kyuss.org/downloads/histori-server.jar"

if [ ! -f ${JAR} ] ; then
  curl ${JAR_URL} > ${JAR} || die "Error downloading ${JAR_URL} -> ${JAR}"
  chown ${CHEF_USER} ${JAR} || die "Error running chown ${CHEF_USER} ${JAR}"
fi
