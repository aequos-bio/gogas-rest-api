#!/bin/bash
#rm -rf ../src/main/resources/templates/*
#rm -rf ../src/main/resources/static/*
#mkdir -p ../src/main/resources/templates
#mkdir -p ../src/main/resources/static
rm -f ../src/main/resources/templates/singlepage.html
mv ./dist/index.html ../src/main/resources/templates/singlepage.html
mv ./dist/* ../src/main/resources/static/
