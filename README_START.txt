===== Kompilieren =====
cd gingerbreadFactory
mvn package

===== JMS ONLY - Broker Starten =====
sh qpid-broker-0.24/bin/qpid-server

Im Verzeichnis startup befinden sich die startup Skripte,
diese müssen (wie unten beschrieben) von dort aus aufgerufen werden.

===== Server starten ===== 
JMS:	sh start_jms_server.sh <factoryID>
MZS:	sh start_spaces_server.sh -> gibt die FabriksID aus

===== GUI starten ===== 
JMS:	sh start_jms_gui.sh <factoryID>
MZS:	sh start_spaces_gui.sh <factoryID>

===== Bäcker starten ===== 
JMS:	sh start_jms_baker.sh <ID> <factoryID> (e.g. sh start_jms_baker.sh 8765 1234)
MZS:	sh start_spaces_baker.sh <ID> <factoryID> (e.g. sh start_spaces_baker.sh 8765 1234)

===== Qualitätskontrolle starten===== 

JMS:	sh start_jms_qa <ID> <DEFECTRATE> <factoryID> (e.g. sh start_jms_qa 129 0.3 1234)
MZS:	sh start_spaces_qa <ID> <DEFECTRATE> <factoryID> (e.g. sh start_spaces_qa 129 0.3 1234)

===== Logistiker starten ===== 

JMS:	sh start_jms_logistics.sh <ID> <factoryID> (e.g. sh start_jms_logistics.sh 777 1234)
MZS:	sh start_spaces_logistics.sh <ID> <factoryID> (e.g. sh start_spaces_logistics.sh 777 1234)

===== LoadBalancer starten ===== 

JMS:	sh start_jms_loadbalancer.sh <factoryID>, <factoryID2> (sh start_jms_loadbalancer.sh 1234)
MZS:	sh start_spaces_loadbalancer.sh <factoryID>, <factoryID2> (sh start_spaces_loadbalancer.sh 1234)
