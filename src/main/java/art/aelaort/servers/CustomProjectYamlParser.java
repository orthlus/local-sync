package art.aelaort.servers;

import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.ServiceDto;
import art.aelaort.models.servers.yaml.CustomFile;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomProjectYamlParser {
	private final YAMLMapper yamlMapper;
	@Value("${servers.management.files.monitoring}")
	private String monitoringFile;

	public DirServer parseCustomYmlFile(Path ymlFile) {
		try {
			Path serverDir = ymlFile.getParent();
			boolean monitoring = serverDir.resolve(monitoringFile).toFile().exists();
			List<String> projects = yamlMapper.readValue(ymlFile.toFile(), CustomFile.class).getProjects();
			List<ServiceDto> services = new ArrayList<>();
			for (String project : projects) {
				services.add(ServiceDto.builder()
						.service(project)
						.ymlName(ymlFile.getFileName().toString())
						.build());
			}
			return DirServer.builder()
					.name(serverDir.getFileName().toString())
					.monitoring(monitoring)
					.services(services)
					.build();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
