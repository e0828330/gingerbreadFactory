#!/bin/sh
if [ "$#" -ne 1 ]; then
    echo "Please supply an factory ID"
    exit
fi
java -Xmx2g -cp ../gingerbreadFactory/target/gingerbreadFactory-0.0.1-SNAPSHOT.jar factory.jmsImpl.server.JMSServer $1
