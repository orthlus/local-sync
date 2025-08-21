package art.aelaort.service;

import art.aelaort.utils.GitUtils;
import art.aelaort.utils.Utils;
import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapGreen;
import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.GitUtils.*;
import static art.aelaort.utils.Utils.log;

@SuppressWarnings("FieldCanBeLocal")
@Component
@RequiredArgsConstructor
public class GitBundleService {
	private final SystemProcess systemProcess;
	private final ObjectMapper prettyObjectMapper;
	private final Utils utils;
	@Value("${root.dir}")
	private Path rootDir;
	@Value("${git.bundles.dir}")
	private Path bundlesDir;
	@Value("${git.bundles.all-dir}")
	private Path allBundlesDir;
	@Value("${git.bundles.exclude.prefix-1}")
	private String excludePrefix1;
	@Value("${git.bundles.exclude.prefix-2}")
	private String excludePrefix2;
	@Value("${git.bundles.last-git-time-sync-file}")
	private Path lastSyncFile;
	@Value("${git.bundles.remotes-urls-by-bundle-file}")
	private Path remotesUrlsByBundleFile;
	private final String gitLastCommitTimestampCommand = "git show --no-patch --format=%ct";
	private final String gitBundleCommand = "git bundle create %s --all";
	private final String gitGetRemoteUrlCommand = "git config --get remote.origin.url";

	public void bundleAll() {
		Path tmp = utils.createTmpDir();

		List<Path> gitRepos = getGitReposAll();

		makeBundles(gitRepos, tmp, true);
//		makeBundles(gitRepos, allBundlesDir);

		deleteDir(allBundlesDir);
		mkdir(allBundlesDir);
		GitUtils.copyDirectory(tmp, allBundlesDir);
		log(wrapGreen("git bundles created"));

		deleteDir(tmp);
	}

	public void makeBundles() {
		List<Path> gitRepos = getGitRepos();
		Set<String> currentBundles = getCurrentBundles();
		gitRepos = filterGitRepos(gitRepos, currentBundles);
		makeBundles(gitRepos, bundlesDir, false);
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

	private void makeBundles(List<Path> gitRepos, Path bundlesDir, boolean log) {
		mkdir(bundlesDir);
		Map<String, String> gitRemotesByBundleName = new HashMap<>();

		for (Path gitRepo : gitRepos) {
			String bundleName = bundleName(gitRepo);
			String bundleCommand = gitBundleCommand.formatted(bundlesDir.resolve(bundleName));

			Response response = systemProcess.callProcess(gitRepo, bundleCommand);
			if (response.exitCode() != 0) {
				log(wrapRed("call git bundle with error:\n") + "%s\n%s".formatted(response.stderr(), response.stdout()));
			}

			Response gitRemoteResp = systemProcess.callProcess(gitRepo, gitGetRemoteUrlCommand);
			if (gitRemoteResp.exitCode() != 0) {
				gitRemotesByBundleName.put(bundleName, gitRemoteResp.stdout().strip());
			} else {
				gitRemotesByBundleName.put(bundleName, null);
			}
			if (log) {
				log(wrapGreen("bundle %s created".formatted(bundleName)));
			}
		}

		saveRemotesMap(gitRemotesByBundleName);
	}

	private List<Path> filterGitRepos(List<Path> gitRepos, Set<String> currentBundles) {
		LocalDateTime lastSyncTime = getLastSyncTime();
		List<Path> filteredGitRepos = new ArrayList<>(gitRepos.size());
		for (Path gitRepo : gitRepos) {
			if (!currentBundles.contains(bundleName(gitRepo))) {
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

	private List<Path> getGitReposAll() {
		try (Stream<Path> walk = Files.walk(rootDir, 6)) {
			return walk
					.filter(Files::isDirectory)
					.filter(path -> path.resolve(".git").toFile().exists())
					.toList();
		} catch (IOException e) {
			log(wrapRed("Error getting git repositories"));
			return List.of();
		}
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

	private String bundleName(Path gitRepo) {
		String parent = gitRepo.getParent().getFileName().toString().replace(" ", "-");
		return "%s--%s.bundle".formatted(parent, gitRepo.getFileName());
	}

	@SneakyThrows
	private void saveRemotesMap(Map<String, String> gitRemotesByBundleName) {
		Files.writeString(remotesUrlsByBundleFile, prettyObjectMapper.writeValueAsString(gitRemotesByBundleName));
	}

	@SneakyThrows
	private void saveTimestamp() {
		Files.writeString(lastSyncFile, String.valueOf(Instant.now().getEpochSecond()));
	}

	@SneakyThrows
	private LocalDateTime getLastSyncTime() {
		if (Files.exists(lastSyncFile)) {
			return parseGitDate(Files.readString(lastSyncFile).trim());
		} else {
			return LocalDateTime.now();
		}
	}
}
