package art.aelaort.scan_show;

import art.aelaort.ServersManagementService;
import art.aelaort.k8s.K8sClusterProvider;
import art.aelaort.models.servers.K8sCluster;
import art.aelaort.models.servers.Server;
import art.aelaort.servers.providers.ServerProvider;
import art.aelaort.utils.ExternalUtilities;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ScanShowServersService {
	private final ServersManagementService serversManagementService;
	private final ExternalUtilities externalUtilities;
	private final ServerProvider serverProvider;
	private final K8sClusterProvider k8sClusterProvider;

	public void sync() {
		List<K8sCluster> clusters = k8sClusterProvider.getClusters();

		Map<String, String> mapNodesByClusterName = k8sClusterProvider.getMapClusterNameByNode(clusters);
		List<Server> servers = serverProvider.scanAndJoinData(mapNodesByClusterName);

		serversManagementService.saveData(servers, clusters);
		serversManagementService.saveIps(servers);
		log("sync done");
	}

	public void syncAll() {
		sync();
		externalUtilities.dirSync();
	}
}
