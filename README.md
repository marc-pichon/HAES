In case HA is required for Event Service, it is possible to define a secondary ES cluster and synchronize between them.

Synchronization is capability to request ES services available and ES data on each ES cluster the same.

One ES cluster is in active state when its ES nodes are available and answering all ES requests from clients.

One ES cluster is in passive state, forbidding any request from ES clients.



Haes provides answers to Event Service dedicated Load Balancer, maintains ES data synched between clusters as much as possible and finally handles fail over and fail back.

Functionalities


Round Robin LB requests use same URL request to check if one Event Service node is available or not.

The LB will execute this request on all HA ES nodes (active and passive cluster)



In case one ES node is on passive cluster, or down or not working properly then LB request answer will be negative.

In all other conditions (LB request on active cluster and cluster healthy and node available) then LB request answer will be positive.



In case the active ES cluster is not healthy (“Red status”) then an automatic Fail Over is triggered.

-        This ES cluster becomes passive

-        The previously passive cluster becomes active

-        The ES LB will execute ES requests on the new available active cluster



The Fail Back operation is always manual. An API is provided to execute Fail Back operation from command line.



ES Health checks are executed on regular basis (configurable).

the LB requests are served by each HA ES node. each HA ES node manages the local check on ES node asynchronously. 
Snapshots and Restore are executed on regular basis (configurable).



Added features
some customers do have SSL termination at Event Service node level

admin port and elastic port configured with SSL supported soon
Potentials
studying rsync option to copy remotely snapshots from one location to remote location

studying alternative to the requirement of sharing snapshot repository to be shared (nfs, shared FS)

Ceph seems promising, potentially GlueFS which seems simpler

the "backup" to some S3 repository may be used

design could be

elastic plugin repository-s3 
ceph s3 storage running on rook(k8s)
AWS S3 is an other option, using an AWS plugin

see https://medium.com/@federicopanini/elasticsearch-backup-snapshot-and-restore-on-aws-s3-f1fc32fbca7f
