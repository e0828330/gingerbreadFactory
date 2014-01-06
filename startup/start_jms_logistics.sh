#!/bin/sh
if [ "$#" -ne 1 ]; then
    echo "Please supply an ID"
    exit
fi
java -cp ../gingerbreadFactory/target/gingerbreadFactory-0.0.1-SNAPSHOT.jar factory.jmsImpl.logistics.JMSLogistics $1
