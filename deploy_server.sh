#!/bin/sh
tar -c --bzip2 -f www.tar www/
scp www.tar cloudsigma@178.22.70.63:~/
rm www.tar

ssh cloudsigma@178.22.70.63 <<'ENDSSH'
rm -rf www
tar -xvf www.tar
rm -rf app/*
cp -rf www/* app/
ENDSSH
