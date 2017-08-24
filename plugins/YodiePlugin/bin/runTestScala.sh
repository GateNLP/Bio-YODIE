#!/bin/bash

scala -cp ${GATE_HOME}/bin/gate.jar:${GATE_HOME}/lib/'*':LodiePlugin.jar "$@"
