#!/bin/bash

if [ "${GATE_HOME}" == "" ]
then
  echo Environment variable GATE_HOME not set
  exit 1
fi

PRG="$0"
CURDIR="`pwd`"
# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
SCRIPTDIR=`dirname "$PRG"`
ROOTDIR=`cd "$SCRIPTDIR/.."; pwd -P`

pluginDir="$ROOTDIR"

java -cp "$ROOTDIR"/'lib/*':"$ROOTDIR"/StringAnnotation.jar:"${GATE_HOME}/bin/gate.jar":"${GATE_HOME}/"'lib/*' -DpluginDir="$pluginDir" -Dgate.home="$GATE_HOME" com.jpetrak.gate.stringannotation.extendedgazetteer.GenerateCache "$@"

