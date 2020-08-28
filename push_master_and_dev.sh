#!/bin/sh

echo "git status"
git status

echo "git stash"
git stash

echo "git pull origin master -r"
git pull origin master -r

echo "git push origin master:master"
git push origin master:master

echo "git checkout dev"
git checkout dev

echo "git merge master"
git merge master

echo "git push origin dev:dev"
git push origin dev:dev

echo "git checkout master"
git checkout master

echo "git stash pop"
git stash pop
