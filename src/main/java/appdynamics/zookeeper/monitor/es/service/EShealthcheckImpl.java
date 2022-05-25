package appdynamics.zookeeper.monitor.es.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService;
import appdynamics.zookeeper.monitor.api.ESZkService;
import appdynamics.zookeeper.monitor.api.ZkService;
import appdynamics.zookeeper.monitor.configuration.LocalProperties;
import appdynamics.zookeeper.monitor.configuration.SRScriptsProperties;
import appdynamics.zookeeper.monitor.model.ESHealthResult;
import appdynamics.zookeeper.monitor.model.EsclusterInfoMasterNode;
import appdynamics.zookeeper.monitor.model.Snapshot;
import appdynamics.zookeeper.monitor.model.SnapshotsList;
import appdynamics.zookeeper.monitor.util.EmailService;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.ES_NODE;
import static appdynamics.zookeeper.monitor.util.ZkHaesUtil.HAES_RESTORE;
import appdynamics.zookeeper.monitor.zkwatchers.ESClusterAvailabilityChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;


/** @author "Marc Pichon" 08/10/20 */

@Slf4j
//@Service (03/09: retiré car pris en compte dans configuration bean en tant que service
public class EShealthcheckImpl implements EShealthcheck {

	/*
	 * we need to reorganize here
	 * some checks are duplictaes: curl http://localhost:9081/healthcheck?pretty=true
	 * also when errors occured, we need to separte correctly when it comes from appd layer and elastic
	 * so that we don't ask to correct elastic when appd is in trouble.
	 */

	private final RestTemplate restTemplate;
	//private ZkClient zkClient;
        public final static SimpleDateFormat dfm = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

	@Autowired 
	private ESZkService eszkService;
	@Autowired 
	private ZkService zkService;
	@Autowired
	private EmailService emailService;
	@Autowired
	private LocalProperties localProperties;

	@Autowired 
	private ESSnapshotRestoreService esSnapshotRestoreService;
	@Autowired
	private SRScriptsProperties srScriptsProperties;


	String local_node_IP = "127.0.0.1";
	String local_loopback_IP = "127.0.0.1";
	String local_node_port = "9081";
	String local_node_port_external = "9080";

	boolean escheckstatus = true;

	/*
	 * (non-Javadoc)
	 * @see appdynamics.zookeeper.monitor.es.service.EShealthcheck#update_escheckstatus(boolean)
	 */

	boolean diag_zk_process_present = false;
	boolean diag_appd_es_process_present = false;
	boolean diag_es_process_present = false;

	/*
	 * these ones must be true first as updates of them will happen after scheduler trigger the checks
	 */
	boolean diag_es_level_cluster_status_ok = true; // red, yellow, green
	boolean diag_appd_level_cluster_status_ok = true; // red, yellow, green


	public void update_escheckstatus(boolean err_status) {
		/*
		 * used by error handler
		 */
		escheckstatus = err_status;
	}


	public EShealthcheckImpl(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplate = restTemplateBuilder.build();


		/*
		 * added for rest error handling in case of http 5xx errors as well.
		 * critical to handle errors checking at ES level
		 */
		//restTemplate.setErrorHandler(new RestTemplateResponseErrorHandler());
	}


