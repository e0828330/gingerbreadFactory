===== Kompilieren =====
cd gingerbreadFactory
mvn assembly\:assembly package

===== JMS ONLY - Broker Starten =====
sh qpid-broker-0.24/bin/qpid-server

Im Verzeichnis startup befinden sich die startup Skripte,
diese müssen (wie unten beschrieben) von dort aus aufgerufen werden.

===== Server starten ===== 
JMS:	sh start_jms_server.sh
MZS:	sh start_spaces_server.sh

===== GUI starten ===== 
JMS:	sh start_jms_gui.sh
MZS:	sh start_spaces_gui.sh

===== Bäcker starten ===== 
JMS:	sh start_jms_baker.sh <ID> (e.g. sh start_jms_baker.sh 8765)
MZS:	sh start_spaces_baker.sh <ID> (e.g. sh start_spaces_baker.sh 8765)

===== Qualitätskontrolle starten===== 

JMS:	sh start_jms_qa <ID> <DEFECTRATE> (e.g. sh start_jms_qa 129 0.3)
MZS:	sh start_spaces_qa <ID> <DEFECTRATE> (e.g. sh start_spaces_qa 129 0.3)

===== Logistiker starten ===== 

JMS:	sh start_jms_logistics.sh <ID> (e.g. sh start_jms_logistics.sh 777)
MZS:	sh start_spaces_logistics.sh <ID> (e.g. sh start_spaces_logistics.sh 777)
