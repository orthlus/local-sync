package art.aelaort.service;

import art.aelaort.exceptions.AppExitErrorException;
import art.aelaort.exceptions.AppPrintUsageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;

@Component
public class GitDirCopyService {
	@Value("${tmp.root.dir}")
	private Path tmpDir;

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

	public void copy(Path gitDir, Path bundlePath) {
		checkParams(gitDir, bundlePath);

		
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