	public boolean esNodeHealthcheckRestcalls() {
		escheckstatus = true;
                boolean connectionOK = true;
		ResponseEntity<String> response = null;
		local_loopback_IP = localProperties.getloopback();
		log.debug("esNodeHealthcheckRestcall/local_loopback_IP: ", local_loopback_IP);
		/*
		 * first check if cluster state is GREEN/RED/YELLOW
		 * IF RED, return node status ko
		 */

		/*
		 * Node check #1 ping/pong check ES level
		 */
		String rc = "ko";

		//HttpClientErrorException – in case of HTTP status 4xx
		//HttpServerErrorException – in case of HTTP status 5xx
		//UnknownHttpStatusCodeException – in case of an unknown HTTP status

		try {
			response = restTemplate.getForEntity("http://".concat(local_loopback_IP).concat(":").concat(local_node_port_external).concat("/").concat("_ping"), String.class);

		} 
		catch (HttpServerErrorException exception) {
			log.info("esNodeHealthcheckRestcall check #1: SERVER_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpClientErrorException exception) {
			log.info("esNodeHealthcheckRestcall check #1: CLIENT_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpStatusCodeException exception) {
			log.info("esNodeHealthcheckRestcall check #1: HTTP_CODE_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (ResourceAccessException exception) { // all IO exceptions handled by RestTemplate here
			log.info("esNodeHealthcheckRestcall check #1: Connection Error " + exception.getLocalizedMessage());
			escheckstatus = false;

		}

		if (escheckstatus) {

			log.info("esNodeHealthcheckRestcall/getStatusCode ES level: {}", response.getStatusCode());
			log.info("esNodeHealthcheckRestcall/result is: {}", response.getBody());
			if (!response.getStatusCode().is2xxSuccessful()) {

			} else {
				if (!response.getBody().contains("_pong")) {
					rc = "ko";
				} else {
					rc = "ok";
				}
			}
		}
		/*
		 * Node check #1 (continued) _ping/_pong check Event service level
		 */
		try {

			response = restTemplate.getForEntity("http://".concat(local_loopback_IP).concat(":").concat(local_node_port).concat("/").concat("ping"), String.class);
		} 
		catch (HttpServerErrorException exception) {
			log.info("esNodeHealthcheckRestcall check #1: SERVER_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpClientErrorException exception) {
			log.info("esNodeHealthcheckRestcall check #1: CLIENT_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpStatusCodeException exception) {
			log.info("esNodeHealthcheckRestcall check #1: HTTP_CODE_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (ResourceAccessException exception) { // all IO exceptions handled by RestTemplate here
                    String message ="Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " esNodeHealthcheckRestcall check #1: Connection Error " + exception.getLocalizedMessage();
			log.info(message);
                        if (emailService.sendMail(message)) {
				log.debug("esNodeHealthcheckRestcall/sendMail succeeded");
			} else {
				log.error("esNodeHealthcheckRestcall/sendMail failed");
			}
			escheckstatus = false;
                        connectionOK = false;
		}

		if (escheckstatus) {		
			log.info("esNodeHealthcheckRestcall/check #1 getStatusCode Event Servive level: {}", response.getStatusCode());
			log.debug("esNodeHealthcheckRestcall/result is: {}", response.getBody());
			if (!response.getStatusCode().is2xxSuccessful()) {

			} else {
				if (!response.getBody().contains("pong")) {
					rc = "ko";
				} else {
					if (rc.contentEquals("ok")) rc = "ok";
				}
			}
		}
		//eszkService.setNodeStatus(rc);
		/*
		 * if rest call has succeeded, then update accordingly, else status not checked. so we presume an error.
		 */
		if (escheckstatus) eszkService.setNodeStatus(rc);
		if (!escheckstatus) eszkService.setNodeStatus("ko");
                return connectionOK;
	}
        
	public void esRepositoryRestcall() {
            
            String s = eszkService.getClusterAvailability(eszkService.getLocalESClusterID());
            if ("ok".equals(s)) {
		
		String restcall = null;
		/*
		 * check the repository for that node has been created. if not send an email
		 */
		/*
		 * get repository
		 */
		try {
			restcall = restTemplate.getForObject("http://".concat(local_loopback_IP).concat(":9200/_snapshot"), String.class);
		} 
		catch (HttpServerErrorException exception) {
			log.info("esRepositoryRestcall: SERVER_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpClientErrorException exception) {
			log.info("esRepositoryRestcall: CLIENT_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpStatusCodeException exception) {
			log.info("esRepositoryRestcall: HTTP_CODE_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (ResourceAccessException exception) { // all IO exceptions handled by RestTemplate here
			log.info("esRepositoryRestcall: Connection Error " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		log.debug("esRepositoryRestcall/repository rescall is: {}", restcall);

		if ((restcall == null) || restcall.isEmpty()) // very likely the repository has not been created/defined on that node
		{
			log.error("Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " esRepositoryRestcall/repository restcall is null. very likely the repository has not been created/defined on that node. exiting");
			/* 
			 * sending email
			 */
			String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " detected the snapshot repository is not accessible. Very likely the repository has not been created/defined on that node.";

			if (emailService.sendMail(message)) {
				log.debug("esRepositoryRestcall/sendMail succeeded");
			} else {
				log.error("esRepositoryRestcall/sendMail failed");
			}
		}
            }

	}
	public void esHealthcheckRestcalls() { 
		
		/*
		 * A MODIFIER: on va mettre les nodes status a jour ICI !!!!!! trouver un statut du cluster a modifier!!!!
		 */
		escheckstatus = true;
		

		local_loopback_IP = localProperties.getloopback();
		log.debug("esHealthcheckRestcalls/local_loopback_IP: ", local_loopback_IP);
		
		/*
		 * Node check #2: control status healthty for 2 properties
		 * Events Service health
		 * underlying Elastic Search health
		 */
		String restcall =  null;
		try {
			restcall = restTemplate.getForObject("http://".concat(local_loopback_IP).concat(":").concat(local_node_port).concat("/").concat("healthcheck"), String.class);
			log.debug("esHealthcheckRestcalls: check #2 restcall: " + restcall);
		} 
		catch (HttpServerErrorException exception) {
			log.info("esHealthcheckRestcalls: check #2 SERVER_ERROR: " + exception.getLocalizedMessage());
			handleHttp500ErrorOnHealthcheck (exception.getResponseBodyAsString());
			escheckstatus = false;

		}
		catch (HttpClientErrorException exception) {
			log.info("esHealthcheckRestcalls: check #2 CLIENT_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpStatusCodeException exception) {
			log.info("esHealthcheckRestcalls:  check #2 HTTP_CODE_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (ResourceAccessException exception) { // all IO exceptions handled by RestTemplate here
			log.info("esHealthcheckRestcalls:  check #2 Connection Error " + exception.getLocalizedMessage());
			escheckstatus = false;

		}

		if (escheckstatus) {
			try {
				// pretty print
				ObjectMapper mapper = new ObjectMapper();
				String json;

				json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(restcall);
				log.debug("esHealthcheckRestcalls/ check #2 cluster health/result is: {}", json);

				ESHealthResult res = mapper.readValue(restcall, ESHealthResult.class);

				Map<String, Object> props = res.getAdditionalProperties();
				props.forEach((k,v)->{
					//log.info("esHealthcheckRestcalls/ check #2 cluster health/properties are: {}",k );
					if("events-service-api-store / Build information".equals(k)){
						//log.info("esHealthcheckRestcalls/ check #2 cluster health/properties values are: {}",v);
						// unfortunately then, this is no more json... need a rough parsing
						if (v.toString().contains("healthy=true")) {
							log.info("esHealthcheckRestcalls/ check #2 cluster health is ok for events-service-api-store / Build information");
							//eszkService.setNodeStatus("ok");
							/*
							 * if rest call has succeeded, then update accordingly, else status not checked. so we presume an error.
							 */
							if (escheckstatus) eszkService.setNodeStatus("ok");
							if (!escheckstatus) eszkService.setNodeStatus("ko");

						} else {
							//eszkService.setNodeStatus("ko");
							/*
							 * if rest call has succeeded, then update accordingly, else status not checked. so we presume an error.
							 */
							if (escheckstatus) eszkService.setNodeStatus("ko");
							if (!escheckstatus) eszkService.setNodeStatus("ko");

						}
					}
					if("events-service-api-store / Connection to ElasticSearch: clusterName=[appdynamics-events-service-cluster]".equals(k)){
						//log.info("esHealthcheckRestcalls/ check #2 cluster health/properties values are: {}",v);
						// unfortunately then, this is no more json... need a rough parsing
						if (v.toString().contains("healthy=true")) {
							log.info("esHealthcheckRestcalls/ check #2 Elastic Search health is ok for events-service-api-store / Connection to ElasticSearch: clusterName=[appdynamics-events-service-cluster]");
							//eszkService.setNodeStatus("ok");
							/*
							 * if rest call has succeeded, then update accordingly, else status not checked. so we presume an error.
							 */
							if (escheckstatus) eszkService.setNodeStatus("ok");
							if (!escheckstatus) eszkService.setNodeStatus("ko");

						} else {
							//eszkService.setNodeStatus("ko");
							/*
							 * if rest call has succeeded, then update accordingly, else status not checked. so we presume an error.
							 */
							if (escheckstatus) eszkService.setNodeStatus("ko");
							if (!escheckstatus) eszkService.setNodeStatus("ko");

						}
					}
				});

				//log.info("esNodeHealthcheckRestcall/cluster health/properties are: {}", res.getAdditionalProperties().toString());
			} catch (JsonParseException e1) {
				log.error("esHealthcheckRestcalls/cluster health/JsonParseException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (JsonMappingException e1) {
				log.error("esHealthcheckRestcalls/cluster health/JsonMappingException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (IOException e1) {
				log.error("esHealthcheckRestcalls/cluster health/IOException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			}

		}
	}
	public void handleHttp500ErrorOnHealthcheck (String restcallbody) {
	

		if (escheckstatus) {
			try {

				// parse

				/*
				 * we need to chnage inner fileds with "\" to "\\"
				 */
				//restcallbody = restcallbody.replace("\\", "\\\\");
				ObjectMapper mapper = new ObjectMapper();
				Map < String, Map > outs = mapper.readValue(restcallbody, Map.class);
				for (Entry < String, Map > out: outs.entrySet()) {
					log.info("esHealthcheckRestcalls " + out.getKey() + " = " + out.getValue());
					if (!(Boolean)out.getValue().get("healthy")) {
						
						log.info("esHealthcheckRestcalls/ Event Service healthcheck: component not healthy: " + out.getKey() + " = " + out.getValue().get("message"));
					} else {
						log.info("esHealthcheckRestcalls/ Event Service healthcheck: component is healthy: " + out.getKey() + " = " + out.getValue().get("error"));						
					}
				}

			} catch (JsonParseException e1) {
				log.error("esHealthcheckRestcalls/cluster health/JsonParseException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (JsonMappingException e1) {
				log.error("esHealthcheckRestcalls/cluster health/JsonMappingException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (IOException e1) {
				log.error("esHealthcheckRestcalls/cluster health/IOException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			}
		}
	}

    @Override
    public void esHealthCheckCluster1LastCheck() {
        log.info("esHealthCheckCluster1LastCheck:  running on "+eszkService.getLocalESClusterID()+"/"+eszkService.getLocalESNodeID());
        boolean amILeader = zkService.getLeaderNodeData2(eszkService.getLocalESClusterID()).contentEquals(eszkService.getLocalESNodeID());
        if (amILeader) {
            log.info("esHealthCheckCluster1LastCheck:  running on "+eszkService.getLocalESClusterID()+"/"+eszkService.getLocalESNodeID()+" (HAES leader)");
        } else {
            log.info("esHealthCheckCluster1LastCheck:  running on "+eszkService.getLocalESClusterID()+"/"+eszkService.getLocalESNodeID()+" (Not HAES leader)");
        }
        if (eszkService.getLocalESClusterID().equals("CLUSTER2") && amILeader) {
            // Le cluster1 a ete detecte KO
            log.info("esHealthCheckCluster1LastCheck: Last cluster1 status update time:"+dfm.format(new Date(ESClusterAvailabilityChangeListener.lastCluster1StatusCheckUpdate)));
            if (System.currentTimeMillis() - ESClusterAvailabilityChangeListener.lastCluster1StatusCheckUpdate > 3 * 60000) {
                log.info("esHealthCheckCluster1LastCheck:  fail over has been triggered due to no update of cluster1 availability after 3 min");

                CompletableFuture<Boolean> rc_ok_cf = esSnapshotRestoreService.doFailOver();
                boolean rc_ok = false;
                try {
                    rc_ok = rc_ok_cf.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("esHealthCheckCluster1LastCheck:  fail over return code action execution control failed");
                    rc_ok = false;
                }

            } else {
                log.info("esHealthCheckCluster1LastCheck:  fail over is not triggered for execution");
            }

        }

    }
    
	private class Suite {

		@JsonProperty("healthy")
		public Boolean healthy;
		public Boolean getHealthy() {
			return healthy;
		}
		public void setHealthy(Boolean healthy) {
			this.healthy = healthy;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		@JsonProperty("message")
		public String message;

	}
        
        
    public boolean esHealthcheckClusterRestcalls() {
        escheckstatus = true;
        boolean connectionOK = true;
        local_loopback_IP = localProperties.getloopback();
        log.debug("esHealthcheckClusterRestcalls/local_loopback_IP {} ", local_loopback_IP);
        String restcall = null;
        try {
            restcall = restTemplate.getForObject("http://".concat(local_loopback_IP).concat(":9200/_cluster/health"), String.class);
            log.debug("esHealthcheckClusterRestcalls/cluster health/result is: {}", restcall);
        } catch (HttpServerErrorException exception) {
            log.info("esHealthcheckClusterRestcalls: SERVER_ERROR: " + exception.getLocalizedMessage());
            escheckstatus = false;

        } catch (HttpClientErrorException exception) {
            log.info("esHealthcheckClusterRestcalls: CLIENT_ERROR: " + exception.getLocalizedMessage());
            escheckstatus = false;

        } catch (HttpStatusCodeException exception) {
            log.info("esHealthcheckClusterRestcalls: HTTP_CODE_ERROR: " + exception.getLocalizedMessage());
            escheckstatus = false;

        } catch (ResourceAccessException exception) { // all IO exceptions handled by RestTemplate here
            String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " esHealthcheckClusterRestcalls: "+"daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() +" - Connection Error " + exception.getLocalizedMessage();
            log.info(message);
            if (emailService.sendMail(message)) {
                log.debug("esHealthcheckClusterRestcalls/sendMail succeeded");
            } else {
                log.error("esHealthcheckClusterRestcalls/sendMail failed");
            }
            escheckstatus = false;
            connectionOK = false;
        }

        boolean redStatus = false;
        if (escheckstatus) {
            try {
                // pretty print
                ObjectMapper mapper = new ObjectMapper();
                String json;

                json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(restcall);
                log.debug("esHealthcheckClusterRestcalls/cluster health/result is: {}", json);

                ESHealthResult res = mapper.readValue(restcall, ESHealthResult.class);

                log.info("esHealthcheckClusterRestcalls/cluster health/status is: {}", res.getStatus());

                if (res.getStatus().toLowerCase().contains("green") || res.getStatus().toLowerCase().contains("yellow")) {
                    diag_es_level_cluster_status_ok = true;
                    /*
                     * if rest call has succeeded, then update accordingly, else status not checked. so we presume an error.
                     */
                    // if (escheckstatus) { // useless
                    /*
                     * get current status from zk
                     * if it's the same then don't update : change => Update as well. A watchdog on other cluster2 check last update from cluster1 nodes. If no more 
                     * nodes available on cluster1, lastupdate is checked and filover fires if last update too far.
                     */
                    if (!eszkService.getClusterAvailability(eszkService.getLocalESClusterID()).contentEquals("ok") && !eszkService.isLocalClusterRunningRestore()) {
                        String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " this cluster is now available.";

                        if (emailService.sendMail(message)) {
                            log.debug("esHealthcheckClusterRestcalls/sendMail succeeded");
                        } else {
                            log.error("esHealthcheckClusterRestcalls/sendMail failed");
                        }
                    }
                    eszkService.setClusterAvailability(eszkService.getLocalESClusterID(), "ok");

                    // }

                    // }
                } else { // obviously red
                    redStatus = true;
                }

            } catch (JsonParseException e1) {
                log.error("esHealthcheckClusterRestcalls/cluster health/JsonParseException error is: {}", e1.getLocalizedMessage());
                e1.printStackTrace();
            } catch (JsonMappingException e1) {
                log.error("esHealthcheckClusterRestcalls/cluster health/JsonMappingException error is: {}", e1.getLocalizedMessage());
                e1.printStackTrace();
            } catch (IOException e1) {
                log.error("esHealthcheckClusterRestcalls/cluster health/IOException error is: {}", e1.getLocalizedMessage());
                e1.printStackTrace();
            }

            // escheckstatus == false
        }
        log.info("esHealthcheckClusterRestcalls : aftercheck : conn="+connectionOK+", escheck="+escheckstatus+", redStatus="+redStatus);
       
        if (connectionOK && !escheckstatus || redStatus) {
            
                    /*
                     * get current status from zk
                     * if it's the same then don't update => OK with that. One cluster is ko, no need to send "alive" updates to cluster2
                     * it will prevent the availability listener to wake up and check for fail over
                     */
                    String s = eszkService.getClusterAvailability(eszkService.getLocalESClusterID());
                    log.info("esHealthcheckClusterRestcalls : "+eszkService.getLocalESClusterID()+" current availability before updating to Ko : "+s);
                    if (!s.equals("ko")) {
                        eszkService.setClusterAvailability(eszkService.getLocalESClusterID(), "ko");
                        // If cluster is passive and currently running a restore, avoid sending normal RED status
                        // as snapshot may involve a temporary short RED status...
                        if (!eszkService.isLocalClusterRunningRestore()) {
                            String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " this cluster is no more available.";
                            if (emailService.sendMail(message)) {
                                log.debug("esHealthcheckClusterRestcalls/sendMail succeeded");
                            } else {
                                log.error("esHealthcheckClusterRestcalls/sendMail failed");
                            }
                        }
                    }
                    
                    diag_es_level_cluster_status_ok = false;
                    /*
                     * if rest call has succeeded, then update accordingly, else status not checked. so we presume an error.
                     */

                    /* 
                     * sending email
                     */
                    if (!eszkService.isLocalClusterRunningRestore()) {
                        String message = "Daemon : " + eszkService.getLocalESClusterID() + "/" + eszkService.getLocalESNodeID() + " detected this cluster is in RED status.";
                        if (emailService.sendMail(message)) {
                            log.debug("esHealthcheckClusterRestcalls/sendMail succeeded");
                        } else {
                            log.error("esHealthcheckClusterRestcalls/sendMail failed");
                        }
                    }
                    
                    // esSnapshotRestoreService.doFailOver(); /// <<< Curious. Failover should be done through zk listeners !
                    // Disable to give a chance to HAES Cluster 2 performing failover.
        }
        return connectionOK;
    }
        
    
        
	public void esMasterNodesRestcall() {
            
		escheckstatus = true;
		/*
		 * problematic currently:
		 * each HAES daemon is doing actually that updates for every node.
		 * 
		 * ideally, when zk election of "master live node" is in place, only that node will trigger this action
		 */
		local_loopback_IP = localProperties.getloopback();
		log.debug("esMasterNodesRestcall/local_loopback_IP: ", local_loopback_IP);

		/*
		 * getting information on master/slave nodes + IDs
		 */
		String restcall = null;
		try {
			restcall = restTemplate.getForObject("http://".concat(local_loopback_IP).concat(":9200/_cat/nodes?format=JSON"), String.class);
			log.debug("esMasterNodesRestcall/identifying master ES nodes/result is: {}", restcall);
		} 
		catch (HttpServerErrorException exception) {
			log.info("esMasterNodesRestcall: SERVER_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpClientErrorException exception) {
			log.info("esMasterNodesRestcall: CLIENT_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpStatusCodeException exception) {
			log.info("esMasterNodesRestcall: HTTP_CODE_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (ResourceAccessException exception) { // all IO exceptions handled by RestTemplate here
			log.info("esMasterNodesRestcall: Connection Error " + exception.getLocalizedMessage());
			escheckstatus = false;

		}

		if (escheckstatus) {

			try {

				ObjectMapper mapper = new ObjectMapper();
				ObjectReader reader = mapper.reader().forType(new TypeReference<List<EsclusterInfoMasterNode>>(){});
				String json;

				// pretty print
				json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(restcall);
				log.debug("esMasterNodesRestcall/ json master ES nodes/result is: {}", json);

				List<EsclusterInfoMasterNode> result = reader.readValue(restcall);

				ListIterator<EsclusterInfoMasterNode> iterator = result.listIterator(); 
                                
                                Map<String, Boolean> SurvivingESNodes = new HashMap<>();                                

				while (iterator.hasNext()) { 
					EsclusterInfoMasterNode node = iterator.next(); 
					log.debug("esMasterNodesRestcall/Node getIp is: {}", node.getIp());
					log.debug("esMasterNodesRestcall/Node getNodeRole is: {}", node.getNodeRole());
					log.debug("esMasterNodesRestcall/Node getMaster is: {}", node.getMaster());
        				/*
					 * should give : * (elected master) and - (not elected master)
					 */
					if (node.getMaster().toLowerCase().contains("*")) {
						// eszkService.getESNodeIDFromIP(node.getIp());
                                                SurvivingESNodes.put(eszkService.getESNodeIDFromIP(node.getIp()), true);
	
						//eszkService.setNodeAsMasterRole(eszkService.getESClusterIDFromIP(node.getIp()), eszkService.getESNodeIDFromIP(node.getIp()), "true");

						/*
						 * if rest call has succeeded, then update accordingly, else status not checked. so we presume an error.
						 */
						// if (escheckstatus) // <<< Useless
							eszkService.setNodeAsMasterRole(eszkService.getESClusterIDFromIP(node.getIp()), eszkService.getESNodeIDFromIP(node.getIp()), "true");

						/// Euh.... we'll never get this code running !
                                                // if (!escheckstatus) 
						// 	eszkService.setNodeAsMasterRole(eszkService.getESClusterIDFromIP(node.getIp()), eszkService.getESNodeIDFromIP(node.getIp()), "false");


						/*
						 * update corresponding election node: creating it if not exists already
						 * check that this node is still alive
						 * then insert into election tree
						 */
						/*
					if (zkService.checkLiveNodeExist(eszkService.getESClusterIDFromIP(node.getIp()), eszkService.getESNodeIDFromIP(node.getIp()))) {
						zkService.createNodeInElectionZnode(eszkService.getESClusterIDFromIP(node.getIp()), eszkService.getESNodeIDFromIP(node.getIp()));
					    ClusterInfo.getClusterInfo().setMaster(eszkService.getESClusterIDFromIP(node.getIp()), zkService.getLeaderNodeData2(eszkService.getESNodeIDFromIP(node.getIp())));
					    zkService.registerChildrenChangeWatcher(eszkService.getLocalESClusterID(),ELECTION_NODE_2, masterChangeListener);
					} else {
						log.error("esMasterNodesRestcall/Node clusterID: " + eszkService.getESClusterIDFromIP(node.getIp()) +  " NodeID: " + eszkService.getESNodeIDFromIP(node.getIp()) + " is not a live node. internal error.");

					}
						 */
					} else {
                                            SurvivingESNodes.put(eszkService.getESNodeIDFromIP(node.getIp()), false);
						//eszkService.setNodeAsMasterRole(eszkService.getESClusterIDFromIP(node.getIp()), eszkService.getESNodeIDFromIP(node.getIp()), "false");

						/*
						 * if rest call has succeeded, then update accordingly, else status not checked. so we presume an error.
						 */
						// if (escheckstatus) 
							eszkService.setNodeAsMasterRole(eszkService.getESClusterIDFromIP(node.getIp()), eszkService.getESNodeIDFromIP(node.getIp()), "false");

						// if (!escheckstatus) 
						// 	eszkService.setNodeAsMasterRole(eszkService.getESClusterIDFromIP(node.getIp()), eszkService.getESNodeIDFromIP(node.getIp()), "false");

					}
				}
                                
                                // Remove old master if node is no more available...
                                String localClusterId = eszkService.getLocalESClusterID();
                                for (String nodeId : zkService.getAllNodes(localClusterId)) {
                                    if (!SurvivingESNodes.containsKey(nodeId) && eszkService.amIanESMaster(localClusterId, nodeId)) {
                                        eszkService.setNodeAsMasterRole(localClusterId, nodeId, "false");
                                    }
                                }

			} catch (JsonParseException e1) {
				log.error("esMasterNodesRestcall/JsonParseException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (JsonMappingException e1) {
				log.error("esMasterNodesRestcall/JsonMappingException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (IOException e1) {
				log.error("esMasterNodesRestcall/IOException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			}
		}
	}
	public SnapshotsList esgetSnapshotsList() {

		boolean esstatus = true;
		local_loopback_IP = localProperties.getloopback();
		log.debug("esHealthcheckClusterRestcalls/local_loopback_IP {} ", local_loopback_IP);
		String restcall = null;
		/*
		 * get repository
		 * problem here is that we may have multiple Elastic repositories
		 * so we have to use the sr.script.snapshotrepositorylocation of application property file
		 */ 

		//try {


		/*
		 * ES settings
		 * curl "localhost:9200/_nodes/settings?pretty"
		 * 
		 * curl -XGET localhost:9200/_snapshot/_all : get all snapshot repositories
		 * output: 
		 * {"/opt/appdynamics/platform/product/events-service/data/appdynamics-analytics-backup":{"type":"fs","settings":{"location":"//opt/appdynamics/platform/product/events-service/data/appdynamics-analytics-backup/appdynamics-events-service-cluster"}}}
		 * 
		 * curl http://localhost:9200/_snapshot/%2Fopt%2Fappdynamics%2Fplatform%2Fproduct%2Fevents-service%2Fdata%2Fappdynamics-analytics-backup/_all?pretty
		 */
		/*
			restcall = restTemplate.getForObject("http://".concat(local_loopback_IP).concat(":9200/_snapshot"), String.class);

			log.info("esgetSnapshotsList/repository rescall is: {}", restcall);
			log.info("esgetSnapshotsList/repository rescall size: {}", restcall.length());

			// sanitized in case hugly answer...
		    StringBuilder fixed = restcall.codePoints()
			        .filter(c -> {
			          switch (Character.getType(c)) {
			            case Character.CONTROL:
			            case Character.FORMAT:
			            case Character.PRIVATE_USE:
			            case Character.SURROGATE:
			            case Character.UNASSIGNED:
			              return false;
			            default:
			              return true;
			          }
			        })
			        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);

		    restcall = fixed.toString();


			String trim = restcall.replaceAll("\\s+","");

			if ((restcall == null) || restcall.isEmpty() || trim.isEmpty()) // very likely the repository has not been created/defined on that node
			{
				log.error("esgetSnapshotsList/repository restcall is null or empty. very likely the repository has not been created/defined on that node. exiting");
				return null;
			}
		 */
		/*
		 * ugly... no json parsing found for this one
		 */
		/*
			String[] parts = restcall.split(":");
			if (parts.length >= 1) {
				String part1 = parts[0];
				if (part1.length() >= 1) {
					String[] repoparts = part1.split("\"");
					if (repoparts.length >= 2) {

						String repo_string = repoparts[1];
						log.info("esgetSnapshotsList/repository  is: {}", repo_string);
						if (repo_string == null) esstatus = false;
		 */
		/*
		 * using URI w.a
		 */
		/*
						// works!!! String endpointUrl = "%2Fopt%2Fappdynamics%2Fplatform%2Fproduct%2Fevents-service%2Fdata%2Fappdynamics-analytics-backup";
						String endpointUrl = repo_string.replaceAll("/", "%2F");

						StringBuilder builder = new StringBuilder(("http://").concat(local_loopback_IP).concat(":9200/_snapshot/"));
						builder.append(endpointUrl);
						builder.append("/_all");


						URI uri = URI.create(builder.toString());
						restcall = restTemplate.getForObject(uri, String.class);
						log.info("esgetSnapshotsList/restcall using URI/result is: {}", restcall);
					} else {
						esstatus = false;
						log.error("esgetSnapshotsList/parsing error on result");
					}
				} else {
					esstatus = false;
					log.error("esgetSnapshotsList/parsing error on result");
				}
			} else {
				esstatus = false;
				log.error("esgetSnapshotsList/parsing error on result");
			}
		} 
		catch (HttpServerErrorException exception) {
			log.info("esgetSnapshotsList: SERVER_ERROR: " + exception.getLocalizedMessage());
			esstatus = false;

		}
		catch (HttpClientErrorException exception) {
			log.info("esgetSnapshotsList: CLIENT_ERROR: " + exception.getLocalizedMessage());
			esstatus = false;

		}
		catch (HttpStatusCodeException exception) {
			log.info("esgetSnapshotsList: HTTP_CODE_ERROR: " + exception.getLocalizedMessage());
			esstatus = false;

		}
		catch (ResourceAccessException exception) { // all IO exceptions handled by RestTemplate here
			log.info("esgetSnapshotsList: Connection Error " + exception.getLocalizedMessage());
			esstatus = false;

		}
		 */


		try {
			String repo_string = srScriptsProperties.getSnapshotrepositorylocation();
			String endpointUrl = repo_string.replaceAll("/", "%2F");

			StringBuilder builder = new StringBuilder(("http://").concat(local_loopback_IP).concat(":9200/_snapshot/"));
			builder.append(endpointUrl);
			builder.append("/_all");


			URI uri = URI.create(builder.toString());
			restcall = restTemplate.getForObject(uri, String.class);
			log.debug("esgetSnapshotsList/restcall using URI/result is: {}", restcall);

		}
		catch (HttpServerErrorException exception) {
			log.info("esgetSnapshotsList: SERVER_ERROR: " + exception.getLocalizedMessage());
			esstatus = false;

		}
		catch (HttpClientErrorException exception) {
			log.info("esgetSnapshotsList: CLIENT_ERROR: " + exception.getLocalizedMessage());
			esstatus = false;

		}
		catch (HttpStatusCodeException exception) {
			log.info("esgetSnapshotsList: HTTP_CODE_ERROR: " + exception.getLocalizedMessage());
			esstatus = false;

		}
		catch (ResourceAccessException exception) { // all IO exceptions handled by RestTemplate here
			log.info("esgetSnapshotsList: Connection Error " + exception.getLocalizedMessage());
			esstatus = false;

		}

		if (esstatus) {

			try {
				// pretty print
				ObjectMapper mapper = new ObjectMapper();

				String json;

				json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(restcall);
				log.debug("esgetSnapshotsList/SnapshotsList is: {}", json);

				SnapshotsList snapshotsList = mapper.readValue(restcall, SnapshotsList.class);

				Iterator it = snapshotsList.getSnapshots().iterator();
				while(it.hasNext()) {
					Snapshot snapshot = (Snapshot) it.next();
					log.debug("esgetSnapshotsList/Snapshots is: " + snapshot.getSnapshot() + " status: " + snapshot.getState());

				}
				return snapshotsList;

			} catch (JsonParseException e1) {
				log.error("EShealthcheck/cluster health/JsonParseException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (JsonMappingException e1) {
				log.error("EShealthcheck/cluster health/JsonMappingException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (IOException e1) {
				log.error("EShealthcheck/cluster health/IOException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			}
		}
		// default
		return null;
	}
	public String getDiagnostic() {
		String message = "<p align=\"center\">Diagnostic Information - local view from that node</p><br>";
		/*
		 * check all required process are there
		 */
		String line;
		Process process;
		try {
			process = Runtime.getRuntime().exec("ps -ef");
			process.getOutputStream().close();
			BufferedReader input =
					new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((line = input.readLine()) != null) {
				if (line.contains("java")) {
					if (line.contains("events-service/processor/elasticsearch")) {
						diag_es_process_present = true;
					}
					if (line.contains("events-service/processor/conf/events-service-api-store.properties")) {
						diag_appd_es_process_present = true;
					}
					if (line.contains("/conf/zoo.cfg")) {
						diag_zk_process_present = true;
					}

				}
			}
			input.close();

		} catch (IOException e) {
			log.error("esDiagnostic/Error is: " + e.getLocalizedMessage().toString());
		}
		if (diag_es_process_present) {
			message = message.concat("ElasticSearch process present").concat("<br>");
		} else {
			message = message.concat("<b><font color='red'> ElasticSearch process not present</font></b>").concat("<br>");
		}

		if (diag_appd_es_process_present) {
			message = message.concat("Appdynamics Event service process present").concat("<br>");
		} else {
			message = message.concat("<b><font color='red'> Appdynamics Event service process not present</font></b>").concat("<br>");
		}
/* not used anymore as zk embedded
		if (diag_zk_process_present) {
			message = message.concat("Zookeeper process present").concat("<br>");
		} else {
			message = message.concat("<b><font color='red'> Zookeeper process not present</font></b>").concat("<br>");
		}
*/
		/*
		 * check cluster health
		 */
		if (diag_es_level_cluster_status_ok) {
			message = message.concat("ElasticSearch cluster is green or yellow").concat("<br>");
		} else {
			message = message.concat("<b><font color='red'> ElasticSearch cluster is red</font></b>").concat("<br>");
		}
		if (diag_appd_level_cluster_status_ok) {
			message = message.concat("ElasticSearch cluster check from Appdynamics Event Service layer is green or yellow").concat("<br>");
		} else {
			message = message.concat("<b><font color='red'> ElasticSearch cluster check from Appdynamics Event Service layer is red</font></b>").concat("<br>");
		}

		boolean escheckstatus = true;
		local_loopback_IP = localProperties.getloopback();
		log.debug("getDiagnostic/local_loopback_IP {} ", local_loopback_IP);
		String restcall = null;
		try {
			restcall = restTemplate.getForObject("http://".concat(local_loopback_IP).concat(":9200/_cluster/health"), String.class);
			log.debug("getDiagnostic/cluster health/result is: {}", restcall);
		} 
		catch (HttpServerErrorException exception) {
			log.info("getDiagnostic: SERVER_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpClientErrorException exception) {
			log.info("getDiagnostic: CLIENT_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (HttpStatusCodeException exception) {
			log.info("getDiagnostic: HTTP_CODE_ERROR: " + exception.getLocalizedMessage());
			escheckstatus = false;

		}
		catch (ResourceAccessException exception) { // all IO exceptions handled by RestTemplate here
			log.info("getDiagnostic: Connection Error " + exception.getLocalizedMessage());
			escheckstatus = false;

		}

		if (escheckstatus) {

			try {
				// pretty print
				ObjectMapper mapper = new ObjectMapper();
				String json;

				json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(restcall);
				log.debug("esHealthcheckClusterRestcalls/cluster health/result is: {}", json);

				ESHealthResult res = mapper.readValue(restcall, ESHealthResult.class);
				
				message = message.concat("HAES Cluster: ").concat(eszkService.getLocalESClusterID()).concat("<br>");
				message = message.concat("HAES Cluster Availability: ").concat(eszkService.getClusterAvailability(eszkService.getLocalESClusterID())).concat("<br>");

				message = message.concat("ES Cluster: ").concat(res.getClusterName()).concat("<br>");
				message = message.concat("Number Of Nodes: ").concat(String.valueOf(res.getNumberOfNodes())).concat("<br>");
				message = message.concat("ES Cluster status: ").concat(res.getStatus()).concat("<br>");
				message = message.concat("Initializing Shards: ").concat(String.valueOf(res.getInitializingShards())).concat("<br>");
				message = message.concat("Unassigned Shards: ").concat(String.valueOf(res.getUnassignedShards())).concat("<br>");
			
				message = message.concat(" ").concat("<br>");
				
				message = message.concat("Active Shards: ").concat(String.valueOf(res.getActiveShards())).concat("<br>");
				message = message.concat("Active Primary Shards: ").concat(String.valueOf(res.getActivePrimaryShards())).concat("<br>");
				message = message.concat("Active Shards Percent As Number: ").concat(String.valueOf(res.getActiveShardsPercentAsNumber())).concat("<br>");
				message = message.concat("Delayed Unassigned Shards: ").concat(String.valueOf(res.getDelayedUnassignedShards())).concat("<br>");			

			} catch (JsonParseException e1) {
				log.error("getDiagnostic/cluster health/JsonParseException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (JsonMappingException e1) {
				log.error("getDiagnostic/cluster health/JsonMappingException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			} catch (IOException e1) {
				log.error("getDiagnostic/cluster health/IOException error is: {}", e1.getLocalizedMessage());
				e1.printStackTrace();
			}
		}

		/* 
		 * check node health
		 */


		/*
		 * end
		 */
		return message;

	}
}