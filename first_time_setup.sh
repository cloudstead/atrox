#!/bin/bash

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}

git submodule init
git submodule update

pushd utils/cobbzilla-parent
mvn install
popd

pushd utils
mvn -DskipTests=true -Dcheckstyle.skip=true install
popd
