package appdynamics.zookeeper.monitor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/*
 * curl http://127.0.0.1:9200/_cluster/health result
 * used in esHealthcheckClusterRestcalls
 */

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonPropertyOrder({
	"cluster_name",
	"status",
	"timed_out",
	"number_of_nodes",
	"number_of_data_nodes",
	"active_primary_shards",
	"active_shards",
	"relocating_shards",
	"initializing_shards",
	"unassigned_shards",
	"delayed_unassigned_shards",
	"number_of_pending_tasks",
	"number_of_in_flight_fetch",
	"task_max_waiting_in_queue_millis",
	"active_shards_percent_as_number"
	})
	@JsonIgnoreProperties(ignoreUnknown = true)
	public class ESHealthResult {


	@JsonProperty("cluster_name")
	private String clusterName;
	@JsonProperty("status")
	private String status;
	@JsonProperty("timed_out")
	private Boolean timedOut;
	@JsonProperty("number_of_nodes")
	private Integer numberOfNodes;
	@JsonProperty("number_of_data_nodes")
	private Integer numberOfDataNodes;
	@JsonProperty("active_primary_shards")
	private Integer activePrimaryShards;
	@JsonProperty("active_shards")
	private Integer activeShards;
	@JsonProperty("relocating_shards")
	private Integer relocatingShards;
	@JsonProperty("initializing_shards")
	private Integer initializingShards;
	@JsonProperty("unassigned_shards")
	private Integer unassignedShards;
	@JsonProperty("delayed_unassigned_shards")
	private Integer delayedUnassignedShards;
	@JsonProperty("number_of_pending_tasks")
	private Integer numberOfPendingTasks;
	@JsonProperty("number_of_in_flight_fetch")
	private Integer numberOfInFlightFetch;
	@JsonProperty("task_max_waiting_in_queue_millis")
	private Integer taskMaxWaitingInQueueMillis;
	@JsonProperty("active_shards_percent_as_number")
	private Double activeShardsPercentAsNumber;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("cluster_name")
	public String getClusterName() {
	return clusterName;
	}

	@JsonProperty("cluster_name")
	public void setClusterName(String clusterName) {
	this.clusterName = clusterName;
	}

	@JsonProperty("status")
	public String getStatus() {
	return status;
	}

	@JsonProperty("status")
	public void setStatus(String status) {
	this.status = status;
	}

	@JsonProperty("timed_out")
	public Boolean getTimedOut() {
	return timedOut;
	}

	@JsonProperty("timed_out")
	public void setTimedOut(Boolean timedOut) {
	this.timedOut = timedOut;
	}

	@JsonProperty("number_of_nodes")
	public Integer getNumberOfNodes() {
	return numberOfNodes;
	}

	@JsonProperty("number_of_nodes")
	public void setNumberOfNodes(Integer numberOfNodes) {
	this.numberOfNodes = numberOfNodes;
	}

	@JsonProperty("number_of_data_nodes")
	public Integer getNumberOfDataNodes() {
	return numberOfDataNodes;
	}

	@JsonProperty("number_of_data_nodes")
	public void setNumberOfDataNodes(Integer numberOfDataNodes) {
	this.numberOfDataNodes = numberOfDataNodes;
	}

	@JsonProperty("active_primary_shards")
	public Integer getActivePrimaryShards() {
	return activePrimaryShards;
	}

	@JsonProperty("active_primary_shards")
	public void setActivePrimaryShards(Integer activePrimaryShards) {
	this.activePrimaryShards = activePrimaryShards;
	}

	@JsonProperty("active_shards")
	public Integer getActiveShards() {
	return activeShards;
	}

	@JsonProperty("active_shards")
	public void setActiveShards(Integer activeShards) {
	this.activeShards = activeShards;
	}

	@JsonProperty("relocating_shards")
	public Integer getRelocatingShards() {
	return relocatingShards;
	}

	@JsonProperty("relocating_shards")
	public void setRelocatingShards(Integer relocatingShards) {
	this.relocatingShards = relocatingShards;
	}

	@JsonProperty("initializing_shards")
	public Integer getInitializingShards() {
	return initializingShards;
	}

	@JsonProperty("initializing_shards")
	public void setInitializingShards(Integer initializingShards) {
	this.initializingShards = initializingShards;
	}

	@JsonProperty("unassigned_shards")
	public Integer getUnassignedShards() {
	return unassignedShards;
	}

	@JsonProperty("unassigned_shards")
	public void setUnassignedShards(Integer unassignedShards) {
	this.unassignedShards = unassignedShards;
	}

	@JsonProperty("delayed_unassigned_shards")
	public Integer getDelayedUnassignedShards() {
	return delayedUnassignedShards;
	}

	@JsonProperty("delayed_unassigned_shards")
	public void setDelayedUnassignedShards(Integer delayedUnassignedShards) {
	this.delayedUnassignedShards = delayedUnassignedShards;
	}

	@JsonProperty("number_of_pending_tasks")
	public Integer getNumberOfPendingTasks() {
	return numberOfPendingTasks;
	}

	@JsonProperty("number_of_pending_tasks")
	public void setNumberOfPendingTasks(Integer numberOfPendingTasks) {
	this.numberOfPendingTasks = numberOfPendingTasks;
	}

	@JsonProperty("number_of_in_flight_fetch")
	public Integer getNumberOfInFlightFetch() {
	return numberOfInFlightFetch;
	}

	@JsonProperty("number_of_in_flight_fetch")
	public void setNumberOfInFlightFetch(Integer numberOfInFlightFetch) {
	this.numberOfInFlightFetch = numberOfInFlightFetch;
	}

	@JsonProperty("task_max_waiting_in_queue_millis")
	public Integer getTaskMaxWaitingInQueueMillis() {
	return taskMaxWaitingInQueueMillis;
	}

	@JsonProperty("task_max_waiting_in_queue_millis")
	public void setTaskMaxWaitingInQueueMillis(Integer taskMaxWaitingInQueueMillis) {
	this.taskMaxWaitingInQueueMillis = taskMaxWaitingInQueueMillis;
	}

	@JsonProperty("active_shards_percent_as_number")
	public Double getActiveShardsPercentAsNumber() {
	return activeShardsPercentAsNumber;
	}

	@JsonProperty("active_shards_percent_as_number")
	public void setActiveShardsPercentAsNumber(Double activeShardsPercentAsNumber) {
	this.activeShardsPercentAsNumber = activeShardsPercentAsNumber;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
	return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
	this.additionalProperties.put(name, value);
	}

	
}
