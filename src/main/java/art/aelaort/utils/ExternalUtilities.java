package art.aelaort.utils;

import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ExternalUtilities {
	private final SystemProcess systemProcess;
	@Value("${cicd.inv-data.dir}")
	private Path invDataDir;

	public void commitInvData() {
		systemProcess.callProcess(invDataDir, "git commit -am \"wip\"");
	}

	public void dirSync() {
		Response response = systemProcess.callProcessInheritFilteredStdout(
				stdout -> !stdout.contains("/.git/")
						  && !stdout.contains("Completed "), "workdir-sync.bat");

		if (response.exitCode() != 0) {
			throw new RuntimeException("dir sync error \n%s\n%s".formatted(response.stderr(), response.stdout()));
		}

		List<String> projects = getProjects(response.stdout());
		int projectsCount = projects.size();
		if (projectsCount > 0) {
			log();
			log("synced %d .idea projects:\n", projectsCount);
			projects.forEach(Utils::log);
			log();
		}

		int gitRows = StringUtils.countMatches(response.stdout(), "/.git/");
		if (gitRows > 0) {
			log("also synced .git: %d paths\n", gitRows);
		}
	}

	private List<String> getProjects(String stdout) {
		String prefix = "upload: ";
		String file = ".idea\\workspace.xml";
		String fileRx = file.replace("\\", "\\\\");

		String[] lines = stdout
				.replace(prefix, "\n" + prefix)
				.split("\\n");
		List<String> projectsLines = Stream.of(lines)
				.filter(line -> line.startsWith(prefix))
				.filter(line -> line.contains(file))
				.toList();
		List<String> projects = new ArrayList<>(projectsLines.size());
		for (String line : projectsLines) {
			String[] split = line.split(fileRx);
			String name = split[0]
					.replaceAll("/$", "")
					.replaceAll("\\\\$", "")
					.replace(prefix, "- ");
			projects.add(name);
		}
		return projects;
	}
}
