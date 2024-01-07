#!/bin/bash
rm -f ../src/main/resources/templates/singlepage.html
mv ./dist/index.html ../src/main/resources/templates/singlepage.html
mv ./dist/assets/* ../src/main/resources/static/assets/
