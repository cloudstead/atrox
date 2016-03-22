#!/bin/bash
#
# Usage: ./deploy.sh [host]
#
# Environment variables:
#
# INIT_FILES -- a directory containing files that are unique to each chef-run.
#               Default is a directory my_init_files in same directory as this script.
#
# Relies on a deploy_lib.sh being either in the same directory as this script,
# or in ../utils/cloudos-lib/chef-repo/ (the location if being run from a local git repo)
#

function die {
  echo 1>&2 "${1}"
  exit 1
}

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}
PROJECT_BASE=$(cd ${BASE}/../.. && pwd)
CHEF_COMMON_BASE=$(cd ${PROJECT_BASE}/utils/cloudos-lib/chef-repo && pwd)

DEPLOYER=${BASE}/deploy_lib.sh
if [ ! -x ${DEPLOYER} ] ; then
  DEPLOYER=${CHEF_COMMON_BASE}/deploy_lib.sh
  if [ ! -x ${DEPLOYER} ] ; then
    die "ERROR: deploy library not found or not executable: ${DEPLOYER}"
  fi
fi

host="${1:?no user@host specified}"

if [ -z ${INIT_FILES} ] ; then
  if [ -d "${BASE}/my_init_files" ] ; then
    INIT_FILES="${BASE}/my_init_files"
  else
    die "INIT_FILES is not defined in the environment."
  fi
fi
INIT_FILES=$(cd ${INIT_FILES} && pwd) # make into absolute path

REQUIRED=" \
data_bags/base/base.json \
data_bags/histori/init.json \
certs/base/ssl-https.key \
certs/base/ssl-https.pem \
"

COOKBOOK_SOURCES="${BASE}/cookbooks"

${DEPLOYER} ${host} ${INIT_FILES} "${REQUIRED}" "${COOKBOOK_SOURCES}"
