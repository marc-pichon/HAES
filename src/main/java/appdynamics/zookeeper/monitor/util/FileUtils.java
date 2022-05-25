package appdynamics.zookeeper.monitor.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class FileUtils {

	public static boolean isStorageAvailable(String dir, String clustertype) {
		log.info("isStorageAvailable/parameters: dir: " + dir + ", clustertype: "+ clustertype);
		try {
			Path path  = Paths.get(new URI("file:///".concat(dir)));
			
			boolean rc_ok = true;
			
			if (!Files.exists(path)) {
				log.error("Snapshot repository store '{}' does not exist.", dir);
				return false;
			}
			
			FileStore fileStore = Files.getFileStore(path);

			long usableSpaceMb = fileStore.getUsableSpace() / (1024 * 1024);
			log.info("isStorageAvailable/usableSpaceMb: " + usableSpaceMb);
			
			if (clustertype.contentEquals("active") && fileStore.isReadOnly()) {
				log.error("Snapshot repository store '{}' is not writable for active cluster.", dir);
				rc_ok = false;
			}
			if (usableSpaceMb  < 100) { // if space available is less than 100 Mb
				log.warn("Snapshot repository has less than 100 Mb available.");
				rc_ok = false;
			}

			return rc_ok;
		}
		catch ( URISyntaxException e) {
			log.error("isStorageAvailable/URI internal error using '" + dir + "', exception: " +  e.getLocalizedMessage());
			return false;
		}
		catch (IOException e) {
			log.debug("Snapshot repository store '{}' is not writable.", dir, e);
		}
		//default
		return false;
	}
}
