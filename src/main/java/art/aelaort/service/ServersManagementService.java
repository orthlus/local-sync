package art.aelaort.service;

import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.k8s.K8sCluster;
import art.aelaort.properties.K8sProps;
import art.aelaort.service.s3.ServersManagementS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ServersManagementService {
	private final ServersManagementS3 serversManagementS3;
	private final ObjectMapper prettyObjectMapper;
	private final K8sProps k8sProps;
	@Value("${servers.management.json_path}")
	private Path jsonDataPath;

	public void saveIps(List<Server> servers) {
		String text = servers.stream()
							  .filter(Server::isMonitoring)
							  .filter(server -> server.getIp() != null)
							  .filter(server -> server.getPort() != null)
							  .map(server -> server.getName() + ":" + server.getIp() + ":" + server.getPort())
							  .collect(Collectors.joining("\n"))
					  + "\n";
		serversManagementS3.uploadIps(text);
		log("ips uploaded");
	}

	public void saveData(List<Server> servers, List<K8sCluster> clusters) {
		String json = toJson(servers);
		saveTextFileToLocal(jsonDataPath, json);

		String jsonK8s = toJsonK8s(clusters);
		saveTextFileToLocal(k8sProps.getSyncFile(), jsonK8s);

		log("saved data to local");

		serversManagementS3.uploadData(json);
		serversManagementS3.uploadK8sData(jsonK8s);
		log("saved data to s3");
	}

	@SneakyThrows
	private String toJson(List<Server> server) {
		return prettyObjectMapper.writeValueAsString(server);
	}

	@SneakyThrows
	private void saveTextFileToLocal(Path file, String jsonStr) {
		Files.writeString(file, jsonStr);
	}

	@SneakyThrows
	private String toJsonK8s(List<K8sCluster> clusters) {
		return prettyObjectMapper.writeValueAsString(clusters);
	}
}
