package art.aelaort.service;

import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapGreen;
import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;

@SuppressWarnings("FieldCanBeLocal")
@Component
@RequiredArgsConstructor
public class GitBundleService {
	private final SystemProcess systemProcess;
	@Value("${root.dir}")
	private Path rootDir;
	@Value("${git.bundles.dir}")
	private Path bundlesDir;
 	@Value("${git.bundles.exclude.prefix-1}")
	private String excludePrefix1;
	@Value("${git.bundles.exclude.prefix-2}")
	private String excludePrefix2;
	@Value("${git.bundles.last-git-time-sync-file}")
	private Path lastSyncFile;
	private final String gitLastCommitTimestampCommand = "git show --no-patch --format=%ct";
	private final String gitBundleCommand = "git bundle create %s --all";

	public void makeBundles() {
		List<Path> gitRepos = getGitRepos();
		Set<String> currentBundles = getCurrentBundles();
		gitRepos = filterGitRepos(gitRepos, currentBundles);
		makeBundles(gitRepos);
		saveTimestamp();
		log(wrapGreen("git bundles created"));
	}

	private Set<String> getCurrentBundles() {
		try (Stream<Path> list = Files.list(bundlesDir)) {
			return list
					.map(Path::getFileName)
					.map(Path::toString)
					.filter(string -> string.endsWith(".bundle"))
					.collect(Collectors.toSet());
		} catch (IOException e) {
			log(wrapRed("Error getting current git bundles"));
			return Set.of();
		}
	}

	private void saveTimestamp() {
		try {
			Files.writeString(lastSyncFile, String.valueOf(Instant.now().getEpochSecond()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void makeBundles(List<Path> gitRepos) {
		for (Path gitRepo : gitRepos) {
			String bundleName = gitRepo.getFileName().toString() + ".bundle";
			String bundleCommand = gitBundleCommand.formatted(bundlesDir.resolve(bundleName));
			Response response = systemProcess.callProcess(gitRepo, bundleCommand);
			if (response.exitCode() != 0) {
				log(wrapRed("call git bundle with error:\n") + "%s\n%s".formatted(response.stderr(), response.stdout()));
			}
		}
	}

	private List<Path> filterGitRepos(List<Path> gitRepos, Set<String> currentBundles) {
		LocalDateTime lastSyncTime = getLastSyncTime();
		List<Path> filteredGitRepos = new ArrayList<>(gitRepos.size());
		for (Path gitRepo : gitRepos) {
			String bundleName = gitRepo.getFileName().toString() + ".bundle";
			if (!currentBundles.contains(bundleName)) {
				filteredGitRepos.add(gitRepo);
				continue;
			}
			Response response = systemProcess.callProcess(gitRepo, gitLastCommitTimestampCommand);
			if (response.exitCode() != 0) {
				log(wrapRed("call git with error:\n") + "%s\n%s".formatted(response.stderr(), response.stdout()));
			} else {
				LocalDateTime lastCommitDateTime = parseGitDate(response.stdout().trim());
				if (lastSyncTime.isBefore(lastCommitDateTime)) {
					filteredGitRepos.add(gitRepo);
				}
			}
		}
		return filteredGitRepos;
	}

	private List<Path> getGitRepos() {
		try (Stream<Path> walk = Files.walk(rootDir, 6)) {
			return walk
					.filter(Files::isDirectory)
					.filter(path -> !path.toString().contains(excludePrefix1))
					.filter(path -> !path.toString().contains(excludePrefix2))
					.filter(path -> path.resolve(".git").toFile().exists())
					.toList();
		} catch (IOException e) {
			log(wrapRed("Error getting git repositories"));
			return List.of();
		}
	}

	private LocalDateTime getLastSyncTime() {
		try {
			if (Files.exists(lastSyncFile)) {
				return parseGitDate(Files.readString(lastSyncFile).trim());
			} else {
				return LocalDateTime.now();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private LocalDateTime parseGitDate(String date) {
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(date)), TimeZone.getDefault().toZoneId());
	}
}
