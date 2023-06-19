#!/usr/bin/env bash
echo running app
$JAVA_HOME/bin/java -jar -Dspring.profiles.active=docker app.jar