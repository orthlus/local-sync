package art.aelaort.service.output.mapper;

import art.aelaort.models.servers.display.ClusterAppRow;
import art.aelaort.models.servers.k8s.K8sApp;
import art.aelaort.models.servers.k8s.K8sCluster;
import art.aelaort.models.servers.k8s.K8sHelmChart;
import art.aelaort.models.servers.k8s.K8sService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

@Component
public class ClusterAppRowMapper {
	public List<ClusterAppRow> mapToClusterAppRows(List<K8sCluster> clusters) {
		List<ClusterAppRow> res = new ArrayList<>();
		for (K8sCluster cluster : clusters) {
			Map<String, K8sService> serviceByPodNameMap = makeServiceByPodNameMap(cluster);
			for (K8sApp app : cluster.apps()) {
				K8sService service = serviceByPodNameMap.get(app.getPodName());
				ClusterAppRow clusterAppRow = new ClusterAppRow(
						null,
						cluster.name(),
						app.getNamespace(),
						app.getImage(),
						app.getName(),
						renameKind(app.getKind()),
						app.getImagePullPolicy(),
						servicePortsString(service),
						serviceType(service),
						app.getSchedule(),
						app.getMemoryLimit(),
						app.getStrategyType(),
						serviceRoute(service)
				);
				res.add(clusterAppRow);
			}

			for (K8sHelmChart helmChart : cluster.helmCharts()) {
				ClusterAppRow clusterAppRow = new ClusterAppRow(
						null,
						cluster.name(),
						helmChart.getTargetNamespace(),
						helmChart.getRepo(),
						helmChart.getChart(),
						"HC",
						null, null, null, null, null, null, null
				);
				res.add(clusterAppRow);
			}
		}
		return res;
	}

	private String renameKind(String kind) {
		return switch (kind) {
			case "Deployment" -> "D";
			case "DaemonSet" -> "DS";
			case "CronJob" -> "CJ";
			default -> kind;
		};
	}

	private Map<String, K8sService> makeServiceByPodNameMap(K8sCluster cluster) {
		Map<String, K8sService> res = new HashMap<>();
		for (K8sApp app : cluster.apps()) {
			if (hasText(app.getPodName())) {
				for (K8sService service : cluster.services()) {
					if (service.getAppSelector().equals(app.getPodName()) && service.getNamespace().equals(app.getNamespace())) {
						res.put(app.getPodName(), service);
					}
				}
			}
		}
		return res;
	}

	private String serviceRoute(K8sService service) {
		if (service == null) {
			return null;
		}
		return service.getRoute();
	}

	private String serviceType(K8sService service) {
		if (service == null) {
			return null;
		}
		return service.getType();
	}

	private String enrichPorts(String port, Boolean hasAnotherPorts) {
		if (hasAnotherPorts != null && hasAnotherPorts) {
			return port + " (+)";
		}
		return port;
	}

	private String servicePortsString(K8sService service) {
		if (service == null || service.getPort() == null) {
			return null;
		}

		String result;
		if (service.getNodePort() == null) {
			if (hasText(service.getTargetPort())) {
				result = "%s %s".formatted(service.getPort(), service.getTargetPort());
			} else {
				result = "%s %s".formatted(service.getPort(), service.getPort());
			}
		} else {
			if (hasText(service.getTargetPort())) {
				result = "%s %s %s".formatted(service.getNodePort(), service.getPort(), service.getTargetPort());
			} else {
				result = "%s %s %s".formatted(service.getNodePort(), service.getPort(), service.getPort());
			}
		}
		return enrichPorts(result, service.getHasAnotherPorts());
	}
}
