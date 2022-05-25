package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties("cluster")
public class Cluster2TypeProperties {

    private String cluster2type;
 
 
    public String getcluster2type() {
        return cluster2type;
    }

    public void setcluster2type(String cluster2type) {
        this.cluster2type = cluster2type;
    }

   
    @Override
    public String toString() {
        return "{" +
                "cluster type='" + cluster2type + 
                '}';
    }
}

