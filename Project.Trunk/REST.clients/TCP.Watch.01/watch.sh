#!/usr/bin/env bash
CP=./build/libs/TCP.Watch.01-1.0-all.jar
#
BASE_URL="-Dbase.url=http://192.168.42.5:8080"
java -cp $CP $BASE_URL nmea.tcp.TCPWatch
