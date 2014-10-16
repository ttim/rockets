#!/bin/sh
lein cljsbuild clean
rm -rf www/*
lein cljsbuild once
cp -rf resources/public/css www/
cp -rf resources/public/img www/
cp -rf resources/public/sound www/
rm www/img/generate.sh
cp resources/public/index.html www/
cp resources/public/CNAME www/
cp resources/public/about.html www/
cat www/index.html | sed 's/js\/compiled\/rockets\.js/rockets.min.js/g' | sed 's/<script src="js\/compiled\/out\/goog\/base.js" type="text\/javascript"><\/script>//g' | sed 's/<script type="text\/javascript">goog.require("rockets.core");<\/script>//g' > www/index_new.html
mv -f www/index_new.html www/index.html

