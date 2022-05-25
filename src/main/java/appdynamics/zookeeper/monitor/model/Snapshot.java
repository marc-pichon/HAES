package appdynamics.zookeeper.monitor.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"snapshot",
"version_id",
"version",
"indices",
"state",
"start_time",
"start_time_in_millis",
"end_time",
"end_time_in_millis",
"duration_in_millis",
"failures",
"shards"
})
public class Snapshot {

@JsonProperty("snapshot")
private String snapshot;
@JsonProperty("version_id")
private Integer versionId;
@JsonProperty("version")
private String version;
@JsonProperty("indices")
private List<String> indices = new ArrayList<String>();
@JsonProperty("state")
private String state;
@JsonProperty("start_time")
private String startTime;
@JsonProperty("start_time_in_millis")
private Long startTimeInMillis;
@JsonProperty("end_time")
private String endTime;
@JsonProperty("end_time_in_millis")
private Long endTimeInMillis;
@JsonProperty("duration_in_millis")
private Long durationInMillis;
@JsonProperty("failures")
private List<Object> failures = new ArrayList<Object>();
@JsonProperty("shards")
private Shards shards;
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

@JsonProperty("snapshot")
public String getSnapshot() {
return snapshot;
}

@JsonProperty("snapshot")
public void setSnapshot(String snapshot) {
this.snapshot = snapshot;
}

public Snapshot withSnapshot(String snapshot) {
this.snapshot = snapshot;
return this;
}

@JsonProperty("version_id")
public Integer getVersionId() {
return versionId;
}

@JsonProperty("version_id")
public void setVersionId(Integer versionId) {
this.versionId = versionId;
}

public Snapshot withVersionId(Integer versionId) {
this.versionId = versionId;
return this;
}

@JsonProperty("version")
public String getVersion() {
return version;
}

@JsonProperty("version")
public void setVersion(String version) {
this.version = version;
}

public Snapshot withVersion(String version) {
this.version = version;
return this;
}

@JsonProperty("indices")
public List<String> getIndices() {
return indices;
}

@JsonProperty("indices")
public void setIndices(List<String> indices) {
this.indices = indices;
}

public Snapshot withIndices(List<String> indices) {
this.indices = indices;
return this;
}

@JsonProperty("state")
public String getState() {
return state;
}

@JsonProperty("state")
public void setState(String state) {
this.state = state;
}

public Snapshot withState(String state) {
this.state = state;
return this;
}

@JsonProperty("start_time")
public String getStartTime() {
return startTime;
}

@JsonProperty("start_time")
public void setStartTime(String startTime) {
this.startTime = startTime;
}

public Snapshot withStartTime(String startTime) {
this.startTime = startTime;
return this;
}

@JsonProperty("start_time_in_millis")
public Long getStartTimeInMillis() {
return startTimeInMillis;
}

@JsonProperty("start_time_in_millis")
public void setStartTimeInMillis(Long startTimeInMillis) {
this.startTimeInMillis = startTimeInMillis;
}

public Snapshot withStartTimeInMillis(Long startTimeInMillis) {
this.startTimeInMillis = startTimeInMillis;
return this;
}

@JsonProperty("end_time")
public String getEndTime() {
return endTime;
}

@JsonProperty("end_time")
public void setEndTime(String endTime) {
this.endTime = endTime;
}

public Snapshot withEndTime(String endTime) {
this.endTime = endTime;
return this;
}

@JsonProperty("end_time_in_millis")
public Long getEndTimeInMillis() {
return endTimeInMillis;
}

@JsonProperty("end_time_in_millis")
public void setEndTimeInMillis(Long endTimeInMillis) {
this.endTimeInMillis = endTimeInMillis;
}

public Snapshot withEndTimeInMillis(Long endTimeInMillis) {
this.endTimeInMillis = endTimeInMillis;
return this;
}

@JsonProperty("duration_in_millis")
public Long getDurationInMillis() {
return durationInMillis;
}

@JsonProperty("duration_in_millis")
public void setDurationInMillis(Long durationInMillis) {
this.durationInMillis = durationInMillis;
}

public Snapshot withDurationInMillis(Long durationInMillis) {
this.durationInMillis = durationInMillis;
return this;
}

@JsonProperty("failures")
public List<Object> getFailures() {
return failures;
}

@JsonProperty("failures")
public void setFailures(List<Object> failures) {
this.failures = failures;
}

public Snapshot withFailures(List<Object> failures) {
this.failures = failures;
return this;
}

@JsonProperty("shards")
public Shards getShards() {
return shards;
}

@JsonProperty("shards")
public void setShards(Shards shards) {
this.shards = shards;
}

public Snapshot withShards(Shards shards) {
this.shards = shards;
return this;
}

@JsonAnyGetter
public Map<String, Object> getAdditionalProperties() {
return this.additionalProperties;
}

@JsonAnySetter
public void setAdditionalProperty(String name, Object value) {
this.additionalProperties.put(name, value);
}

public Snapshot withAdditionalProperty(String name, Object value) {
this.additionalProperties.put(name, value);
return this;
}

}
