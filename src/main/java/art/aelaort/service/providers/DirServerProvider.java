package art.aelaort.service.providers;

import art.aelaort.service.DockerComposeParser;
import art.aelaort.models.servers.DirServer;
import art.aelaort.service.CustomProjectYamlParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static art.aelaort.utils.Utils.log;
import static java.lang.Integer.parseInt;

@Component
@RequiredArgsConstructor
public class DirServerProvider {
	private final DockerComposeParser dockerComposeParser;
	private final CustomProjectYamlParser customProjectYamlParser;
	@Value("${servers.management.custom_projects_file}")
	private String projectsYmlFileName;
	@Value("${servers.management.dir}")
	private Path serversDir;
	@Value("${servers.management.prices}")
	private Path pricesPath;
	@Value("${servers.management.files.not_scan}")
	private String notScanFile;
	private Map<String, Integer> pricesMap;
	@Value("${servers.management.files.monitoring}")
	private String monitoringFile;

	@PostConstruct
	private void init() {
		fillPrices();
	}

	@SuppressWarnings("OptionalIsPresent")
	public List<DirServer> scanServersDir() {
		List<DirServer> result = new ArrayList<>();
		for (Path serverDir : scanLocalDirs()) {
			Optional<Path> ymlFile = findYmlFile(serverDir);
			if (ymlFile.isPresent()) {
				Optional<DirServer> dirServer = parseYmlFile(ymlFile.get());
				if (dirServer.isPresent()) {
					result.add(dirServer.get());
				}
			} else {
				result.add(DirServer.builder()
						.name(serverDir.getFileName().toString())
						.monitoring(serverDir.resolve(monitoringFile).toFile().exists())
						.services(List.of())
						.build());
			}
		}

		return result.stream()
				.map(this::enrich)
				.toList();
	}

	private void fillPrices() {
		try {
			pricesMap = Files.readAllLines(pricesPath)
					.stream()
					.collect(Collectors.toMap(
							s -> s.split(",")[0],
							s -> parseInt(s.split(",")[1])
					));
		} catch (IOException e) {
			log("prices file read error");
			throw new RuntimeException(e);
		}
	}

	private DirServer enrich(DirServer dirServer) {
		if (pricesMap.containsKey(dirServer.name())) {
			return dirServer
					.toBuilder()
					.price(pricesMap.get(dirServer.name()))
					.build();
		}
		return dirServer;
	}

	private Optional<DirServer> parseYmlFile(Path ymlFile) {
		String file = ymlFile.getFileName().toString();
		if (file.equals(projectsYmlFileName)) {
			return Optional.of(customProjectYamlParser.parseCustomYmlFile(ymlFile));
		} else if (file.contains("docker")) {
			return Optional.of(dockerComposeParser.parseDockerYmlFile(ymlFile));
		}
		return Optional.empty();
	}

	private Optional<Path> findYmlFile(Path dir) {
		try (Stream<Path> walk = Files.walk(dir, 1)) {
			List<Path> paths = walk
					.filter(path -> {
						String lowerCase = path.getFileName().toString().toLowerCase();
						return lowerCase.endsWith(".yml") || lowerCase.endsWith(".yaml");
					})
					.toList();
			if (paths.size() == 1) {
				return Optional.of(paths.get(0));
			}
			if (paths.size() > 1) {
				Optional<Path> pathNonDockerOp = paths.stream()
						.filter(path -> path.getFileName().toString().equals(projectsYmlFileName))
						.findFirst();
				if (pathNonDockerOp.isPresent()) {
					return pathNonDockerOp;
				} else {
					return paths.stream()
							.filter(path -> path.getFileName().toString().contains("docker"))
							.findFirst();
				}
			}

			return Optional.empty();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Path> scanLocalDirs() {
		try (Stream<Path> walk = Files.walk(serversDir, 1)) {
			return walk
					.filter(path -> !path.equals(serversDir))
					.filter(path -> path.toFile().isDirectory())
					.filter(path -> !path.resolve(notScanFile).toFile().exists())
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
