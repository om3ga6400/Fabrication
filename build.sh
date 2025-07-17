#!/bin/bash -e
./build-features.sh
rm -rf build/libs
echo Building Fabrication...
./gradlew clean build -x ap:clean
rm -f build/libs/*-dev.jar
echo Done