package art.aelaort.service.mappers;

import art.aelaort.models.servers.ssh.SshConfigServer;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;

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
							getNameByConfigEntry(e, serverNamesByHost),
							e.getHostName(),
							key
									.replace(sshCommonConfigKeyFilePrefix, "")
									.replace(".pub", ""),
							e.getPort()
					);
				})
				.toList();
	}

	private String getNameByConfigEntry(HostConfigEntry e, Map<String, String> serverNamesByHost) {
		String string = serverNamesByHost.get(e.getHost());
		if (string == null) {
			if (StringUtils.hasText(e.getProxyJump())) {
				log(wrapRed("ssh конфиг - %s не имеет полного имени".formatted(e.getHost())));
			}

			return e.getHost();
		}
		return string;
	}
}
