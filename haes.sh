#/bin/bash
#rm ./nohup.out
#java  -Dserver.port=8081 -Dzk.url=127.0.0.1:2181  -jar ./appdynamics.haes.beta-1.0-SNAPSHOT.jar --spring.profiles.active=dev --spring.config.location=file:////opt/appdyn/HAES/application-dev.properties
#java  -Xmx200m -Xms50m -Dserver.port=8081 -Dzk.url=127.0.0.1:2181 -Dzk.conf=/opt/appdyn/HAES/zoo.cfg -Djdk.tls.rejectClientInitiatedRenegotiation=true -jar ./appdynamics.haes.beta-embedded-zk-1.0-SNAPSHOT.jar --spring.profiles.active=dev --spring.config.location=file:////opt/appdyn/HAES/application-dev.properties 

#export ZOO_LOG4J_PROP="DEBUG,CONSOLE,ROLLINGFILE"
#export ZOO_LOG_DIR="/opt/appdyn/HAES/logs" 
#-Djava.net.preferIPv4Stack=true
#java  -Xmx200m -Xms100m -Dserver.port=8081 -Dzk.url=127.0.0.1:2181 -Dzk.conf=/opt/appdyn/HAES/zoo.cfg -Dzookeeper.log.threshold=DEBUG -Dzookeeper.console.threshold=DEBUG -Djdk.tls.rejectClientInitiatedRenegotiation=true -jar ./appdynamics.haes.beta-embedded-zk-1.0-SNAPSHOT.jar --spring.profiles.active=dev --spring.config.location=file:////opt/appdyn/HAES/application-dev.properties --debug
#java  -Xmx150m -Xms100m -Dserver.port=8081 -Dzk.url=127.0.0.1:2181 -Dzk.conf=/opt/appdyn/HAES/zoo.cfg -Dzookeeper.4lw.commands.whitelist=* -Djdk.tls.rejectClientInitiatedRenegotiation=true -jar ./appdynamics.haes.beta-embedded-zk-1.0-SNAPSHOT.jar --spring.profiles.active=dev --spring.config.location=file:////opt/appdyn/HAES/application-dev.properties

#java  -Xmx150m -Xms100m -Dserver.port=8081 -Dzk.url=127.0.0.1:2181 -Dzk.conf=/opt/appdyn/HAES/zoo.cfg -Dzookeeper.4lw.commands.whitelist=* -Dlogging.config=/opt/appdyn/HAES/logback.xml -Djdk.tls.rejectClientInitiatedRenegotiation=true -jar ./appdynamics.haes.embedded-zk-1.0-SNAPSHOT.jar --spring.profiles.active=dev --spring.config.location=file:////opt/appdyn/HAES/application-dev.properties

nohup java  -Xmx150m -Xms100m -Dserver.port=8081 -Dzk.url=127.0.0.1:2181 -Dzk.conf=/opt/appdyn/HAES/zoo.cfg -Dzookeeper.admin.enableServer=false -Dzookeeper.4lw.commands.whitelist=* -Dlogging.config=/opt/appdyn/HAES/logback.xml -Djdk.tls.rejectClientInitiatedRenegotiation=true -jar ./appdynamics.haes.embedded-zk-1.0-SNAPSHOT.jar --spring.profiles.active=dev --spring.config.location=file:////opt/appdyn/HAES/haes.properties &
 
 
