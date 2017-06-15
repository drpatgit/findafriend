#!/bin/bash -x
set -x 
protoc -I=. --java_out=../../../src/main/java/ mymqttmessages.proto
