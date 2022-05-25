package appdynamics.zookeeper.monitor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("sr.script")
public class SRScriptsProperties {

    private String rootlocation;
    private String eventservicelocation;
    private String snapshotrepositorylocation;
    private String daemonscript;
    private String user;
    private String filesystemtype;
    private String rsynctargethostdns;

    public String getRsynctargethostdns() {
        return rsynctargethostdns;
    }

    public void setRsynctargethostdns(String rsynctargethostdns) {
        this.rsynctargethostdns = rsynctargethostdns;
    }

    public String getSnapshotrepositorylocation() {
        return snapshotrepositorylocation;
    }

    public void setSnapshotrepositorylocation(String snapshotrepositorylocation) {
        this.snapshotrepositorylocation = snapshotrepositorylocation;
    }

    public String getFilesystemtype() {
        return filesystemtype;
    }

    public void setFilesystemtype(String filesystemtype) {
        this.filesystemtype = filesystemtype;
    }

    public String getsnapshotrepositorylocation() {
        return getSnapshotrepositorylocation();
    }

    public void setsnapshotrepositorylocation(String snapshotrepositorylocation) {
        this.setSnapshotrepositorylocation(snapshotrepositorylocation);
    }

    public String getrootlocation() {
        return rootlocation;
    }

    public void setrootlocation(String rootlocation) {
        this.rootlocation = rootlocation;
    }

    public String geteventservicelocation() {
        return eventservicelocation;
    }

    public void seteventservicelocation(String eventservicelocation) {
        this.eventservicelocation = eventservicelocation;
    }

    public String getdaemonscript() {
        return daemonscript;
    }

    public void setdaemonscript(String daemonscript) {
        this.daemonscript = daemonscript;
    }

    public String getuser() {
        return user;
    }

    public void setuser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "SnapshotRestoreProperties{"
                + "eventservicelocation='" + eventservicelocation
                + ", snapshotrepositorylocation='" + snapshotrepositorylocation
                + ", rootlocation='" + rootlocation
                + ", daemonscript='" + daemonscript
                + ", user='" + user
                + ", filesystemtype='" + filesystemtype
                + ", rsynctargethostdns='" + rsynctargethostdns
                + '}';
    }
}
