package art.aelaort.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Component
public class GitBundleService {
	@Value("${root.dir}")
	private String rootDir;
	private final String gitCommand = "git show --no-patch --format=%ct";

	private LocalDateTime parseGitDate(String date) {
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(date)), TimeZone.getDefault().toZoneId());
	}
}
