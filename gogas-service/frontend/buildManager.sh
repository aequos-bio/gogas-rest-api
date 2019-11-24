#!/bin/bash
rm -rf ../src/main/resources/templates/*
rm -rf ../src/main/resources/static/*
mkdir -p ../src/main/resources/templates
mkdir -p ../src/main/resources/static
mv ./build/index.html ../src/main/resources/templates/singlepage.html
mv ./build/* ../src/main/resources/static/
