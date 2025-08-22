package art.aelaort.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;

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

	public static String bundleName(Path gitRepo) {
		String parent = gitRepo.getParent().getFileName().toString().replace(" ", "-");
		return "%s--%s.bundle".formatted(parent, gitRepo.getFileName());
	}

	public static Path unbundleName(String bundleName) {
		if (bundleName.contains("--")) {
			String[] split = bundleName.split("--");
			if (split.length > 2) {
				throw new UnsupportedOperationException("bundle name cannot have more than 2 parts");
			}

			return Path.of(split[0], split[1].replaceAll("\\.bundle$", ""));
		} else {
			return Path.of(bundleName);
		}
	}

	public static List<Path> getGitReposAll(Path rootDir, int depth) {
		try (Stream<Path> walk = Files.walk(rootDir, depth)) {
			return walk
					.filter(Files::isDirectory)
					.filter(path -> path.resolve(".git").toFile().exists())
					.toList();
		} catch (IOException e) {
			log(wrapRed("Error getting git repositories"));
			return List.of();
		}
	}
}
