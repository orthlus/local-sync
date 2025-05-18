package art.aelaort.service.providers;

import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.servers.yaml.TabbyFile;
import art.aelaort.service.mappers.TabbyMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TabbyServerProvider {
	private final YAMLMapper yamlMapper;
	private final TabbyMapper tabbyMapper;
	@Value("${tabby.config.path}")
	private Path tabbyConfigPath;
	@Value("${tabby.config.inner-path}")
	private Path tabbyConfigInnerPath;

	public List<TabbyServer> readLocalInner() {
		TabbyFile tabbyFile = parseFile(tabbyConfigInnerPath);
		return tabbyMapper.map(tabbyFile);
	}

	@SneakyThrows
	private TabbyFile parseFile(Path tabbyConfigPath) {
		return yamlMapper.readValue(tabbyConfigPath.toFile(), TabbyFile.class);
	}
}
