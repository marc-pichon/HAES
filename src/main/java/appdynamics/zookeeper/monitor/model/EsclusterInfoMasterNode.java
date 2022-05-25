package appdynamics.zookeeper.monitor.model;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"host",
"ip",
"heap.percent",
"ram.percent",
"load",
"node.role",
"master",
"name"
})
public class EsclusterInfoMasterNode {

@JsonProperty("host")
private String host;
@JsonProperty("ip")
private String ip;
@JsonProperty("heap.percent")
private String heapPercent;
@JsonProperty("ram.percent")
private String ramPercent;
@JsonProperty("load")
private String load;
@JsonProperty("node.role")
private String nodeRole;
@JsonProperty("master")
private String master;
@JsonProperty("name")
private String name;
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

@JsonProperty("host")
public String getHost() {
return host;
}

@JsonProperty("host")
public void setHost(String host) {
this.host = host;
}

@JsonProperty("ip")
public String getIp() {
return ip;
}

@JsonProperty("ip")
public void setIp(String ip) {
this.ip = ip;
}

@JsonProperty("heap.percent")
public String getHeapPercent() {
return heapPercent;
}

@JsonProperty("heap.percent")
public void setHeapPercent(String heapPercent) {
this.heapPercent = heapPercent;
}

@JsonProperty("ram.percent")
public String getRamPercent() {
return ramPercent;
}

@JsonProperty("ram.percent")
public void setRamPercent(String ramPercent) {
this.ramPercent = ramPercent;
}

@JsonProperty("load")
public String getLoad() {
return load;
}

@JsonProperty("load")
public void setLoad(String load) {
this.load = load;
}

@JsonProperty("node.role")
public String getNodeRole() {
return nodeRole;
}

@JsonProperty("node.role")
public void setNodeRole(String nodeRole) {
this.nodeRole = nodeRole;
}

@JsonProperty("master")
public String getMaster() {
return master;
}

@JsonProperty("master")
public void setMaster(String master) {
this.master = master;
}

@JsonProperty("name")
public String getName() {
return name;
}

@JsonProperty("name")
public void setName(String name) {
this.name = name;
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
