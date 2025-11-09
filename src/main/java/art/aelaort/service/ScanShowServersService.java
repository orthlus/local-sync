package art.aelaort.service;

import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.display.K8sIngressRouteRow;
import art.aelaort.models.servers.k8s.K8sCluster;
import art.aelaort.models.servers.k8s.K8sIngressRoute;
import art.aelaort.service.k8s.K8sClusterProvider;
import art.aelaort.service.output.mapper.K8sRowMapper;
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
	private final K8sRowMapper k8sRowMapper;

	public void sync() {
		List<K8sCluster> clustersNoArgocd = k8sClusterProvider.getClustersNoArgocd();

		List<Server> servers = serverProvider.scanAndJoinData();

		serversManagementService.saveData(servers, clustersNoArgocd);
		serversManagementService.saveIps(servers);

		outputJsonService.saveApps(servers);
		outputJsonService.saveServers(servers);
		outputJsonService.saveK8sApps(clustersNoArgocd);

		Map<String, List<K8sIngressRoute>> allClustersIngressRoutes = k8sClusterProvider.getAllClustersIngressRoutes();
		List<K8sIngressRouteRow> k8sIngressRouteRows = k8sRowMapper.mapToIngressRouteRows(allClustersIngressRoutes);
		bookmarksService.saveBookmarks(k8sIngressRouteRows);

//		tabbyServerProvider.copyToRepo();
		log(wrapGreen("servers and apps sync finished"));
	}

	public void syncAll() {
		sync();
		gitBundleService.makeBundles();
		externalUtilities.dirSync();
	}
}
