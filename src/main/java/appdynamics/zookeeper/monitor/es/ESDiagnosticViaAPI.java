package appdynamics.zookeeper.monitor.es;

import java.io.IOException;

/*
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.search.stats.SearchStats.Stats;
import org.elasticsearch.rest.RestStatus;
*/

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class ESDiagnosticViaAPI {

	boolean checkstatus = true;

	private static ESDiagnosticViaAPI instance;
	
	//private final RestHighLevelClient client;


	public static  ESDiagnosticViaAPI getInstance() {
		if (instance == null)
			instance = new ESDiagnosticViaAPI(); 
		return instance;
	}
	private ESDiagnosticViaAPI() {
	/*	
		this.client = new RestHighLevelClient(
				RestClient.builder(
						new HttpHost("localhost", 9200, "http"),
						new HttpHost("localhost", 9201, "http")));
*/
	}	

	/*
	 * curl -XGET http://localhost:9200/_cat/shards?v&bytes=b&h=node,index,shard,docs,store
	 * 
	 * 
	 * 
	 * checkClusterHealth
	 * http://localhost:9200/_cluster/health
	 * 
	 * checkApiHealth
	 * http://localhost:9081/healthcheck
	 * 
	 * 
	 * 
	 * https://sematext.com/blog/top-10-elasticsearch-metrics-to-watch/
	 * 
	 * Cluster Health – Nodes and Shards
	Search Performance – Request Latency and
	Search Performance – Request Rate
	Indexing Performance – Refresh Times
	Indexing Performance – Merge Times
	Node Health – Memory Usage
	Node Health – Disk I/O
	Node Health – CPU
	JVM Health  – Heap Usage and Garbage Collection
	JVM health – JVM Pool Size
	 */

	/*
	 * An Elasticsearch cluster can consist of one or more nodes. A node is a member of the cluster, hosted on an individual server. Adding additional nodes is what allows us to scale the cluster horizontally. Indexes organize the data within the cluster. An index is a collection of documents which share a similar characteristic.

Consider the example of an Elasticsearch cluster deployed to store log entries from an application. An index might be set up to collect all log entries for a day. Each log entry is a document which contains the contents of the log and associated metadata.

In large datasets, the size of an index might exceed the storage capacity on a single node. We also want to ensure that we have redundant copies of our index, in case something happens to a node. Elasticsearch handles this by dividing an index into a defined number of shards. Elasticsearch distributes the shards across all nodes in the cluster. By default, an Elasticsearch index has five shards with one replica. The result of this default configuration is an index divided into five shards, each with a single replica stored on a different node.

It is essential to find the right number of shards for an index because too few shards may negatively affect search efficiency and distribution of data across the nodes. Conversely, too many nodes create an excessive demand on the resources of the cluster for their management.


	 */

	/*
	 * Relocating and initializing shards indicate rebalancing on the cluster or the creation of new shards. Rebalancing occurs when a node is added or removed from the cluster and will affect the performance of the cluster. By understanding these metrics and how they affect cluster performance, you will have more insight into the cluster and can tune the cluster for better performance. One such adjustment is adding a shard relocation delay when a node leaves the cluster, eliminating excessive overhead if it returns quickly.
	 */

	/*
	 ***************************************** Important Metrics for Cluster Health
Status	The status of the cluster:
Red: No shards have been allocated.
Yellow: Only the primary shards have been allocated.
Green: All shards have been allocated.
Nodes	This metric includes the total number of nodes in the cluster, and includes the count of successful and failed nodes.
Count of Active Shards	The number of active shards within the cluster.
Relocating Shards	Count of shards being moved due to the loss of a node.
Initializing Shards	Count of shards being initialized due to the addition of an index.
Unassigned Shards	Count of shards for which replicas have not been created or assigned yet.
	 */
	public String clusterHealthDiag() {
		/*
		 * Cluster Health:  Shards and Nodes
		 * GET _cluster/health
		 */

		String message = "ClusterHealthDiag<br>";
		message = message.concat("under construction: need to align the current ES release libs").concat("<br>");

		// https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-cluster-health.html
/*
		ClusterHealthRequest request = new ClusterHealthRequest();
		request.timeout(TimeValue.timeValueSeconds(10));
		ClusterHealthResponse response = null;
		try {
			response = client.cluster().health(request, RequestOptions.DEFAULT);

		}
		catch (ElasticsearchException e) {
			log.error("ClusterHealthDiag/ES API client ElasticsearchException error is: {}", e.getLocalizedMessage());
			return null;
		} 

		catch (IOException e) {
			log.error("ClusterHealthDiag/ES API client IOException error is: {}", e.getLocalizedMessage());
			return null;
		} 

		if (response.status() == RestStatus.OK) { 
			message = message.concat("ES Cluster: ").concat(response.getClusterName()).concat("<br>");
			message = message.concat("Number Of Nodes: ").concat(String.valueOf(response.getNumberOfNodes())).concat("<br>");
			message = message.concat("ES Cluster status: ").concat(response.getStatus().toString()).concat("<br>");
			message = message.concat("Initializing Shards: ").concat(String.valueOf(response.getInitializingShards())).concat("<br>");
			message = message.concat("Unassigned Shards: ").concat(String.valueOf(response.getUnassignedShards())).concat("<br>");

			message = message.concat(" ").concat("<br>");

			message = message.concat("Active Shards: ").concat(String.valueOf(response.getActiveShards())).concat("<br>");
			message = message.concat("Active Primary Shards: ").concat(String.valueOf(response.getActivePrimaryShards())).concat("<br>");
			message = message.concat("Active Shards Percent As Number: ").concat(String.valueOf(response.getActiveShardsPercent())).concat("<br>");
			message = message.concat("Delayed Unassigned Shards: ").concat(String.valueOf(response.getDelayedUnassignedShards())).concat("<br>");			
			message = message.concat("Relocating Shards: ").concat(String.valueOf(response.getRelocatingShards())).concat("<br>");			

			message = message.concat(" ").concat("<br>");
			message = message.concat("Status	The status of the cluster").concat("<br>");
			message = message.concat("Red: No shards have been allocated.").concat("<br>");
			message = message.concat("Yellow: Only the primary shards have been allocated.").concat("<br>");
			message = message.concat("Green: All shards have been allocated.").concat("<br>");
			message = message.concat("Nodes	This metric includes the total number of nodes in the cluster, and includes the count of successful and failed nodes.").concat("<br>");
			message = message.concat("Count of Active Shards	The number of active shards within the cluster.").concat("<br>");
			message = message.concat("Relocating Shards	Count of shards being moved due to the loss of a node.").concat("<br>");
			message = message.concat("Initializing Shards	Count of shards being initialized due to the addition of an index.").concat("<br>");
			message = message.concat("Unassigned Shards	Count of shards for which replicas have not been created or assigned yet.").concat("<br>");
			message = message.concat(" ").concat("<br>");
		}
		
		*/
		
		/* exemple pour recup json
		SearchResponse response = client.prepareSearch(index).setExplain(true).execute().actionGet();

		XContentBuilder builder = XContentFactory.jsonBuilder();
		response.toXContent(builder, ToXContent.EMPTY_PARAMS);
		JSONObject json = new JSONObject(builder.string());	
		 */


		return message;

	}

	/*
	 * Search Performance: Request Rate and Latency
A data source is only as good as it is useful, and we can measure the effectiveness of the cluster by measuring the rate at which the system is processing requests and how long each request is taking.

When the cluster receives a request, it may need to access data from multiple shards, across multiple nodes. Knowing the rate at which the system is processing and returning requests, how many requests are currently in progress, and how long requests are taking can provide valuable insights into the health of the cluster.

The request process itself is divided into two phases, the first is the query phase, during which the cluster distributes the request to each shard (either primary or replica) within the index. During the second, fetch phase, the results of the query are gathered, compiled and returned to the user.
	 */

	/*
	 * We want to be aware of spikes in any of these metrics, as well as any emerging trends which might indicate growing problems within the cluster. These metrics are calculated by index and are available from the RESTful endpoints on the cluster itself.

Please refer to the table below for metrics which are available from the index endpoint which is found at /index_name/_stats where index_name is the name of the index. Performance specific metrics have been highlighted in light blue.

	 ******************************** Important Metrics for Request Performance
Number of queries currently in progress	Count of queries currently being processed by the cluster.
Number of fetches currently in progress	Count of fetches in progress within the cluster.
Total number of queries	Aggregated number of all queries processed by the cluster
Total time spent on queries	Total time consumed by all queries in milliseconds.
Total number of fetches	Aggregated number of all fetches processed by the cluster.
Total time spent on fetches	Total time consumed by all fetches in milliseconds. 
	 */

	public String searchPerformanceDiag() {
		/*
		 * Search Performance: Request Rate and Latency
		 * GET /index_name/_stats
		 */

		String message = "Search Performance Diag<br>";


		// https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-cluster-health.html
/*
		ClusterHealthRequest request = new ClusterHealthRequest();
		request.timeout(TimeValue.timeValueSeconds(10));
		
		request.local(true);
		request.level(ClusterHealthRequest.Level.SHARDS);
		
		
		ClusterHealthResponse response = null;
		try {
			response = client.cluster().health(request, RequestOptions.DEFAULT);

		}
		catch (ElasticsearchException e) {
			log.error("ClusterHealthDiag/ES API client ElasticsearchException error is: {}", e.getLocalizedMessage());
			return null;
		} 

		catch (IOException e) {
			log.error("ClusterHealthDiag/ES API client IOException error is: {}", e.getLocalizedMessage());
			return null;
		} 

		message = message.concat(" ").concat("<br>");
		message = message.concat("Number of queries currently in progress	Count of queries currently being processed by the cluster.").concat("<br>");
		message = message.concat("Number of fetches currently in progress	Count of fetches in progress within the cluster.").concat("<br>");
		message = message.concat("Total number of queries	Aggregated number of all queries processed by the cluster").concat("<br>");
		message = message.concat("Total time spent on queries	Total time consumed by all queries in milliseconds.").concat("<br>");
		message = message.concat("Total number of fetches	Aggregated number of all fetches processed by the cluster.").concat("<br>");
		message = message.concat("Total time spent on fetches	Total time consumed by all fetches in milliseconds.").concat("<br>");
		message = message.concat(" ").concat("<br>");


		*/
		

		return message;

	}

	/*
	 * Index Performance: Refresh and Merge Times
As documents are updated, added and removed from an index, the cluster needs to continually update their indexes and then refresh them across all the nodes.  All of this is taken care of by the cluster, and as a user, you have limited control over this process, other than to configure the refresh interval rate.

Additions, updates, and deletions are batched and flushed to disk as new segment, and as each segment consumes resources, it is important for performance that smaller segments are consolidated and merged into larger segments. Like indexing, this is managed by the cluster itself.
	 */

	/*
	 * Monitoring the indexing rate of documents and merge time can help with identifying anomalies and related problems before they begin to affect the performance of the cluster. Considering these metrics in parallel with the health of each node can provide essential clues to potential problems within the system, or opportunities to optimize performance.
	 */

	/*
	 * Index performance metrics can be retrieved from the /_nodes/stats endpoint and can be summarized at the node, index or shard level. This endpoint has a plethora of information, and the sections under merges and refresh are where you’ll find relevant metrics for index performance.
	 */

	/*
	 * ********************************Important Metrics for Index Performance
Total refreshes	Count of the total number of refreshes.
Total time spent refreshing	Aggregation of all time spent refreshing. Measure in milliseconds.
Current merges	Merges currently being processed.
Total merges	Count of the total number of merges.
Total time spent merging	Aggregation of all time spent merging segments.
	 */

	public String indexPerformanceDiag() {
		/*
		 * Index Performance: Refresh and Merge Times
		 * GET /_nodes/stats
		 */

		String message = "Index Performance Diag<br>";

		message = message.concat(" ").concat("<br>");
		message = message.concat("Total refreshes	Count of the total number of refreshes. ").concat("<br>");
		message = message.concat("Total time spent refreshing	Aggregation of all time spent refreshing. Measure in milliseconds. ").concat("<br>");
		message = message.concat("Current merges	Merges currently being processed. ").concat("<br>");
		message = message.concat("Total merges	Count of the total number of merges. ").concat("<br>");
		message = message.concat("Total time spent merging	Aggregation of all time spent merging segments. ").concat("<br>");
		message = message.concat(" ").concat("<br>");

		return message;

	}

	/*
	 * ****************************** Important Metrics for Node Health
Total disk capacity	Total disk capacity on the node’s host machine.
Total disk usage	Total disk usage on the node’s host machine.
Total available disk space	Total disk space available.
Percentage of disk used	Percentage of disk which is already used.
Current RAM usage	Current memory usage (unit of measurement).
RAM percentage	Percentage of memory being used.
Maximum RAM	Total amount of memory on the node’s host machine
CPU	Percentage of the CPU in use
	 */

	public String nodeResourcesDiag() {
		/*
		 * Node Health: Memory, Disk, and CPU Metrics
		 * GET /_cat/nodes
		 */
		String message = "Node Resources Diag<br>";

		/* 
		 * cluster level
		 */
		
		/*
		
		ClusterStateRequest request = new ClusterStateRequest();
		
		request.clear().nodes(true);
		request.local();
		request.masterNodeTimeout(new TimeValue(60, TimeUnit.SECONDS));
		*/
		
		/*
		 * nodes level
		 */
		
		/*
		
        NodesInfoRequest nodesInfoRequest = new NodesInfoRequest();
        //nodesInfoRequest.clear().jvm(false).os(false).process(true);
        
        NodesStatsRequest nodesStatsRequest = new NodesStatsRequest();
        //nodesStatsRequest.clear().jvm(false).os(false).fs(false).indices(false).process(false);
        
        NodeClient nodeclient = new NodeClient(null, null);
        nodeclient.admin().cluster().nodesStats(nodesStatsRequest);
        
        NodesStatsResponse response = nodeclient.admin().cluster().nodesStats(new NodesStatsRequest().all()).actionGet();
        NodeStats node = null;
        for (NodeStats n : response.getNodes()) {
			log.info("nodeResourcesDiag/ Node name: {}", n.getNode().getName());
			log.info("nodeResourcesDiag/ Host name: {}", n.getNode().getHostName());
			log.info("nodeResourcesDiag/ Data Node: {}", n.getNode().isDataNode());
			log.info("nodeResourcesDiag/ Master Node: {}", n.getNode().isMasterNode());

       }
       
       for (Entry<String, NodeStats> nodemap : response.getNodesMap().entrySet()) {
			log.info("nodeResourcesDiag/ Node name: {}", nodemap.getKey());
			log.info("nodeResourcesDiag/ HeapMax: {}", nodemap.getValue().getJvm().getMem().getHeapMax());
			log.info("nodeResourcesDiag/ HeapUsed: {}", nodemap.getValue().getJvm().getMem().getHeapUsed());
			log.info("nodeResourcesDiag/ HeapUsedPercent: {}", nodemap.getValue().getJvm().getMem().getHeapUsedPercent());
			
		    for (Entry<String, org.elasticsearch.index.shard.IndexingStats.Stats> indicemap : nodemap.getValue().getIndices().getIndexing().getTypeStats().entrySet()) {
		    	log.info("nodeResourcesDiag/ indice / : {}", indicemap.getKey());
		    	log.info("nodeResourcesDiag/ IndexCount / : {}", indicemap.getValue().getIndexCount());
		    	log.info("nodeResourcesDiag/ IndexTime / : {}", indicemap.getValue().getIndexTime());
		    	log.info("nodeResourcesDiag/ IndexTime / : {}", indicemap.getValue().getIndexFailedCount());
		    }
		   
		    log.info("nodeResourcesDiag/ TotalOperations: {}", nodemap.getValue().getFs().getIoStats().getTotalOperations());
		    log.info("nodeResourcesDiag/ TotalReadKilobytes: {}", nodemap.getValue().getFs().getIoStats().getTotalReadKilobytes());
		    log.info("nodeResourcesDiag/ TotalReadOperations: {}", nodemap.getValue().getFs().getIoStats().getTotalReadOperations());
		    log.info("nodeResourcesDiag/ TotalWriteKilobytes: {}", nodemap.getValue().getFs().getIoStats().getTotalWriteKilobytes());
		    log.info("nodeResourcesDiag/ TotalWriteOperations: {}", nodemap.getValue().getFs().getIoStats().getTotalWriteOperations());
       	     
       }
 

		message = message.concat(" ").concat("<br>");
		message = message.concat("Total disk capacity	Total disk capacity on the node’s host machine. ").concat("<br>");
		message = message.concat("Total disk usage	Total disk usage on the node’s host machine. ").concat("<br>");
		message = message.concat("Total available disk space	Total disk space available. ").concat("<br>");
		message = message.concat("Percentage of disk used	Percentage of disk which is already used. ").concat("<br>");
		message = message.concat("Current RAM usage	Current memory usage (unit of measurement). ").concat("<br>");
		message = message.concat("RAM percentage	Percentage of memory being used. ").concat("<br>");
		message = message.concat("Maximum RAM	Total amount of memory on the node’s host machine ").concat("<br>");
		message = message.concat("CPU	Percentage of the CPU in use ").concat("<br>");
		message = message.concat(" ").concat("<br>");

*/

		return message;

	}

	/*
	 * JVM metrics can be retrieved from the /_nodes/stats endpoint.

	 ************************** Important Metrics for JVM Health
Memory usage	Usage statistics for heap and non-heap processes and pools.
Threads	Current threads in use, and maximum number.
Garbage collection	Counts and total time spent with garbage collection.
	 */
	public String nodeJVMDiag() {
		/*
		 * JVM Health: Heap, GC, and Pool Size
		 * GET /_nodes/stats
		 */

		String message = "Node JVM Diag<br>";


		// https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-cluster-health.html

		/*
		ClusterHealthRequest request = new ClusterHealthRequest();
		request.timeout(TimeValue.timeValueSeconds(10));
		
		request.local(true);
		request.level(ClusterHealthRequest.Level.SHARDS);

		
		ClusterHealthResponse response = null;
		try {
			response = client.cluster().health(request, RequestOptions.DEFAULT);

		}
		catch (ElasticsearchException e) {
			log.error("nodeJVMDiag/ES API client ElasticsearchException error is: {}", e.getLocalizedMessage());
			return null;
		} 

		catch (IOException e) {
			log.error("nodeJVMDiag/ES API client IOException error is: {}", e.getLocalizedMessage());
			return null;
		} 

		message = message.concat(" ").concat("<br>");
		message = message.concat("Memory usage	Usage statistics for heap and non-heap processes and pools. ").concat("<br>");
		message = message.concat("Threads	Current threads in use, and maximum number. ").concat("<br>");
		message = message.concat("Garbage collection	Counts and total time spent with garbage collection. ").concat("<br>");
		message = message.concat(" ").concat("<br>");

*/
		return message;

	}

	/*
	 * Cluster Health:  Shards and Nodes
	 * GET _cluster/health
	 */

	/*
	 * Search Performance: Request Rate and Latency
	 * GET /index_name/_stats
	 */

	/*
	 * Index Performance: Refresh and Merge Times
	 * GET /_nodes/stats
	 */

	/*
	 * Node Health: Memory, Disk, and CPU Metrics
	 * GET /_cat/nodes
	 */

	/*
	 * JVM Health: Heap, GC, and Pool Size
	 * GET /_nodes/stats
	 */

	/*
	 * 
	 */
	/**
	 * Retrieves cluster-health information for shards associated with the specified index. The request will time out
	 * if no results are returned after a period of time indicated by timeout.
	 * @param index is used to restrict the request to a specified index.
	 * @param timeout is the command timeout period in seconds.
	 * @return a set of shard ids for the specified index.
	 * @throws IOException if an error occurs while sending the request to the Elasticsearch instance.
	 * @throws RuntimeException if the request times out, or no active-primary shards are present.
	 */
	/*
	public Set<Integer> getShardIds(String index, RestHighLevelClient client, long timeout)
	        throws RuntimeException, IOException
	{
	*/
		/*
	    ClusterHealthRequest request = new ClusterHealthRequest(index)
	            .timeout(new TimeValue(timeout, TimeUnit.SECONDS));
	            
	            */
	    
	    // Set request to shard-level details
		
		/*
		
	    request.level(ClusterHealthRequest.Level.SHARDS);
	    
	    ClusterHealthResponse response = client.cluster().health(request, RequestOptions.DEFAULT);

	    if (response.isTimedOut()) {
	        throw new RuntimeException("Request timed out for index (" + index + ").");
	    }
	    else if (response.getActiveShards() == 0) {
	        throw new RuntimeException("There are no active shards for index (" + index + ").");
	    }
	    else if (response.getStatus() == ClusterHealthStatus.RED) {
	        throw new RuntimeException("Request aborted for index (" + index +
	                ") due to cluster's status (RED) - One or more primary shards are unassigned.");
	    }
	    else if (!response.getIndices().containsKey(index)) {
	        throw new RuntimeException("Request has an invalid index (" + index + ").");
	    }
	    String result = "";
	    response.getIndices().keySet().forEach(name -> {result.concat(name);});
	    log.info("getShardIds:list of shards for index: " + index + " " + result);

	  
	    return response.getIndices().get(index).getShards().keySet();
	    
	}
	*/
	
	public void close() {
		/*
		try {
			client.close();
		} catch (IOException e) {
			log.error("nodeJVMDiag/ES API client Exception on closing client, error is: {}", e.getLocalizedMessage());
		}
*/
	}

}