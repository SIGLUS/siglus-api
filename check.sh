#!/bin/sh

./gradlew clean build -x :stockmanagement:build -x :requisition:build -x :fulfillment:build
