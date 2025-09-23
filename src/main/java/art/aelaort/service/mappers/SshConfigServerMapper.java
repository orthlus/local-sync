package art.aelaort.service.mappers;

import art.aelaort.exceptions.SshNameNotFoundException;
import art.aelaort.models.servers.ssh.SshConfigServer;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SshConfigServerMapper {
	@Value("${ssh.common.config.key_file_prefix}")
	private String sshCommonConfigKeyFilePrefix;

	public List<SshConfigServer> map(List<HostConfigEntry> configEntries, Map<String, String> serverNamesByHost) {
		return configEntries.stream()
				.map(e -> {
					String key = "";
					if (!e.getIdentities().isEmpty()) {
						key = e.getIdentities().iterator().next();
					}
					return new SshConfigServer(
							getNameByHost(e.getHost(), serverNamesByHost),
							e.getHostName(),
							key
									.replace(sshCommonConfigKeyFilePrefix, "")
									.replace(".pub", ""),
							e.getPort()
					);
				})
				.toList();
	}

	private String getNameByHost(String host, Map<String, String> serverNamesByHost) {
		String string = serverNamesByHost.get(host);
		if (string == null) {
			throw new SshNameNotFoundException();
		}
		return string;
	}
}
