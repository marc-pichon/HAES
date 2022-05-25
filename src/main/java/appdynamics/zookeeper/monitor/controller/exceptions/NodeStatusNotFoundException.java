package appdynamics.zookeeper.monitor.controller.exceptions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NodeStatusNotFoundException extends RuntimeException {

	public NodeStatusNotFoundException(String exception) {
		super(exception);
	}

}