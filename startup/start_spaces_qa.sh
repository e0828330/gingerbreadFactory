#!/bin/sh
if [ "$#" -ne 3 ]; then
    echo "Please supply an ID and defectRate and factory ID"
    exit
fi
java -cp ../gingerbreadFactory/target/gingerbreadFactory-0.0.1-SNAPSHOT.jar factory.spacesImpl.QAEmployee $1 $2 $3
