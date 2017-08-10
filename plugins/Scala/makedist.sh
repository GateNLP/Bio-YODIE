#!/bin/bash

name=Scala
tmpdir=/tmp
curdir=`pwd -P`
version=`perl -n -e 'if (/VERSION="([^"]+)"/) { print $1;}' < $curdir/creole.xml`
destdir=$tmpdir/${name}$$

rm -rf "$destdir"
mkdir -p $destdir
git clone . $destdir/$name
rm -rf $destdir/$name/.git
cp build.properties $destdir/$name
rm -f $name-*.zip
rm -f $name-*.tgz
pushd $destdir/$name
ant || exit
ant clean.classes || exit
rm -rf build.properties
rm -rf makedist.sh
rm -rf test
cd ..
## find $name -name '.svn' | xargs -n 1 rm -rf
zip -r $curdir/$name-$version.zip $name
# cp $curdir/$name-$version.zip $curdir/creole.zip
# tar zcvf $curdir/$name-$version.tgz $name
# cp $curdir/creole.zip $name
# mv $name $name-$version
# tar zcvf $curdir/$name-$version-updatesite.tgz $name-$version
popd
