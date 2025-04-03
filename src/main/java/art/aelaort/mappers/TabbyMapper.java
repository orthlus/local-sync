package art.aelaort.mappers;

import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.servers.yaml.TabbyFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TabbyMapper {
	@Value("${tabby.config.rsa_file_prefix}")
	private String tabbyConfigRsaFilePrefix;

	public List<TabbyServer> map(TabbyFile tabbyFile) {
		return tabbyFile.getProfiles()
				.stream()
				.filter(profile -> profile.getType().equals("ssh"))
				.map(profile -> {
					Integer filePort = profile.getOptions().getPort();
					return new TabbyServer(
							profile.getName(),
							profile.getOptions().getHost(),
							privateKey(profile).replace(tabbyConfigRsaFilePrefix, ""),
							filePort != null ? filePort : 22
					);
				})
				.toList();
	}

	private String privateKey(TabbyFile.Profile profile) {
		List<String> privateKeys = profile.getOptions().getPrivateKeys();
		if (privateKeys != null && !privateKeys.isEmpty()) {
			return privateKeys.get(0);
		}
		return "";
	}
}
