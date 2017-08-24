#!/bin/bash
trap "exit" INT

compile=1
if [ "$1" == "clean" ]
then
  compile=0
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
SCRIPTDIR=`cd "$SCRIPTDIR"; pwd -P`
pushd "$SCRIPTDIR"
tmpout=/tmp/`whoami`-compilePlugins$$.out
for file in gateplugin-Java gateplugin-JdbcLookup gateplugin-ModularPipelines gateplugin-StringAnnotation gateplugin-VirtualCorpus YodiePlugin
do
  if [ "$file" == ANNIE ] || [ ! -d "$file" ]
  then
    echo skipping $file
  else 
    pushd "$file"
    if [[ -f build.xml ]]
    then
      echo Build of plugin $file started
      ant clean
      if [ "$compile" == 1 ]
      then 
        ant | tee $tmpout
        grep -q "BUILD SUCCESSFUL" $tmpout 
        if [ "$?" != 0 ]
        then
          echo ERROR Build of plugin $file failed, plugin compilation script aborted. Log is in $tmpout
          exit 1
        else 
          echo Build of plugin $file completed
          rm $tmpout
        fi
      fi
    else 
      echo No build.xml, skipping $file
    fi
    popd
  fi
done
popd
