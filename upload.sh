#!/bin/sh
lein cljsbuild clean
rm -rf www/*
lein cljsbuild once
cp -rf resources/public/css www/
cp -rf resources/public/img www/
cp -rf resources/public/sound www/
rm www/img/generate.sh
cp resources/public/index.html www/
cat www/index.html | sed 's/js\/compiled\/rockets\.js/rockets.min.js/g' | sed 's/<script src="js\/compiled\/out\/goog\/base.js" type="text\/javascript"><\/script>//g' | sed 's/<script type="text\/javascript">goog.require("rockets.core");<\/script>//g' > www/index_new.html
mv -f www/index_new.html www/index.html

tar -c --bzip2 -f www.tar www/
scp www.tar cloudsigma@178.22.70.63:~/
rm www.tar

ssh cloudsigma@178.22.70.63 <<'ENDSSH'
rm -rf www
tar -xvf www.tar
rm -rf app/*
cp -rf www/* app/
ENDSSH
