#!/bin/bash
# Affichage du nom su script
echo "script in use : $0" 
# Affichage du nombre de paramètres
echo "Nb of parameters :  $# " 
# Liste des paramètres (un seul argument)
#for param in "$*"
#do
# echo "Voici la liste des paramètres (un seul argument) : $param"
#done
# Liste des paramètres (un paramètre par argument)
echo "parameters list : " 
for param in "$@"
do
 echo -e "	parameter : $param" 
done
# Affichage du processus
echo "Shell PID of script execution : $$" 


event_service_location=
snapshot_repository_location=
user=
snapshotid=
action=
scriptdir=
rsynctargethostdns=


snapshot_executing=
restore_executing=
action_status=0

f () {
    errorCode=$? # save the exit code as the first thing done in the trap function
    echo "error $errorCode" 
    echo "the command executing at the time of the error was" 
    echo "$BASH_COMMAND" 
    echo "on line ${BASH_LINENO[0]}" 
    # do some error handling, cleanup, logging, notification
    # $BASH_COMMAND contains the command that was being executed at the time of the trap
    # ${BASH_LINENO[0]} contains the line number in the script of that command
    # exit the script or return to try again, etc.
    exit $errorCode  # or use some other value or do return instead
}
trap f ERR


function usage()
{
                echo "usage: $0 <options>" 
                echo "    -U       user executing task" 
                echo "    -a       action to perform: can be either failover, failback, snaphsot, fastrestore, restoreto, restore, rsync, mount" 
                echo "             fastrestore: will restore from the last available snapshot" 
                echo "             restoreuntill: will restore untill snapshot specified" 
                echo "             restore: will restore every available snapshot one by one ordered by date ascending" 
                echo "    -l       location of <HOME_EVENT_SERVICE>" 
                echo "    -r       clusters shared snapshot repository location" 
                echo "    -H       if action is rsync, provide the target host DNS name" 
                echo "    [ -s ]   if action to perform is restore, then specify here snapshot id to restore" 
                exit -1
}

