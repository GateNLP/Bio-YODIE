#!/bin/bash


## First fetch any remote updates ...
git fetch --recurse-submodules=on-demand

## we still need to update the submodules, if necessary
git submodule update --init --recursive


./plugins/compilePlugins.sh | tee compilePlugins.log | grep 'Build of plugin' 
