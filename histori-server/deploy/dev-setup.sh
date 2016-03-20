#!/bin/bash

function die {
  echo 1>&2 "${1}"
  exit 1
}

if [ $(whoami) != "root" ] ; then
  die "Must run $0 as root user"
fi

# Ensure main env file exists
if [ ! -f ${HISTORI_ENV_FILE} ] ; then
  die "HISTORI_ENV_FILE not defined, or not a file: ${HISTORI_ENV_FILE}"
fi
. ${HISTORI_ENV_FILE}

# Main env file should define git ssh key
if [ ! -f ${HISTORI_GIT_SSH_KEY} ] ; then
  die "HISTORI_GIT_SSH_KEY not defined, or not a file: ${HISTORI_GIT_SSH_KEY}"
fi
if [ -z "${HISTORI_DB_PASS}" ] ; then
  die "HISTORI_DB_PASS not defined in env file"
fi

apt-get update
apt-get install -y git openjdk-7-jdk maven postgresql redis-server apache2 emacs24-nox screen

if id histori 2> /dev/null ; then useradd -m histori -d /home/histori ; fi

cp ${HISTORI_ENV_FILE} /home/histori/.histori.env
cp ${HISTORI_ENV_FILE} /home/histori/.histori-dev.env

KNOWN_HOSTS=/home/histori/.ssh/known_hosts
mkdir -p $(dirname ${KNOWN_HOSTS})

echo '|1|uAIN6yn0SWzjZa0MRW/0R0MbSX8=|kOc4rMClJTQ86a/gKFirSfVfCKc= ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYs\
dlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gb\
ODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ==
|1|l+rw4BBYCwLvE5kNJGSnWi4L1z8=|pTjoLsUXOsqysIJU0WFmwgN/1xE= ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJiz\
Hhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTW\
lg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ==' >> ${KNOWN_HOSTS}
chmod 644 ${KNOWN_HOSTS}

SSH_KEY_FILE=/home/histori/.ssh/id_dsa
cat ${HISTORI_GIT_SSH_KEY} > ${SSH_KEY_FILE} && chmod 600 ${SSH_KEY_FILE}

chown -R histori /home/histori

sudo -u postgres createuser histori
echo "ALTER USER histori PASSWORD '"${HISTORI_DB_PASS}"'" | sudo -u postgres psql -U postgres

for DB in histori histori_test histori_0 histori_1 histori_2 histori_3 ; do
  sudo -u postgres createdb ${DB}
  echo "GRANT ALL ON ALL TABLES IN SCHEMA public TO histori" | sudo -u postgres psql -U postgres ${DB}
done
