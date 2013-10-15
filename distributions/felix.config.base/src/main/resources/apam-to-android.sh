#!/bin/bash
#this script transform apam distribution into one compatible with android
# Steps before executing this script
# Android SDK Configuration
# 1°  Install Android SDK
# 2° With Android SDK Manager, install Android 4.2.2 (17.0.0)
# 3° Create an AVD (android) for Android 4.2.2 or use a real device (developer mode enabled, USB debugging enabled, root access)
# 4° Start the Emulator (emulator avd myEmulator)


# 6° Modify the ANDROIDPATH in this script accordingly to your Android SDK installation
# 7° Modify the ANDROIDADBDEVICE in this script accordingly to your devices (adb devices)
# 8° move this script on the directory ApAM/distributions/basic-distribution
# 9° Execute using sh apam-to-android.sh

# script to start felix : /data/felix/felix-android.sh



ANDROIDPATH=/home/thibaud/AndroidSDK/adt-bundle-linux-x86-20130729
ANDROIDVERSION=17.0.0

ANDROIDFELIXPATH=/data/felix

ANDROIDADBDEVICE=084972d8


export PATH=$PATH:$ANDROIDPATH/sdk/build-tools/$ANDROIDVERSION:$ANDROIDPATH/sdk/tools:$ANDROIDPATH/sdk/platform-tools

echo
echo XXXX
echo "Current PATH=$PATH"
echo XXXX



echo
echo XXXX
echo Creating temporary android-apam files and directories
echo XXXX

rm -rf android-apam

mkdir android-apam
mkdir android-apam/bin
mkdir android-apam/bundle
mkdir android-apam/conf
mkdir android-apam/tmp

echo
echo XXXX
echo Creating dex file for felix.jar
echo XXXX
cp bin/felix.jar android-apam/tmp/felix.jar
cd android-apam/tmp
dx --dex --output=classes.dex felix.jar
aapt add felix.jar classes.dex
mv felix.jar ../bin/felix.jar
cd ..
cd ..
rm android-apam/tmp/*

echo
echo XXXX
echo Creating dex files for the other bundles
echo XXXX
cp bundle/* android-apam/tmp/
cd android-apam/tmp
rm felix-gogo-*
rm apam-universal-shell-*

for jarfile in *.jar; do
	dx --dex --output=classes.dex $jarfile
	aapt add $jarfile classes.dex
	mv $jarfile ../bundle/$jarfile
done
cd ..
rm tmp/*
cd ..

echo
echo XXXX
echo Creating script felix-android.sh
echo XXXX
echo "/system/bin/dalvikvm -classpath bin/felix.jar org.apache.felix.main.Main" > android-apam/felix-android.sh


echo
echo XXXX
echo Setting configuration files
echo You may add dalvik packages to \"org.osgi.framework.system.packages.extra\"
echo (depending on your own bundle needs)
echo XXXX
cp conf/* android-apam/conf/
echo updating felix configuration file
#~ echo "org.osgi.service.http.port=8080" >> android-apam/conf/config.properties
echo "felix.fileinstall.tmpdir="$ANDROIDFELIXPATH"/tmp" >> android-apam/conf/config.properties
echo "ipojo.proxy=disabled" >> android-apam/conf/config.properties
echo "felix.bootdelegation.implicit=false" >> android-apam/conf/config.properties
echo "obr.repository.url=http://felix.apache.org/obr/releases.xml" >> android-apam/conf/config.properties

echo " org.osgi.framework.system.packages.extra= \\" >> android-apam/conf/config.properties
echo " javax.xml.parsers; \\" >> android-apam/conf/config.properties
echo " org.xml.sax; \\" >> android-apam/conf/config.properties
echo " org.xml.sax.ext; \\" >> android-apam/conf/config.properties
echo " org.xml.sax.helpers; \\" >> android-apam/conf/config.properties
echo " javax.net.ssl; \\" >> android-apam/conf/config.properties
echo " javax.security.auth" >> android-apam/conf/config.properties


echo
echo XXXX
echo Setting up Android terminal
echo These might not work on a real terminal, you should do the following commands as root on the device
echo XXXX

echo "cd android-apam"
echo "adb -s $ANDROIDADBDEVICE shell rm -rf $ANDROIDFELIXPATH"
echo "adb -s $ANDROIDADBDEVICE shell mkdir $ANDROIDFELIXPATH/tmp"
echo "adb -s $ANDROIDADBDEVICE shell chmod 700 /data/felix"
echo "find * -type f -exec adb push -s $ANDROIDADBDEVICE {} $ANDROIDFELIXPATH/{} \;"
echo "cd .."

cd android-apam
adb -s $ANDROIDADBDEVICE shell rm -rf $ANDROIDFELIXPATH
adb -s $ANDROIDADBDEVICE shell mkdir $ANDROIDFELIXPATH/tmp
adb -s $ANDROIDADBDEVICE shell chmod 700 /data/felix
find * -type f -exec adb push -s $ANDROIDADBDEVICE {} $ANDROIDFELIXPATH/{} \;
cd ..

echo
echo XXXX
echo Android terminal ready
echo XXXX
