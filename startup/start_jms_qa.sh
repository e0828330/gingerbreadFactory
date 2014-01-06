#!/bin/sh
if [ "$#" -ne 2 ]; then
    echo "Please supply an ID and defectRate."
    exit
fi
java -cp ../gingerbreadFactory/target/gingerbreadFactory-0.0.1-SNAPSHOT.jar factory.jmsImpl.qualityControl.JMSQualityControl $1 $2
