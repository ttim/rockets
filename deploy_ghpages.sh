#!/bin/sh
rm -rf tmp_git
git clone https://github.com/ttim/rockets.git tmp_git
cd tmp_git
version=`git log -1 --format='oneline'`
git checkout gh-pages
rm -rf ./*
cp -rf ../www/* ./
git add --all
git commit -m "upload $version"
git push origin gh-pages
cd ..
rm -rf tmp_git

