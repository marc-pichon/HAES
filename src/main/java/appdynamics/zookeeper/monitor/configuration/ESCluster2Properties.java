package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties("esnode")
public class ESCluster2Properties {

    private List<Cluster2> cluster2 = new ArrayList<>();

    public static class Cluster2 {
        private String ip;
        private String port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "ESCluster2{" +
                    "ip='" + ip + '\'' +
                    ", port='" + port + '\'' +
                    '}';
        }
    }

    
 
    public List<Cluster2> getCluster2() {
        return cluster2;
    }

    public void setCluster2(List<Cluster2> escluster) {
        this.cluster2 = escluster;
    }

    @Override
    public String toString() {
        return "ESCluster2Properties{" +
                ", ES Cluster2=" + cluster2 +
                '}'
                ;
    }

}

