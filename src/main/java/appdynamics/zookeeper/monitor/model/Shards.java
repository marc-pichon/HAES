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
"total",
"failed",
"successful"
})
public class Shards {

@JsonProperty("total")
private Integer total;
@JsonProperty("failed")
private Integer failed;
@JsonProperty("successful")
private Integer successful;
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

@JsonProperty("total")
public Integer getTotal() {
return total;
}

@JsonProperty("total")
public void setTotal(Integer total) {
this.total = total;
}

public Shards withTotal(Integer total) {
this.total = total;
return this;
}

@JsonProperty("failed")
public Integer getFailed() {
return failed;
}

@JsonProperty("failed")
public void setFailed(Integer failed) {
this.failed = failed;
}

public Shards withFailed(Integer failed) {
this.failed = failed;
return this;
}

@JsonProperty("successful")
public Integer getSuccessful() {
return successful;
}

@JsonProperty("successful")
public void setSuccessful(Integer successful) {
this.successful = successful;
}

public Shards withSuccessful(Integer successful) {
this.successful = successful;
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

public Shards withAdditionalProperty(String name, Object value) {
this.additionalProperties.put(name, value);
return this;
}

}
