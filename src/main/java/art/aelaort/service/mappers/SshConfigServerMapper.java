package art.aelaort.service.mappers;

import art.aelaort.models.servers.ssh.SshConfigServer;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class SshConfigServerMapper {
	public List<SshConfigServer> map(List<HostConfigEntry> configEntries) {
		return configEntries.stream()
				.map(e -> {
					String key = "";
					String name = e.getHost();
					if (!e.getIdentities().isEmpty()) {
						String keyFile = e.getIdentities().iterator().next();
						key = keyFile;
						name = Path.of(keyFile).getFileName().toString().split("\\.")[0];
					}
					return new SshConfigServer(
							name,
							e.getHostName(),
							key,
							e.getPort()
					);
				})
				.toList();
	}
}
