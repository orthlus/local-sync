package art.aelaort.service.providers;

import art.aelaort.models.servers.ssh.SshConfigServer;
import art.aelaort.service.mappers.SshConfigServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SshConfigServerProvider {
	private final SshConfigServerMapper sshConfigServerMapper;
	@Value("${ssh.common.config}")
	private Path sshCommonConfig;
	@Value("${ssh.common.config-names}")
	private Path sshCommonConfigNames;


	public List<SshConfigServer> readLocal() {
		List<HostConfigEntry> hostConfigEntries = parseFile(sshCommonConfig);
		Map<String, String> serverNamesByHost = readHostNames(sshCommonConfigNames);
		return sshConfigServerMapper.map(hostConfigEntries, serverNamesByHost);
	}

	@SneakyThrows
	private List<HostConfigEntry> parseFile(Path configPath) {
		return HostConfigEntry.readHostConfigEntries(configPath);
	}

	@SneakyThrows
	private Map<String, String> readHostNames(Path configNamesPath) {
		try (Stream<String> lines = Files.lines(configNamesPath)) {
			return lines
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.map(line -> line.split("="))
					.collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
		}
	}
}
