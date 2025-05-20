package art.aelaort.service;

import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.display.AppRow;
import art.aelaort.models.servers.display.ClusterAppRow;
import art.aelaort.models.servers.display.ServerRow;
import art.aelaort.models.servers.k8s.K8sCluster;
import art.aelaort.service.output.mapper.AppRowMapper;
import art.aelaort.service.output.mapper.ClusterAppRowMapper;
import art.aelaort.service.output.mapper.ServerRowMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutputJsonService {
	private final ClusterAppRowMapper clusterAppRowMapper;
	private final ObjectMapper jacksonObjectMapper;
	private final AppRowMapper appRowMapper;
	private final ServerRowMapper serverRowMapper;
	@Value("${servers.output.cluster-app-rows-file}")
	private Path clusterAppRowsFile;
	@Value("${servers.output.app-rows-file}")
	private Path appRowsFile;
	@Value("${servers.output.servers-rows-file}")
	private Path serversRowsFile;

	public void saveServers(List<Server> servers) {
		List<ServerRow> serverRows = serverRowMapper.mapToServerRow(servers);
		try {
			jacksonObjectMapper.writeValue(serversRowsFile.toFile(), serverRows);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void saveApps(List<Server> servers) {
		List<AppRow> appRows = appRowMapper.mapToAppRows(servers);
		appRows = AppRow.addNumbers(appRows);
		try {
			jacksonObjectMapper.writeValue(appRowsFile.toFile(), appRows);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void saveK8sApps(List<K8sCluster> clusters) {
		List<ClusterAppRow> clusterAppRows = clusterAppRowMapper.mapToClusterAppRows(clusters);
		clusterAppRows = ClusterAppRow.addNumbers(clusterAppRows);
		try {
			jacksonObjectMapper.writeValue(clusterAppRowsFile.toFile(), clusterAppRows);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
