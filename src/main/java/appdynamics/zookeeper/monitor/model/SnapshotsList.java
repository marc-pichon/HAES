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
"snapshots"
})
public class SnapshotsList {

@JsonProperty("snapshots")
private List<Snapshot> snapshots = new ArrayList<Snapshot>();
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

@JsonProperty("snapshots")
public List<Snapshot> getSnapshots() {
return snapshots;
}

@JsonProperty("snapshots")
public void setSnapshots(List<Snapshot> snapshots) {
this.snapshots = snapshots;
}

public SnapshotsList withSnapshots(List<Snapshot> snapshots) {
this.snapshots = snapshots;
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

public SnapshotsList withAdditionalProperty(String name, Object value) {
this.additionalProperties.put(name, value);
return this;
}

}
