package appdynamics.zookeeper.monitor.es.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import appdynamics.zookeeper.monitor.api.ESSnapshotRestoreService;
import appdynamics.zookeeper.monitor.configuration.HaesNodetypeProperties;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component

public class ESSnapshotRestoreScheduler {

	@Autowired
	private ESSnapshotRestoreService esSnapshotRestoreService;

	@Autowired
	private HaesNodetypeProperties haesNodetypeProperties;

	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	@Scheduled(cron = "${es.snapshot.cron}")
	// @Scheduled(cron = "0 * * * * ?")
	public void scheduleSnapshotTaskWithCronExpression() {
		if (!haesNodetypeProperties.getType().equals("arbitrator")) {
			log.info("scheduleSnapshotTaskWithCronExpression/Execution Snapshot request at: {}", dateTimeFormatter.format(LocalDateTime.now()));
			log.info("Current Thread : {}", Thread.currentThread().getName());
			if (esSnapshotRestoreService.doSnapShot()) {
				log.info("scheduleSnapshotTaskWithCronExpression/ Snapshot execution successfullly. Ended at: {}", dateTimeFormatter.format(LocalDateTime.now()));
			} else {
				log.info("scheduleSnapshotTaskWithCronExpression/ Snapshot execution not successfull. Ended at: {}", dateTimeFormatter.format(LocalDateTime.now()));
			}
		}
	}

	@Scheduled(cron = "${es.restore.cron}")
	// @Scheduled(cron = "0 * * * * ?")
	public void scheduleRestoreTaskWithCronExpression() {
		if (!haesNodetypeProperties.getType().equals("arbitrator")) {
			log.info("scheduleRestoreTaskWithCronExpression/Execution Restore request at: {}", dateTimeFormatter.format(LocalDateTime.now()));
			log.info("Current Thread : {}", Thread.currentThread().getName());
			if (esSnapshotRestoreService.doRestore()) {
				log.info("scheduleRestoreTaskWithCronExpression/ Restore execution successfullly. Ended at: {}", dateTimeFormatter.format(LocalDateTime.now()));
			} else {
				log.info("scheduleRestoreTaskWithCronExpression/ Restore execution not successfull. Ended at: {}", dateTimeFormatter.format(LocalDateTime.now()));
			}
		}
	}

	@Scheduled(cron = "${es.restore.cron}")
	// @Scheduled(cron = "0 * * * * ?")
	public void scheduleFilesystemRepositoryCheckTaskWithCronExpression() {
		if (!haesNodetypeProperties.getType().equals("arbitrator")) {
			log.info("scheduleFilesystemRepositoryCheckTaskWithCronExpression/Execution Check FS/Mount request at: {}", dateTimeFormatter.format(LocalDateTime.now()));
			log.info("Current Thread : {}", Thread.currentThread().getName());
			if (esSnapshotRestoreService.doCheckRepository()) {
				log.info("scheduleFilesystemRepositoryCheckTaskWithCronExpression/ Check FS/Mount execution successfullly. Ended at: {}", dateTimeFormatter.format(LocalDateTime.now()));
			} else {
				log.info("scheduleFilesystemRepositoryCheckTaskWithCronExpression/ Check FS/Mount execution not successfull. Ended at: {}", dateTimeFormatter.format(LocalDateTime.now()));
			}
		}
	}
}
