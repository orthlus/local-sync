package art.aelaort.service.providers;

import art.aelaort.models.servers.ssh.SshConfigServer;
import art.aelaort.service.mappers.SshConfigServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SshConfigServerProvider {
	private final SshConfigServerMapper sshConfigServerMapper;
	@Value("${ssh.common.config}")
	private Path sshCommonConfig;

	public List<SshConfigServer> readLocal() {
		List<HostConfigEntry> hostConfigEntries = parseFile(sshCommonConfig);
		return sshConfigServerMapper.map(hostConfigEntries);
	}

	@SneakyThrows
	private List<HostConfigEntry> parseFile(Path configPath) {
		return HostConfigEntry.readHostConfigEntries(configPath);
	}
}
