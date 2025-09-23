package art.aelaort.service;

import art.aelaort.exceptions.AppExitErrorException;
import art.aelaort.exceptions.AppPrintUsageException;
import art.aelaort.exceptions.SshNameNotFoundException;
import art.aelaort.utils.ExternalUtilities;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;
import static art.aelaort.utils.Utils.slice;

@Component
@RequiredArgsConstructor
public class Entrypoint implements CommandLineRunner {
	private final ScanShowServersService scanShow;
	private final ExternalUtilities externalUtilities;
	private final GitBundleService gitBundleService;
	private final GitDirCopyService gitDirCopyService;

	@Override
	public void run(String... args) {
		try {
			if (args.length >= 1) {
				switch (args[0]) {
					case "git-bundle-all" -> gitBundleService.bundleAll();
					case "git-dir-copy-all" -> gitDirCopyService.copyAll();
					case "git-dir-copy" -> gitDirCopyService.copy(slice(args, 1));
					case "sync", "s" -> scanShow.sync();
					case "sync-all", "sa" -> scanShow.syncAll();
					default -> log("unknown args\n" + usage());
				}
				externalUtilities.commitInvData();
			} else {
				log("at least one arg required");
				log(usage());
				System.exit(1);
			}
		} catch (SshNameNotFoundException e) {
			log(wrapRed("в ssh-config-names.properties не найден какой-то ssh сервер из конфига :("));
			System.exit(1);
		} catch (AppExitErrorException e) {
			System.exit(1);
		} catch (AppPrintUsageException e) {
			log(usage());
			System.exit(1);
		}
	}

	private String usage() {
		return """
				usage:
					sync, s          - quick sync
					sync-all, sa     - long sync all data
					git-bundle-all   - bundle all repos to another dir, no save timestamp
					git-dir-copy     - copy .git from bundle to dir with replace
									   1 - dir with .git
									   2 - bundle path
					git-dir-copy-all - execute git-dir-copy for work dir, auto find repos and bundles""";
	}
}
