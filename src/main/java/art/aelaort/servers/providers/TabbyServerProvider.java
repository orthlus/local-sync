package art.aelaort.servers.providers;

import art.aelaort.mappers.TabbyMapper;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.servers.yaml.TabbyFile;
import art.aelaort.s3.TabbyS3;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class TabbyServerProvider {
	private final TabbyS3 tabbyS3;
	private final YAMLMapper yamlMapper;
	private final TabbyMapper tabbyMapper;
	private final RestTemplate tabbyDecoder;
	@Value("${tabby.config.path}")
	private Path tabbyConfigPath;
	@Value("${tabby.decode.password}")
	private String decodePassword;

	public List<TabbyServer> readRemote() {
		String fileContent = getRemoteFileContent();
		save(fileContent);
		TabbyFile tabbyFile = parseFile(fileContent);
		return tabbyMapper.map(tabbyFile);
	}

	public List<TabbyServer> readLocal() {
		TabbyFile tabbyFile = parseFile(tabbyConfigPath);
		return tabbyMapper.map(tabbyFile);
	}

	@SneakyThrows
	private void save(String fileContent) {
		Files.writeString(tabbyConfigPath, fileContent);
		log("tabby config downloaded");
	}

	@SneakyThrows
	private TabbyFile parseFile(Path tabbyConfigPath) {
		return yamlMapper.readValue(tabbyConfigPath.toFile(), TabbyFile.class);
	}

	@SneakyThrows
	private TabbyFile parseFile(String fileContent) {
		return yamlMapper.readValue(fileContent, TabbyFile.class);
	}

	private String getRemoteFileContent() {
		String downloaded = tabbyS3.download().split("\n")[1];
		return decode(downloaded);
	}

	private String decode(String data) {
		String url = "/decrypt?password={decodePassword}";
		return tabbyDecoder.postForObject(url, new HttpEntity<>(data), String.class, decodePassword);
	}
}
