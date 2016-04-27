#!/bin/bash

function die {
  echo 1>&2 "${1}"
  exit 1
}

if [[ $(whoami) != "root" ]] ; then
  die "Must run as root"
fi

function download {
  dest=${1}
  url=${2}
  if [ ! -f ${dest} ] ; then
    curl ${url} > ${dest} || die "Error downloading ${url} -> ${dest}"
    chown ${CHEF_USER} ${dest} || die "Error running chown ${CHEF_USER} ${dest}"
  fi
}

CHEF_USER=$(cat /etc/chef-user)
if [ -z ${CHEF_USER} ] ; then
  die "No chef user found in /etc/chef-user"
fi
CHEF_USER_HOME=$(bash -c "cd ~${CHEF_USER} && pwd")

JAR="${CHEF_USER_HOME}/chef/cookbooks/histori/files/default/assets/histori-server.jar"
JAR_URL="http://kyuss.org/downloads/histori-server.jar"

SITE="${CHEF_USER_HOME}/chef/cookbooks/histori/files/default/assets/histori-site.jar"
SITE_URL="http://kyuss.org/downloads/histori-site.jar"

download ${JAR} ${JAR_URL}
download ${SITE} ${SITE_URL}
