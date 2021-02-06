#!/bin/bash
mvn package 
java -cp target/gframework-1.0-SNAPSHOT.jar:./antlr-4.8-complete.jar gframework.App