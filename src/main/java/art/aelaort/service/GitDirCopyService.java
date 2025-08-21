package art.aelaort.service;

import art.aelaort.exceptions.AppExitErrorException;
import art.aelaort.exceptions.AppPrintUsageException;
import art.aelaort.utils.Utils;
import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class GitDirCopyService {
	private final SystemProcess systemProcess;
	private final Utils utils;
	private static final String DOT_GIT = ".git";

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

		FileUtils.deleteQuietly(targetGit.toFile());
		copyDirectory(targetProjectDir.resolve(DOT_GIT), bundleGitDirOp.get());
		callGitAdd(tmp);

		FileUtils.deleteQuietly(tmp.toFile());
	}

	private static void copyDirectory(Path targetProjectDir, Path bundleGitDir) {
		try {
			FileUtils.copyDirectory(bundleGitDir.toFile(), targetProjectDir.toFile());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

	private void callGitAdd(Path tmp) {
		systemProcess.callProcessThrows(tmp, "git add .");
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
