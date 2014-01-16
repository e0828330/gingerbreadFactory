#!/bin/sh
if [ "$#" -ne 2 ]; then
    echo "Please supply an ID and factory ID"
    exit
fi
java -cp ../gingerbreadFactory/target/gingerbreadFactory-0.0.1-SNAPSHOT.jar factory.jmsImpl.logistics.JMSLogistics $1 $2
