package appdynamics.zookeeper.monitor.es.service.exceptions;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RestTemplateResponseErrorHandler
implements ResponseErrorHandler {
	//@Autowired 
	//@Qualifier("eshealthcheckService")
	//private EShealthcheck eshealthcheck;


@Override
public boolean hasError(ClientHttpResponse httpResponse)
    throws IOException {

    return (
       httpResponse
      .getStatusCode()
      .series() == HttpStatus.Series.CLIENT_ERROR || 
       httpResponse
      .getStatusCode()
      .series() == HttpStatus.Series.SERVER_ERROR);
}

@Override
public void handleError(ClientHttpResponse httpResponse)
    throws IOException {

    if (httpResponse
      .getStatusCode()
      .series() == HttpStatus.Series.SERVER_ERROR) {
    	// handle 5xx errors
		log.info("esNodeHealthcheckRestcall/RestTemplateResponseErrorHandler : SERVER_ERROR: " + httpResponse
			      .getStatusCode().toString());
        /*
         * inhibates any throwing.
         * if local ES calls are failing, they are catched and contolled to give back an application status
         */
		//throw new HttpClientErrorException(httpResponse.getStatusCode());
		
    } else if (httpResponse
      .getStatusCode()
      .series() == HttpStatus.Series.CLIENT_ERROR) {
    	// handle 4xx errors
		log.info("esNodeHealthcheckRestcall/RestTemplateResponseErrorHandler : CLIENT_ERROR: + " + httpResponse
			      .getStatusCode().toString());

        if (httpResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
            //throw new NotFoundException();
        }
    }
    /*
     * forward back the status
     */
    //eshealthcheck.update_escheckstatus(false);
}
}
