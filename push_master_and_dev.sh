#!/bin/sh

BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ "${BRANCH}" != "master" ]]; then
  echo "Current branch: ${BRANCH}"
  echo "Aborting script because you are not on the master branch."
  exit 1
fi

echo "=> git status"
git status
echo "-----------------------------------------------------"

echo "=> git stash"
git stash
echo "-----------------------------------------------------"

echo "=> git pull origin master -r"
git pull origin master -r
echo "-----------------------------------------------------"

echo "=> git push origin master:master"
git push origin master:master
echo "-----------------------------------------------------"

echo "=> git checkout dev"
git checkout dev
echo "-----------------------------------------------------"

echo "=> git pull origin dev -r"
git pull origin dev -r
echo "-----------------------------------------------------"

echo "=> git merge master"
git merge master --commit --no-edit
echo "-----------------------------------------------------"

echo "=> git push origin dev:dev"
git push origin dev:dev
echo "-----------------------------------------------------"

echo "=> git checkout master"
git checkout master
echo "-----------------------------------------------------"

echo "=> git stash pop"
git stash pop
echo "-----------------------------------------------------"
