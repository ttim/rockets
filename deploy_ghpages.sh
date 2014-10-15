#!/bin/sh

version=`git log -1 --format='format:%h'`
message=`git log -1 --format='format:%s'`

published=`git branch -r --contains $version`

if [ -z $published ];
then
  echo "Push commit $version before uploading it. Please :)"
  exit 1
fi

echo "Going to upload $version ... Sit tight!"

rm -rf tmp_git
git clone https://github.com/ttim/rockets.git tmp_git
cd tmp_git

git checkout gh-pages
build=`cat build.number`
if [ -z $build ];
then
  build=0
fi

rm -rf ./*
cp -rf ../www/* ./
((build++))
echo $build > build.number

git add --all
git commit -m "upload $version $message"
git push origin gh-pages

git checkout master
date=`date`
major=0
git tag -a "v$major.$build" -m "upload build $build to github pages $date" $version
git push origin "v$major.$build"
cd ..
rm -rf tmp_git

