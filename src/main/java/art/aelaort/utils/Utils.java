package art.aelaort.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

@Component
public class Utils {
	@Value("${servers.management.docker-reg-name-1}")
	private String dockerImagePattern1;
	@Value("${servers.management.docker-reg-name-2}")
	private String dockerImagePattern2;
	@Value("${servers.management.docker-reg-name-1.replacement}")
	private String dockerImagePattern1Replacement;
	@Value("${servers.management.docker-reg-name-2.replacement}")
	private String dockerImagePattern2Replacement;
	@Value("${tmp.root.dir}")
	private Path tmpRootDir;

	public Path createTmpDir() {
		return createTmpDir(tmpRootDir);
	}

	public static Path createTmpDir(Path tmpRootDir) {
		try {
			Path path = tmpRootDir.resolve(UUID.randomUUID().toString());
			Files.createDirectory(path);
			return path;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void log(String format, Object... args) {
		System.out.printf(format, args);
	}

	public static void log(String message) {
		System.out.println(message);
	}

	public static void log() {
		System.out.println();
	}

	public static String[] slice(String[] arr, int start) {
		return Arrays.copyOfRange(arr, start, arr.length);
	}

	public static String[] slice(String[] arr, int start, int end) {
		return Arrays.copyOfRange(arr, start, end);
	}

	public String dockerImageClean(String dockerImage) {
		return dockerImage
				.replace(":latest", "")
				.replace(dockerImagePattern1, dockerImagePattern1Replacement)
				.replace(dockerImagePattern2, dockerImagePattern2Replacement);
	}
}
