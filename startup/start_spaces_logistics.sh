#!/bin/sh
if [ "$#" -ne 2 ]; then
    echo "Please supply an ID and defect Rate"
fi
java -cp ../gingerbreadFactory/target/gingerbreadFactory-0.0.1-SNAPSHOT-jar-with-dependencies.jar factory.spacesImpl.LogisticsEmployee $1 $2
