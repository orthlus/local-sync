package art.aelaort.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

@UtilityClass
public class GitUtils {
	@SneakyThrows
	public static void copyDirectory(Path src, Path target) {
		FileUtils.copyDirectory(src.toFile(), target.toFile());
	}

	@SneakyThrows
	public static void mkdir(Path bundlesDir) {
		if (Files.notExists(bundlesDir)) {
			Files.createDirectory(bundlesDir);
		}
	}

	public static LocalDateTime parseGitDate(String date) {
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(date)), TimeZone.getDefault().toZoneId());
	}

	public static void deleteDir(Path dir) {
		FileUtils.deleteQuietly(dir.toFile());
	}
}