function failover()
{
  echo "failover request executing..." 
  echo "failover requesting a restore on secondary cluster..." 
  # failover real action is to restore on the secondary cluster
  fastrestore
  if [ $? -eq 0 ]
  then
     echo "failover successfull" 
  else
     echo "Problem failed" 
  fi
}
function failback()
{
  echo "failback request executing..." 
  echo "failback requesting a restore on primary cluster..." 

  # failback real action is to restore on the primary cluster
  fastrestore

  if [ $? -eq 0 ]
  then
     echo "failback successfull" 
  else
     echo "failback failed" 
  fi
}
function snapshot()
{
  echo "snaphsot request begin executing..." 

  # cmde en arriere plan (dangereux ici: semble bloquer haes...)
  #sleep 100 &
  # Affichage du processus lancé en arrière-plan
  #echo "PID of last command executed in background : $!"
  if [[ ! -d $event_service_location ]]; then
    echo "checking event service home location: the directory does not exist. stoping execution" 
    exit 1
  fi
  cd $event_service_location
  snapshot_executing=$scriptdir/snapshot/snapshot.executing
  if [[ -e $snapshot_executing ]]; then
      echo "checking one snapshot still executing: the snapshot.executing still exists. stoping execution" 
      exit 1
  fi
  touch $snapshot_executing

  if [[ "$snapshot_repository_location" == %2F* ]]; then
  # S3 repository => removing oldest snapshots more than 30
    for str in `./bin/events-service.sh snapshot-list -p conf/events-service-api-store.properties | grep snapshot_ | awk '{print $3}' | sort -r | awk 'BEGIN {i=0} {i++; if (i>30) print $0;}'`; do
      echo "Removing" $str "..." `curl -s -X DELETE http://127.0.0.1:9200/_snapshot/$snapshot_repository_location/$str` 
    done
  else
  # FS repository => Rotating on first snapshot of sunday...
    if [ "`date +%u`" == "7" ]; then
      rotateFolderOfDay=$snapshot_repository_location/SNAPSHOT-BKP_`date +%m-%d-%Y`
      if [ ! -e $rotateFolderOfDay ]; then
        echo "starting snapshot folder rotation..." 
        lastSundayRotateFolder=`ls -al $snapshot_repository_location | grep SNAPSHOT-BKP | awk '{print $9}'`
        if [ "$lastSundayRotateFolder" != "" ]; then
          echo "removing old rotation : " $snapshot_repository_location/$lastSundayRotateFolder 
          rm -rf $snapshot_repository_location/$lastSundayRotateFolder;
        fi
        echo "moving current to " $rotateFolderOfDay 
        mkdir $rotateFolderOfDay
        mv $snapshot_repository_location/snap-* $rotateFolderOfDay
        mv $snapshot_repository_location/meta-* $rotateFolderOfDay
        mv $snapshot_repository_location/ind* $rotateFolderOfDay
      fi
    fi
  fi

  echo "starting snapshot..." 
  ./bin/events-service.sh snapshot-run -p conf/events-service-api-store.properties
  echo "checking snapshot status ..." 
  executing_snapshot="yes"
  while [ ${executing_snapshot} = "yes" ]; do
    sleep 60
    get_status_snapshot_ok=$(./bin/events-service.sh snapshot-status -p conf/events-service-api-store.properties | grep -i "Snapshot state is SUCCESS"  | grep -iv "Hibernate Validator" | wc -l)
    get_status_snapshot_started=$(./bin/events-service.sh snapshot-status -p conf/events-service-api-store.properties | grep -i "Snapshot state is STARTED"  | grep -iv "Hibernate Validator" | wc -l)
    get_status_snapshot_ko=$(./bin/events-service.sh snapshot-status -p conf/events-service-api-store.properties | egrep -iv "(Snapshot state is STARTED|Snapshot state is SUCCESS)" | grep -iv "Hibernate Validator" | wc -l)
    if [ ${get_status_snapshot_ok} = "1" ] ; then
      executing_snapshot="no"
    fi
    if [ ${get_status_snapshot_ko} = "1" ] ; then
      echo "Error taking snapshot. exiting" 
      rm -f $snapshot_executing
      false # returns 1 so it triggers the trap
      exit 1
    fi
  done
  cd $scriptdir
  # useless...
  # var_res=`ls -ltr $snapshot_repository_location | grep snap-snapshot_ | awk '{ print $NF }' | awk -F_ '{print $2}' | awk -F. '{print "snapshot_"$1}' |awk '/./{line=$0} END{print line}'`
  # simulation update_restore_state $1
  # update_snapshot_state $var_res  <<< No WAY to find function update_snapshot_state ???
  rm -f $snapshot_executing
  if [ $? -eq 0 ]
  then
     echo "snaphsot successfull" 
  else
     echo "snaphsot failed" 
  fi
}
function fastrestore()
{
  echo "fast restore executing..." 
  echo "execute request snapshot restore of the last available snapshot" 
  if [[ ! -d $event_service_location ]]; then
    echo "checking event service home location: the directory does not exist. stoping execution" 
    exit 1
  fi
#             incremental cluster restor

# it will restore the latest snapshot
  execute_restore_for_latest_snapshot
  if [ $? -eq 0 ]
  then
     echo "fastrestore successfull" 
  else
     echo "fastrestore failed" 
  fi
}
function restoreuntill()
{
  echo "restoreuntill executing..." 
  echo "execute request snapshot restore of all available snapshots untill the ID specified with option -s" 
  if [[ ! -d $event_service_location ]]; then
    echo "checking event service home location: the directory does not exist. stoping execution" 
    exit 1
  fi
  execute_restore_untill_snapshot
  if [ $? -eq 0 ]
  then
     echo "restoreuntill successfull" 
  else
     echo "restoreuntill failed" 
  fi
}
function restoreto()
{
  echo "restoreto executing..." 
  echo "execute request snapshot restore of snapshot to the ID specified with option -s" 
  if [[ ! -d $event_service_location ]]; then
    echo "checking event service home location: the directory does not exist. stoping execution" 
    exit 1
  fi
  execute_restore_for_id
  if [ $? -eq 0 ]
  then
     echo "restoreto successfull" 
  else
     echo "restoreto failed" 
  fi
}

