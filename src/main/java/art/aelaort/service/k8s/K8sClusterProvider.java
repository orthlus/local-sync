package art.aelaort.service.k8s;

import art.aelaort.models.servers.k8s.K8sApp;
import art.aelaort.models.servers.k8s.K8sCluster;
import art.aelaort.models.servers.k8s.K8sIngressRoute;
import art.aelaort.properties.K8sProps;
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
	private final IngressRouteParser ingressRouteParser;
	private final HelmChartParser helmChartParser;
	@Value("${servers.management.files.not_scan}")
	private String notScanFile;

	public Map<String, String> getMapClusterNameByNode() {
		Map<String, String> result = new HashMap<>();
		for (Path clustersDir : getClustersDirs()) {
			for (String node : readNodes(clustersDir)) {
				result.put(node, clustersDir.getFileName().toString());
			}
		}
		return result;
	}

	public Map<String, List<K8sIngressRoute>> getAllClustersIngressRoutes() {
		Map<String, List<K8sIngressRoute>> result = new HashMap<>();

		for (Path clustersDir : getClustersDirs()) {
			List<K8sIngressRoute> k8sIngressRoutes = parseClusterIngressRoutes(clustersDir);
			result.put(clustersDir.getFileName().toString(), k8sIngressRoutes);
		}

		for (Path clustersDir : getClustersArgocdDirs()) {
			List<K8sIngressRoute> k8sIngressRoutes = parseClusterIngressRoutes(clustersDir);
			result.put(clustersDir.getFileName().toString(), k8sIngressRoutes);
		}

		return result;
	}

	public List<K8sCluster> getClustersNoArgocd() {
		List<K8sCluster> result = new ArrayList<>();

		for (Path clustersDir : getClustersDirs()) {
			K8sCluster k8sCluster = parseWholeCluster(clustersDir);
			result.add(k8sCluster);
		}

		validateAndLog(result);

		return result;
	}

	private List<K8sIngressRoute> parseClusterIngressRoutes(Path clustersDir) {
		List<HasMetadata> hasMetadataList = getYamlFiles(clustersDir).stream()
				.map(k8sYamlParser::parse)
				.flatMap(Collection::stream)
				.toList();
		return ingressRouteParser.getIngressRoutes(hasMetadataList);
	}

	private K8sCluster parseWholeCluster(Path clustersDir) {
		List<HasMetadata> hasMetadataList = getYamlFiles(clustersDir).stream()
				.map(k8sYamlParser::parse)
				.flatMap(Collection::stream)
				.toList();
		return K8sCluster.builder()
				.name(clustersDir.getFileName().toString())
				.apps(k8sYamlParser.parseK8sYmlFileForApps(hasMetadataList))
				.services(k8sYamlParser.parseK8sYmlFileForServices(hasMetadataList))
				.helmCharts(helmChartParser.getChartsList(hasMetadataList))
//				.ingressRoutes(ingressRouteParser.getIngressRoutes(hasMetadataList))
//				.nodes(readNodes(clustersDir))
				.build();
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
			Path file = cluster.resolve(k8sProps.getNodesFile());
			if (Files.exists(file)) {
				return Files.readAllLines(file);
			}

			return List.of();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Path> getYamlFiles(Path dir) {
		Path root = dir;
		if (Files.exists(dir.resolve(k8sProps.getPathFiles()))) {
			root = dir.resolve(k8sProps.getPathFiles());
		}
		try (Stream<Path> walk = Files.walk(root)) {
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

	private List<Path> getClustersArgocdDirs() {
		return Arrays.asList(k8sProps.getArgoDirs());
	}

	private List<Path> getClustersDirs() {
		List<Path> res = new ArrayList<>();
		try (Stream<Path> walk = Files.walk(k8sProps.getDir(), 1)) {
			List<Path> list = walk
					.filter(path -> !path.equals(k8sProps.getDir()))
					.filter(path -> path.toFile().isDirectory())
					.filter(path -> !path.resolve(notScanFile).toFile().exists())
					.filter(path -> path.resolve(k8sProps.getPathFiles()).toFile().exists())
					.toList();
			res.addAll(list);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return res;
	}
}
