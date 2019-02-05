#!/bin/bash
#
# Read a GPS
#
CP=./build/libs/Serial.IO-1.0-all.jar
CP=$CP:/usr/share/java/RXTXcomm.jar
echo Make sure the GPS is connected it through its USB cable.
#
SERIAL_PORT=/dev/ttyUSB0
# SERIAL_PORT=/dev/tty.Bluetooth-Incoming-Port
BAUD_RATE=4800
#
JAVA_OPTS="-Dserial.port=$SERIAL_PORT -Dbaud.rate=$BAUD_RATE"
#
DARWIN=`uname -a | grep Darwin`
if [ "$DARWIN" != "" ]
then
	echo Running on Mac
  JAVA_OPTIONS="$JAVA_OPTIONS -Djava.library.path=/Library/Java/Extensions"  # for Mac
else
	echo Assuming Linux/Raspberry Pi
  JAVA_OPTIONS="$JAVA_OPTIONS -Djava.library.path=/usr/lib/jni"              # RPi
fi
#
sudo java $JAVA_OPTS -cp $CP sample.GPSReader