#!/bin/sh
if [ "$#" -ne 1 ]; then
    echo "Please supply an ID"
    exit
fi
java -cp ../gingerbreadFactory/target/gingerbreadFactory-0.0.1-SNAPSHOT-jar-with-dependencies.jar factory.jmsImpl.qualityControl.JMSQualityControl $1
