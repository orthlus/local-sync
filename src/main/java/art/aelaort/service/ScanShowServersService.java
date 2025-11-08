package art.aelaort.service;

import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.k8s.K8sCluster;
import art.aelaort.service.k8s.K8sClusterProvider;
import art.aelaort.service.providers.ServerProvider;
import art.aelaort.utils.ExternalUtilities;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapGreen;
import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ScanShowServersService {
	private final ServersManagementService serversManagementService;
	private final ExternalUtilities externalUtilities;
	private final ServerProvider serverProvider;
	private final K8sClusterProvider k8sClusterProvider;
	private final GitBundleService gitBundleService;
	private final OutputJsonService outputJsonService;
	private final BookmarksService bookmarksService;

	public void sync() {
		List<K8sCluster> clusters = k8sClusterProvider.getClusters();

		Map<String, String> mapNodesByClusterName = k8sClusterProvider.getMapClusterNameByNode(clusters);
		List<Server> servers = serverProvider.scanAndJoinData(mapNodesByClusterName);

		serversManagementService.saveData(servers, clusters);
		serversManagementService.saveIps(servers);

		outputJsonService.saveApps(servers);
		outputJsonService.saveServers(servers);
		outputJsonService.saveK8sApps(clusters);
		bookmarksService.saveBookmarks(clusters);

//		tabbyServerProvider.copyToRepo();
		log(wrapGreen("servers and apps sync finished"));
	}

	public void syncAll() {
		sync();
		gitBundleService.makeBundles();
		externalUtilities.dirSync();
	}
}
