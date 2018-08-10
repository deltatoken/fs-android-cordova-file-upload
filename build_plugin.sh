#!/usr/bin/env bash
rm -Rf cordova-plugin/src/android
mkdir cordova-plugin/src/android
mkdir cordova-plugin/src/android/res
cp plugin-source/src/main/java/com/friendlysol/fsupload/* cordova-plugin/src/android/
cp plugin-source/src/main/res/values/* cordova-plugin/src/android/res