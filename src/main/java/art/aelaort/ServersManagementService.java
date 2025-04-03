package art.aelaort;

import art.aelaort.k8s.K8sProps;
import art.aelaort.models.servers.K8sCluster;
import art.aelaort.models.servers.Server;
import art.aelaort.s3.ServersManagementS3;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
	private final JsonMapper jsonMapper;
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
		saveJsonToLocal(json);

		String jsonK8s = toJsonK8s(clusters);
		saveK8sJsonToLocal(jsonK8s);

		log("saved data to local");

		serversManagementS3.uploadData(json);
		serversManagementS3.uploadK8sData(jsonK8s);
		log("saved data to s3");
	}

	@SneakyThrows
	private String toJson(List<Server> server) {
		return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(server);
	}

	@SneakyThrows
	private void saveJsonToLocal(String jsonStr) {
		Files.writeString(jsonDataPath, jsonStr);
	}

	@SneakyThrows
	private String toJsonK8s(List<K8sCluster> clusters) {
		return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(clusters);
	}

	@SneakyThrows
	private void saveK8sJsonToLocal(String jsonStr) {
		Files.writeString(k8sProps.getSyncFile(), jsonStr);
	}
}
