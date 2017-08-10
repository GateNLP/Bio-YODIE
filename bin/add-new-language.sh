#!/bin/bash
#
# add-new-language.sh
#
# Copyright (c) 1995-2016, The University of Sheffield. See the file
# COPYRIGHT.txt in the software or at
# http://gate.ac.uk/gate/COPYRIGHT.txt
#
# This file is part of Bio-YODIE (see 
# https://gate.ac.uk/applications/bio-yodie.html), and is free 
# software, licenced under the GNU Affero General Public License
# Version 3, 19 November 2007
#
# A copy of this licence is included in the distribution in the file
# LICENSE-AGPL3.html, and is also available at
# http://www.gnu.org/licenses/agpl-3.0-standalone.html
#
# G. Gorrell, 26 September 2016

# Add new directory structure to facilitate adding a new language.
# Single argument should be the two letter language code.

PRG="$0"
CURDIR="`pwd`"
## find the location of the script
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

if [[ $# -eq 0 ]]
then
  echo "One argument required--the two letter language code!"
  exit 1
fi

echo "Creating directory structure for language $1"

if [ -d main-bio-$1 ] || [ -d gazetteer-$1 ] || [ -d preprocess-$1 ] || [ -d lookupinfo-$1 ]
then
  echo "Language already exists! Clean up in order to use this script."
  exit 1
fi

echo "Copying the required directories .."

cp -r main-bio main-bio-$1
cp -r preprocess-en preprocess-$1
cp -r gazetteer-en gazetteer-$1
cp -r lookupinfo-en lookupinfo-$1

echo "Editing xgapps .."

sed -i "s/gazetteer-en/gazetteer-$1/g" main-bio-$1/main-bio.xgapp
sed -i "s/gazetteer-en/gazetteer-$1/g" gazetteer-$1/gazetteer.xgapp
sed -i "s/bio-yodie-resources\/en/bio-yodie-resources\/$1/g" gazetteer-$1/gazetteer.xgapp

sed -i "s/preprocess-en/preprocess-$1/g" main-bio-$1/main-bio.xgapp
sed -i "s/preprocess-en/preprocess-$1/g" preprocess-$1/preprocess.xgapp
sed -i "s/bio-yodie-resources\/en/bio-yodie-resources\/$1/g" preprocess-$1/preprocess.xgapp

sed -i "s/lookupinfo-en/lookupinfo-$1/g" main-bio-$1/main-bio.xgapp
sed -i "s/lookupinfo-en/lookupinfo-$1/g" lookupinfo-$1/lookupinfo.xgapp
sed -i "s/bio-yodie-resources\/en/bio-yodie-resources\/$1/g" lookupinfo-$1/lookupinfo.xgapp

echo "Editing config .."

sed -i "s/preprocess-en/preprocess-$1/g" main-bio-$1/main-bio.config.yaml
sed -i "s/gazetteer-en/gazetteer-$1/g" main-bio-$1/main-bio.config.yaml
sed -i "s/lookupinfo-en/lookupinfo-$1/g" main-bio-$1/main-bio.config.yaml
sed -i "s|bio-yodie-resources/en|bio-yodie-resources/$1|g" main-bio-$1/main-bio.config.yaml

echo "New language structure created for $1."
echo "preprocess-$1 should now be updated to include language appropriate language processing resources."
echo "gazetteer-$1 should now be updated to include language appropriate stopword list."
echo "Ensure language appropriate resources are available in or linked to bio-yodie-resources directory."

