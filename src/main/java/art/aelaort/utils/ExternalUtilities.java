package art.aelaort.utils;

import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ExternalUtilities {
	private final SystemProcess systemProcess;

	public void dirSync() {
		Response response = systemProcess.callProcessInheritFilteredStdout(
				stdout -> !stdout.contains("/.git/")
						  && !stdout.contains("Completed ")
						  && !stdout.contains(".idea\\workspace.xml"), "workdir-sync.bat");

		if (response.exitCode() != 0) {
			throw new RuntimeException("dir sync error \n%s\n%s".formatted(response.stderr(), response.stdout()));
		}

		int projectsCount = StringUtils.countMatches(response.stdout(), ".idea\\workspace.xml");
		if (projectsCount > 0) {
			log("also synced .idea projects: %d projects\n", projectsCount);
		}

		int gitRows = StringUtils.countMatches(response.stdout(), "/.git/");
		if (gitRows > 0) {
			log("also synced .git: %d paths\n", gitRows);
		}
	}
}
