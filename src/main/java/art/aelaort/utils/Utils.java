package art.aelaort.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class Utils {
	@Value("${servers.management.docker.image.pattern}")
	private String dockerImagePattern;
	private String[] dockerImagePatternSplit;

	@PostConstruct
	private void init() {
		dockerImagePatternSplit = dockerImagePattern.split("%%");
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
				.replace(dockerImagePatternSplit[0], "")
				.replace(dockerImagePatternSplit[1], "");
	}
}
