#!/bin/bash

BASE_PORT="`perl -MSocket -le 'socket S, PF_INET, SOCK_STREAM,getprotobyname("tcp"); $port = 1080; ++$port until bind S, sockaddr_in($port,inet_aton("127.1")); print $port'`"
RMI_PORT=$[BASE_PORT+1]
JMX_PORT=$[BASE_PORT+2]
HTTP_PORT=$[BASE_PORT+3]

TMP="`mktemp -d -t qpid-XXXXXX`"
./qpid-server -prop qpid.amqp_port=$BASE_PORT -prop qpid.http_port=$HTTP_PORT -prop qpid.rmi_port=$RMI_PORT -prop qpid.jmx_port=$JMX_PORT -prop qpid.work_dir=$TMP

