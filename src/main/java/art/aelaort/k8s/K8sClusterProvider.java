package art.aelaort.k8s;

import art.aelaort.models.servers.k8s.K8sApp;
import art.aelaort.models.servers.k8s.K8sCluster;
import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class K8sClusterProvider {
	private final K8sYamlParser k8sYamlParser;
	private final K8sProps k8sProps;
	@Value("${servers.management.files.not_scan}")
	private String notScanFile;

	public Map<String, String> getMapClusterNameByNode(List<K8sCluster> clusters) {
		Map<String, String> res = new HashMap<>();
		for (K8sCluster cluster : clusters) {
			for (String node : cluster.nodes()) {
				res.put(node, cluster.name());
			}
		}
		return res;
	}

	public List<K8sCluster> getClusters() {
		List<K8sCluster> result = new ArrayList<>();

		for (Path clustersDir : getClustersDirs()) {
			Path dir = clustersDir.resolve(k8sProps.getPathFiles());

			List<HasMetadata> hasMetadataList = getYamlFiles(dir).stream()
					.map(k8sYamlParser::parse)
					.flatMap(Collection::stream)
					.toList();
			K8sCluster k8sCluster = K8sCluster.builder()
					.name(clustersDir.getFileName().toString())
					.apps(k8sYamlParser.parseK8sYmlFileForApps(hasMetadataList))
					.services(k8sYamlParser.parseK8sYmlFileForServices(hasMetadataList))
					.helmCharts(k8sYamlParser.parseK8sYmlFileForHelmCharts(hasMetadataList))
					.ingressRoutes(k8sYamlParser.parseK8sYmlFileForIngressRoutes(hasMetadataList))
					.nodes(readNodes(clustersDir))
					.build();

			result.add(k8sCluster);
		}

		validateAndLog(result);

		return result;
	}

	private void validateAndLog(List<K8sCluster> clusters) {
		Set<String> elements = new HashSet<>();
		List<String> duplicates = clusters
				.stream()
				.flatMap(cluster -> cluster.apps().stream())
				.map(K8sApp::getContainerName)
				.filter(n -> !elements.add(n))
				.toList();

		if (!duplicates.isEmpty()) {
			log(wrapRed("containers names is duplicated:"));
			duplicates.forEach(s -> log(wrapRed(s)));
		}
	}

	private List<String> readNodes(Path cluster) {
		try {
			return Files.readAllLines(cluster.resolve(k8sProps.getNodesFile()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Path> getYamlFiles(Path dir) {
		try (Stream<Path> walk = Files.walk(dir)) {
			return walk
					.filter(path -> {
						String lowerCase = path.getFileName().toString().toLowerCase();
						return lowerCase.endsWith(".yml") || lowerCase.endsWith(".yaml");
					})
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Path> getClustersDirs() {
		try (Stream<Path> walk = Files.walk(k8sProps.getDir(), 1)) {
			return walk
					.filter(path -> !path.equals(k8sProps.getDir()))
					.filter(path -> path.toFile().isDirectory())
					.filter(path -> !path.resolve(notScanFile).toFile().exists())
					.filter(path -> path.resolve(k8sProps.getPathFiles()).toFile().exists())
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
