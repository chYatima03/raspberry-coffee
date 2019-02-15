#!/usr/bin/env bash
CP=./build/libs/TCP.Watch.01-1.0-all.jar
#
# This is the address and port of the logger
BASE_URL="-Dbase.url=http://192.168.42.8:9999"
sudo java -cp $CP $BASE_URL nmea.tcp.TCPWatch
