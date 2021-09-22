#!/bin/sh

echo 'git stash'
git stash

echo 'git pull -r'
git pull -r

echo 'git push'
git push --no-verify

echo 'git stash pop'
git stash pop

