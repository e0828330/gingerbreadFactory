===== Kompilieren =====
cd gingerbreadFactory
mvn assembly\:assembly package

===== JMS ONLY =====
sh <PATH>/qpid-broker-0.24/bin/qpid-server

===== Server starten ===== 
JMS:	sh start_jms_server.sh
MZS:		sh start_spaces_server.sh

===== GUI starten ===== 
JMS:	sh start_jms_gui.sh
MZS:	sh start_spaces_gui.sh

===== Bäcker starten ===== 
JMS:	sh start_jms_baker.sh <ID> (e.g. sh start_jms_baker.sh 8765)
MZS:	sh start_spaces_baker.sh <ID> (e.g. sh start_spaces_baker.sh 8765)

===== Qualitätskontrolle starten===== 

JMS:	sh start_jms_qa <ID> (e.g. sh start_jms_qa 129)
MZS:	sh start_spaces_qa <ID> (e.g. sh start_spaces_qa 129)

===== Logistiker ===== 

JMS:	sh start_jms_logistics.sh <ID> (e.g. sh start_jms_logistics.sh 777)
MZS:	sh start_spaces_logistics.sh <ID> (e.g. sh start_spaces_logistics.sh 777)