function restore()
{
  echo "restore executing..." 
  echo "execute request snapshot restore incremental" 
  if [[ ! -d $event_service_location ]]; then
    echo "checking event service home location: the directory does not exist. stoping execution" 
    exit 1
  fi
#             incremental cluster restore
# it will restore all the snapshots from the restore_state ID to the last, in right order
  for s in "${snapshots_to_restore_list[@]}"
  do
    echo "will execute snapshot restore for $s" 
    execute_restore_for_id $s
  done
  if [ $? -eq 0 ]
  then
     echo "restore successfull" 
  else
     echo "restore failed" 
  fi
}
function execute_restore_for_latest_snapshot()
{
  if [[ ! -d $event_service_location ]]; then
    echo "checking event service home location: the directory does not exist. stoping execution" 
    exit 1
  fi

  restore_executing=$scriptdir/snapshot/restore.executing
  touch $restore_executing
  echo "execute snapshot restore (using default behavior of events-service.sh snapshot-restore, which restores the latest)..." 
  cd $event_service_location
  ./bin/events-service.sh snapshot-restore -p conf/events-service-api-store.properties
  echo "checking restore status ..." 
  executing_restore="yes"
  while [ ${executing_restore} = "yes" ]; do
    sleep 60
    get_status_restore=$(./bin/events-service.sh snapshot-restore-status -p conf/events-service-api-store.properties | grep -i "Restore is complete"  | wc -l)
    get_status_restore_ko=$(./bin/events-service.sh snapshot-restore-status -p conf/events-service-api-store.properties | timeout 60 egrep -iv "(Hibernate Validator|Restore is complete|Restore is still in process)"  | wc -l)
    echo "get_status_restore=$get_status_restore get_status_restore_ko=$get_status_restore_ko" 
    if [ ${get_status_restore} = "1" ] ; then
      executing_restore="no"
    fi
    if [ ${get_status_restore_ko} = "1" ] ; then
      echo "Error restoring snapshot. exiting" 
      rm -f $restore_executing
      false # returns 1 so it triggers the trap
      exit 1
    fi
  done
  rm -f $restore_executing

}
function execute_restore_untill_snapshot()
{
  echo "not yet implemeted. doing nothing." 
}


