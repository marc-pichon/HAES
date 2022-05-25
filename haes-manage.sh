#!/bin/bash

HAES_HOME=/opt/appdyn/HAES
HAES_LOGS=/logs/appdyn/HAES
ES_HOME=/opt/appdyn/platform/product/events-service/processor

function snapshotStatus() {
  cd ${ES_HOME}; ./bin/events-service.sh snapshot-status -p conf/events-service-api-store.properties
}

function restoreStatus() {
  cd ${ES_HOME}; ./bin/events-service.sh snapshot-restore-status -p conf/events-service-api-store.properties
}

function listSnapshots() {
  cd ${ES_HOME}; ./bin/events-service.sh snapshot-list -p conf/events-service-api-store.properties
}

function checkPatternInLogs() {
  cd ${HAES_HOME}; for url in `cat haes.properties | awk -F= '/esnode\..+ip/ {ip=$2} /esnode\..+port/ {print ip}'`; do ssh -o LogLevel=error $url "echo Checking pattern $1 on "$url"; grep "$1 ${HAES_LOGS}"/haes.log; echo --------------------------"; done
}

function resetZkDiskData() {
  cd ${HAES_HOME}; for url in `cat haes.properties | awk -F= '/esnode\..+ip/ {ip=$2} /esnode\..+port/ {print ip}'`; do ssh -o LogLevel=error $url "echo Cleaning zookeeper data on local disk for "$url"; rm -rf /logs/appdyn/zookeper/version-2; rm -rf /data/appdyn/zookeper/data/version-2"; done
}

function checkZkPorts() {
  cd ${HAES_HOME}; for url in `cat haes.properties | awk -F= '/esnode\..+ip/ {ip=$2} /esnode\..+port/ {print ip}'`; do ssh -o LogLevel=error $url "echo Checking Zookeeper ports for "$url"; netstat -anp | egrep \(3888\|2181\); echo --------------------------"; done
}

function checkLocalCluster() {
  curl -s -X GET http://127.0.0.1:9200/_cat/health?v
}

function checkZkContent() {
  curl -s -X GET http://localhost:8081/HAES/getZKTree | sed 's/<br>/"\n"/g' | awk 'BEGIN {s=0;} /^"/ {s++; if (s>1) s=0; if (s==0) {print p " => " $0;} else {p=$0}}'
}

function deleteFromHH() {
  cd ${HAES_HOME}; for url in `cat haes.properties | awk -F= '/esnode\..+ip/ {ip=$2} /esnode\..+port/ {print ip}'`; do ssh -o LogLevel=error $url "rm "${HAES_HOME}"/"$1; done
}

function deployToHH() {
  cd ${HAES_HOME}; for url in `cat haes.properties | awk -F= '/esnode\..+ip/ {ip=$2} /esnode\..+port/ {print ip}'`; do scp -o LogLevel=error ${HAES_HOME}/$1 $url:${HAES_HOME}; done
}

function checkF5() {
  cd ${HAES_HOME}; for url in `cat haes.properties | awk -F= '/esnode\..+ip/ {ip=$2} /esnode\..+port/ {print "http://" ip ":8081"}'`; do curl -s -X GET $url/EScluster/Node/Status; echo ; done
}

function checkAll() {
  cd ${HAES_HOME}; for url in `cat haes.properties | awk -F= '/esnode\..+ip/ {ip=$2} /esnode\..+port/ {print ip}'`; do ssh -o LogLevel=error $url "ps -aef | grep appdynamics.haes | grep -v grep | wc -l | awk '/1/ {print \""$url"\" \": HAES is running\"} /0/ {print \""$url"\" \": HAES is not running\"}'"; done
}

function stopAll() {
  cd ${HAES_HOME}; for url in `cat haes.properties | awk -F= '/esnode\..+ip/ {ip=$2} /esnode\..+port/ {print ip}'`; do ssh -o LogLevel=error $url "kill -9 \`ps -aef | grep appdynamics.haes | grep -v grep | awk '{print \$2}'\`"; done
}

function startAll() {
  cd ${HAES_HOME}; for url in `cat haes.properties | awk -F= '/esnode\..+ip/ {ip=$2} /esnode\..+port/ {print ip}'`; do ssh -o LogLevel=error $url 'cd '${HAES_HOME}'; ' ${HAES_HOME}'/haes.sh < /dev/null >/dev/null 2>&1'; done
}

function usage() {
  echo -e "haes-manage.sh <command> [<parameters>]"
  echo -e ""  
  echo -e "commands :" 
  echo -e "  startAll                     : démarre tous les noeuds HAES spécifié dans la configuration HAES"
  echo -e "  stopAll                      : arrête tous les noeuds HAES"
  echo -e "  checkAll                     : verifie l'état de tous les noeuds HAES"
  echo -e "  checkF5                      : verifie la réponse au check des F5 de tous les noeuds HAES"
  echo -e "  deployToHH <file>            : déploie un fichier local dans HAES_HOME sur tous les noeuds HAES"
  echo -e "  deleteFromHH <file>          : supprime un fichier dans HAES_HOME sur tous les noeuds HAES"
  echo -e "  checkZkContent               : affiche le contenu courant du cluster zookeeper HAES"
  echo -e "  checkLocalCluster            : affiche l'état du cluster \"Events Service\" local"
  echo -e "  checkZkPorts                 : affiche l'état  des ports Zookeeper"
  echo -e "  checkPatternInLogs <pattern> : Recherche un pattern dans les logs de tous les noeuds HAES"
  echo -e "  listSnapshots                : affiche la liste des snapshots"
  echo -e "  snapshotStatus               : affiche l'état du dernier snapshot"
  echo -e "  restoreStatus                : affiche l'état de la derniere restauration"
  echo -e "  resetZkDiskData              : Supprime les données Zk sur disque (HAES doit être arrêté)"
}

PARAMS=""
COMMAND=$1

  case "$COMMAND" in
    startAll)
      startAll
      ;;
    stopAll)
      stopAll
      ;;
    checkAll)
      checkAll
      ;;
    checkF5)
      checkF5
      ;;
    deployToHH)
      deployToHH $2
      ;;
    deleteFromHH)
      deleteFromHH $2
      ;;
    checkZkContent)
      checkZkContent
      ;;
    checkLocalCluster)
      checkLocalCluster
      ;;
    checkPatternInLogs)
      checkPatternInLogs $2
      ;;
    checkZkPorts)
      checkZkPorts
      ;;
    resetZkDiskData)
      resetZkDiskData
      ;;
    listSnapshots)
      listSnapshots
      ;;
    snapshotStatus)
      snapshotStatus
      ;;
    restoreStatus)
      restoreStatus
      ;;
    *)
      usage
      ;;
  esac
