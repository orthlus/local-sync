package art.aelaort.service.output.mapper;

import art.aelaort.models.servers.display.K8sAppRow;
import art.aelaort.models.servers.display.K8sCronJobRow;
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
public class K8sRowMapper {
	public List<K8sCronJobRow> mapToCronJobRows(List<K8sCluster> clusters) {
		List<K8sCronJobRow> res = new ArrayList<>();
		for (K8sCluster cluster : clusters) {
			for (K8sApp app : cluster.apps()) {
				if (app.getSchedule() == null) {
					continue;
				}
				K8sCronJobRow k8sCronJobRow = new K8sCronJobRow(
						null,
						cluster.name(),
						app.getNamespace(),
						app.getImage(),
						app.getName(),
						app.getSchedule(),
						app.getImagePullPolicy(),
						app.getMemoryLimit());

				res.add(k8sCronJobRow);
			}
		}
		return res;
	}

	public List<K8sAppRow> mapToAppRows(List<K8sCluster> clusters) {
		List<K8sAppRow> res = new ArrayList<>();
		for (K8sCluster cluster : clusters) {
			Map<String, K8sService> serviceByPodNameMap = makeServiceByPodNameMap(cluster);
			for (K8sApp app : cluster.apps()) {
				if (app.getSchedule() != null) {
					continue;
				}
				K8sService service = serviceByPodNameMap.get(app.getPodName());
				K8sAppRow clusterAppRow = new K8sAppRow(
						null,
						cluster.name(),
						app.getNamespace(),
						app.getImage(),
						app.getName(),
						renameKind(app.getKind()),
						app.getImagePullPolicy(),
						servicePortsString(service),
						serviceType(service),
						serviceRoute(service),
						app.getMemoryLimit(),
						app.getStrategyType()
				);
				res.add(clusterAppRow);
			}

			for (K8sHelmChart helmChart : cluster.helmCharts()) {
				K8sAppRow clusterAppRow = new K8sAppRow(
						null,
						cluster.name(),
						helmChart.getTargetNamespace(),
						helmChart.getRepo(),
						helmChart.getChart(),
						"HC",
						null, null, null, null, null, null
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
