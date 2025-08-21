package art.aelaort.service;

import art.aelaort.exceptions.AppExitErrorException;
import art.aelaort.exceptions.AppPrintUsageException;
import art.aelaort.utils.Utils;
import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.GitUtils.copyDirectory;
import static art.aelaort.utils.GitUtils.deleteDir;
import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class GitDirCopyService {
	private final SystemProcess systemProcess;
	private final Utils utils;
	private static final String DOT_GIT = ".git";

	public void copyAll() {
		// TODO обходить все папки в root.dir, искать репы, для каждой искать бандл в git.bundles.all-dir и вызывать copy()
	}

	public void copy(String[] args) {
		if (args.length != 2) {
			log(wrapRed("Wrong number of arguments"));
			throw new AppPrintUsageException();
		}

		try {
			copy(Path.of(args[0]), Path.of(args[1]));
		} catch (InvalidPathException e) {
			log(wrapRed("Invalid path"));
			throw new AppPrintUsageException();
		}
	}

	public void copy(Path targetProjectDir, Path bundlePath) {
		checkParams(targetProjectDir, bundlePath);

		Path targetGit = targetProjectDir.resolve(DOT_GIT);
		if (Files.notExists(targetGit)) {
			log(wrapRed("target .git not exists"));
			throw new AppExitErrorException();
		}

		Path tmp = utils.createTmpDir();
		Response response = extractBundle(bundlePath, tmp);
		if (response.exitCode() != 0) {
			log(wrapRed("git clone error"));
			throw new AppExitErrorException();
		}

		Optional<Path> bundleGitDirOp = getGitDir(tmp, bundlePath);
		if (bundleGitDirOp.isEmpty()) {
			log(wrapRed("bundle git extract error"));
			throw new AppExitErrorException();
		}

		deleteDir(targetGit);
		copyDirectory(bundleGitDirOp.get(), targetProjectDir.resolve(DOT_GIT));
		callGitAdd(targetProjectDir);
		updateGitRemoteUrl(targetProjectDir, "");

		deleteDir(tmp);
	}

	private void updateGitRemoteUrl(Path tmp, String url) {
		systemProcess.callProcessThrows(tmp, "git remote set-url origin " + url);
	}

	private void callGitAdd(Path tmp) {
		systemProcess.callProcessThrows(tmp, "git add .");
	}

	private static Optional<Path> getGitDir(Path tmp, Path bundlePath) {
		String projectName = FilenameUtils.removeExtension(bundlePath.getFileName().toString());
		Path projectDir = tmp.resolve(projectName);
		if (Files.notExists(projectDir)) {
			return Optional.empty();
		}

		Path gitDir = projectDir.resolve(DOT_GIT);
		if (Files.notExists(gitDir)) {
			return Optional.empty();
		}
		return Optional.of(gitDir);
	}

	private Response extractBundle(Path bundlePath, Path tmp) {
		return systemProcess.callProcess(tmp, "git clone --no-checkout " + bundlePath.toString());
	}

	private static void checkParams(Path gitDir, Path bundlePath) {
		if (Files.notExists(gitDir)) {
			log(wrapRed("not found dir " + gitDir));
			throw new AppExitErrorException();
		}
		if (Files.notExists(bundlePath)) {
			log(wrapRed("not found file " + bundlePath));
			throw new AppExitErrorException();
		}
	}
}
