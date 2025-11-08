package art.aelaort.service;

import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.display.AppRow;
import art.aelaort.models.servers.display.K8sAppRow;
import art.aelaort.models.servers.display.K8sIngressRouteRow;
import art.aelaort.models.servers.display.ServerRow;
import art.aelaort.models.servers.k8s.K8sCluster;
import art.aelaort.service.output.mapper.AppRowMapper;
import art.aelaort.service.output.mapper.K8sRowMapper;
import art.aelaort.service.output.mapper.ServerRowMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutputJsonService {
	private final K8sRowMapper k8sRowMapper;
	private final ObjectMapper prettyObjectMapper;
	private final AppRowMapper appRowMapper;
	private final ServerRowMapper serverRowMapper;
	private final RestTemplate serversUiRestTemplate;
	@Value("${servers.output.cluster-app-rows-file}")
	private Path clusterAppRowsFile;
	@Value("${servers.output.app-rows-file}")
	private Path appRowsFile;
	@Value("${servers.output.servers-rows-file}")
	private Path serversRowsFile;

	public void saveServers(List<Server> servers) {
		List<ServerRow> serverRows = serverRowMapper.mapToServerRow(servers);

		String jsonStr = writeJson(serverRows);
		save(serversRowsFile, jsonStr);
	}

	public void saveApps(List<Server> servers) {
		List<AppRow> appRows = appRowMapper.mapToAppRows(servers);
		appRows = AppRow.addNumbers(appRows);

		String jsonStr = writeJson(appRows);
		save(appRowsFile, jsonStr);
	}

	public void saveK8sApps(List<K8sCluster> clusters) {
		List<K8sAppRow> k8sAppRows = k8sRowMapper.mapToAppRows(clusters);
		k8sAppRows = K8sAppRow.addNumbers(k8sAppRows);

		String jsonStr = writeJson(k8sAppRows);
		save(clusterAppRowsFile, jsonStr);
	}

	private String writeJson(List<?> rows) {
		try {
			return prettyObjectMapper.writeValueAsString(rows);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private void save(Path file, String jsonStr) {
		try {
			Files.writeString(file, jsonStr);
			serversUiRestTemplate.put("/" + file.getFileName().toString(), jsonStr);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