function execute_restore_for_id()
{
  echo "execute request snapshot restore for one ID" 

  if [[ ! -d $event_service_location ]]; then
    echo "checking event service home location: the directory does not exist. stoping execution" 
    exit 1
  fi

  restore_executing=$scriptdir/snapshot/restore.executing
  touch $restore_executing
  echo "execute snapshot restore for $snapshotid" 
  cd $event_service_location
  ./bin/events-service.sh snapshot-restore -p conf/events-service-api-store.properties -id $snapshotid
  echo "checking restore status ..." 
  executing_restore="yes"
  while [ ${executing_restore} = "yes" ]; do
    sleep 60
    get_status_restore=$(./bin/events-service.sh snapshot-restore-status -p conf/events-service-api-store.properties | grep -i "Restore is complete"  | grep -iv "Hibernate Validator" | wc -l)
    get_status_restore_ko=$(./bin/events-service.sh snapshot-restore-status -p conf/events-service-api-store.properties | egrep -iv "(Restore is complete|Restore is still in process)"  | grep -iv "Hibernate Validator" | wc -l)
    if [ ${get_status_restore} = "1" ] ; then
      executing_restore="no"
    fi
    if [ ${get_status_restore_ko} = "1" ] ; then
      echo "Error restoring snapshot. exiting" 
      rm -f $restore_executing
      false # returns 1 so it triggers the trap
      exit 1
    fi
  done
  rm -f $restore_executing

}
function get_ordered_list_of_snapshots_to_restore()
{
  # Comment du to not implemented function for S3
  # # cat ./snapshots.list | awk '{ if ( $1 > '$restore_state' ) { print } }' > ./snapshots_to_restore.list
  # cat  ./snapshots.list | sed -ne '/'$restore_state'/,$ p' | grep -v "$restore_state" > ./snapshots_to_restore.list
  # if [ -e ./snapshots_to_restore.list ] ; then
  #   if [ -s ./snapshots_to_restore.list ] ; then
  #     echo "snapshots to be restored from $snapshot_repository_location :"
  #     snapshots_to_restore_list=`cat ./snapshots_to_restore.list`
  #     echo $snapshots_to_restore_list
  #   else
  #     echo "no snapshots to be restored from $snapshot_repository_location. exiting"
  #     exit
  #   fi
  # fi
  
  echo "get_ordered_list_of_snapshots_to_restore not implemented for S3" 
  
}
function get_list_of_snapshots_to_restore()
{
  # # get local snapshot list ordered
  # ls -ltr $snapshot_repository_location | grep snap-snapshot_ | awk '{ print $NF }' | awk -F_ '{print $2}' | awk -F. '{print "snapshot_"$1}' > ./snapshots.list
  # echo "current existing snapshots from $snapshot_repository_location :"
  # snapshot_list=`cat ./snapshots.list`
  # echo "$snapshot_list"
  # 
  get_ordered_list_of_snapshots_to_restore
}
function rsync()
{
  echo "rsync request executing..." 
  rsync -az $snapshot_repository_location $user@$rsynctargethostdns:$snapshot_repository_location

  if [ $? -eq 0 ]
  then
     echo "failover successfull" 
  else
     echo "Problem failed" 
     false # returns 1 so it triggers the trap
     exit 1
  fi
}
function mount()
{
  if [[ "$snapshot_repository_location" == %2F* ]]; then
    echo "S3 snapshot repository, no more mount point..." 
  else
    echo "mount Point Check request executing..." 
    mount_exist=$(df -k $snapshot_repository_location | wc -l)
    if [ ${mount_exist} = "2" ] ; then
      echo "mount Point for Repository exists." 
      exit 0
    else
      echo "ERROR: mount Point for Repository does not exists." 
      exit 1
    fi
    
    if [ $? -eq 0 ]
    then
       echo "mount successfull" 
    else
       echo "mount failed" 
    fi
  fi
}
function execution()
{
[[ -z "${context}" ]] && context='default' || context="${context}"
[[ -z "${type}" ]] && type='default' || type="${type}"

  echo "executing $1 ..." 
  echo "             environment variable provided by haes: $context" 
  echo "             environment variable  provided by haes: $type" 
  echo "             event service location: $event_service_location" 
  echo "             clusters shared snapshot repository location: $snapshot_repository_location" 
  echo "             user: $user" 
  echo "             action: $action" 
  echo "             rsynctargethostdns: $rsynctargethostdns" 
  echo "             snapshotid: $snapshotid" 
  case $action in
  failover)
    failover
    ;;
  failback)
    failback
    ;;
  snapshot)
    snapshot
    ;;
  fastrestore)
    fastrestore
    ;;
  restoreto)
    restoreto
    ;;
  restore)
    restore
    ;;
  rsync)
    rsync
    ;;
    mount)
      mount
      ;;
  *)
    echo "wrong action specified." 
    exit 1
    ;;
  esac

}
while getopts U:a:l:r:s:H:h flag; do
               case $flag in
                U)
                                user=$OPTARG
                                ;;
                a)
                                action=$OPTARG

                                ;;
                l)

                                event_service_location=$OPTARG
                                ;;
                r)
                                snapshot_repository_location=$OPTARG
                                ;;

                s)
                                snapshotid=$OPTARG
                                ;;
                H)
                                rsynctargethostdns=$OPTARG
                                ;;

                h|*)
                                usage
                ;;
                esac
done
scriptdir=$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
cd $scriptdir

echo " using script directory : $scriptdir" 


[ ! -d "$scriptdir/snapshot" ] && mkdir -p "$scriptdir/snapshot"


execution


if [ $action_status -eq 0 ]
then
   echo "execution successfull for $action" 
   exit 0
else
   echo "execution failed  for $action" 
   exit 1
fi

exit 1
