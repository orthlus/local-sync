package art.aelaort.service.k8s;

import art.aelaort.models.servers.k8s.K8sApp;
import art.aelaort.models.servers.k8s.K8sHelmChart;
import art.aelaort.models.servers.k8s.K8sIngressRoute;
import art.aelaort.models.servers.k8s.K8sService;
import art.aelaort.utils.Utils;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class K8sYamlParser {
	private final Utils utils;
	private final IngressRouteParser ingressRouteParser;
	private final HelmChartParser helmChartParser;

	public List<K8sIngressRoute> parseK8sYmlFileForIngressRoutes(List<HasMetadata> hasMetadataList) {
		return ingressRouteParser.getIngressRoutes(hasMetadataList);
	}

	public List<K8sHelmChart> parseK8sYmlFileForHelmCharts(List<HasMetadata> hasMetadataList) {
		return helmChartParser.getChartsList(hasMetadataList);
	}

	public List<K8sService> parseK8sYmlFileForServices(List<HasMetadata> hasMetadataList) {
		List<K8sService> result = new ArrayList<>(hasMetadataList.size());

		for (HasMetadata k8sObject : hasMetadataList) {
			K8sService obj;
			if (k8sObject instanceof Service o) {
				obj = convert(o);
			} else {
				continue;
			}
			result.add(obj);
		}

		return enrichWithRoutes(result, hasMetadataList);
	}

	private List<K8sService> enrichWithRoutes(List<K8sService> result, List<HasMetadata> k8sObjects) {
		List<K8sService> newResult = new ArrayList<>(k8sObjects.size());

		Map<String, K8sIngressRoute> mapRoutesByServiceName = ingressRouteParser.getMapRoutesByServiceName(k8sObjects);

		for (K8sService k8sService : result) {
			K8sIngressRoute ingressRouteSpec = mapRoutesByServiceName.get(k8sService.getName());
			if (ingressRouteSpec != null) {
				String routeMatch = Utils.cleanK8sRouteMatchIfPossible(ingressRouteSpec.getMatch());
				K8sService e = k8sService.withRoute(routeMatch);
				newResult.add(e);
			} else {
				newResult.add(k8sService);
			}
		}

		return newResult;
	}

	private K8sService convert(Service service) {
		K8sService.K8sServiceBuilder builder = K8sService.builder()
				.name(service.getMetadata().getName())
				.namespace(namespace(service.getMetadata()))
				.kind(service.getKind())
				.type(service.getSpec().getType())
				.appSelector(service.getSpec().getSelector().get("app"));
		enrichWithPorts(builder, service);
		return builder
				.build();
	}

	private void enrichWithPorts(K8sService.K8sServiceBuilder builder, Service service) {
		List<ServicePort> ports = service.getSpec().getPorts();
		if (ports == null || ports.isEmpty()) {
			return;
		}

		if (ports.size() > 1 && !allPortsNumberSame(ports)) {
			builder.hasAnotherPorts(true);
		}

		ServicePort port = ports.get(0);

		builder.port(port.getPort());

		if (port.getTargetPort() != null && port.getTargetPort().getValue() != null) {
			builder.targetPort(unwrap(port.getTargetPort()));
		}

		if (port.getNodePort() != null) {
			builder.nodePort(port.getNodePort());
		}
	}

	private boolean allPortsNumberSame(List<ServicePort> ports) {
		if (ports.size() == 1) {
			return true;
		}

		if (ports.stream().allMatch(sp -> sp.getNodePort() != null)) {
			int port = ports.get(0).getNodePort();
			for (ServicePort servicePort : ports) {
				if (servicePort.getNodePort() != port) {
					return false;
				}
			}
			return true;
		} else if (ports.stream().allMatch(sp -> sp.getNodePort() == null)) {
			int port = ports.get(0).getPort();
			for (ServicePort servicePort : ports) {
				if (servicePort.getPort() != port) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public List<K8sApp> parseK8sYmlFileForApps(List<HasMetadata> hasMetadataList) {
		List<K8sApp> result = new ArrayList<>(hasMetadataList.size());

		for (HasMetadata k8sObject : hasMetadataList) {
			K8sApp obj;
			if (k8sObject instanceof Pod o) {
				obj = convert(o);
			} else if (k8sObject instanceof Deployment o) {
				obj = convert(o);
			} else if (k8sObject instanceof DaemonSet o) {
				obj = convert(o);
			} else if (k8sObject instanceof CronJob o) {
				obj = convert(o);
			} else {
				continue;
			}
			result.add(clean(obj));
		}

		return result;
	}

	private K8sApp convert(CronJob cronJob) {
		return K8sApp.builder()
				.image(cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
				.containerName(cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().get(0).getName())
				.imagePullPolicy(cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().get(0).getImagePullPolicy())
				.memoryLimit(memoryLimit(cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().get(0).getResources()))
				.name(cronJob.getMetadata().getName())
				.namespace(namespace(cronJob.getMetadata()))
				.kind(cronJob.getKind())
				.schedule(cronJob.getSpec().getSchedule())
				.build();
	}

	private K8sApp convert(Deployment deployment) {
		DeploymentStrategy strategy = deployment.getSpec().getStrategy();
		return K8sApp.builder()
				.image(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
				.containerName(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getName())
				.imagePullPolicy(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImagePullPolicy())
				.memoryLimit(memoryLimit(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources()))
				.podName(deployment.getSpec().getTemplate().getMetadata().getLabels().get("app"))
				.name(deployment.getMetadata().getName())
				.namespace(namespace(deployment.getMetadata()))
				.kind(deployment.getKind())
				.strategyType(strategy == null ? null : strategy.getType())
				.build();
	}

	private K8sApp convert(DaemonSet daemonSet) {
		return K8sApp.builder()
				.image(daemonSet.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
				.containerName(daemonSet.getSpec().getTemplate().getSpec().getContainers().get(0).getName())
				.imagePullPolicy(daemonSet.getSpec().getTemplate().getSpec().getContainers().get(0).getImagePullPolicy())
				.memoryLimit(memoryLimit(daemonSet.getSpec().getTemplate().getSpec().getContainers().get(0).getResources()))
				.podName(daemonSet.getSpec().getTemplate().getMetadata().getLabels().get("app"))
				.name(daemonSet.getMetadata().getName())
				.namespace(namespace(daemonSet.getMetadata()))
				.kind(daemonSet.getKind())
				.build();
	}

	private K8sApp convert(Pod pod) {
		return K8sApp.builder()
				.image(pod.getSpec().getContainers().get(0).getImage())
				.containerName(pod.getSpec().getContainers().get(0).getName())
				.imagePullPolicy(pod.getSpec().getContainers().get(0).getImagePullPolicy())
				.memoryLimit(memoryLimit(pod.getSpec().getContainers().get(0).getResources()))
				.name(pod.getMetadata().getName())
				.namespace(namespace(pod.getMetadata()))
				.podName(pod.getMetadata().getName())
				.kind(pod.getKind())
				.build();
	}

	private String memoryLimit(ResourceRequirements resources) {
		try {
			return resources.getLimits().get("memory").toString();
		} catch (NullPointerException e) {
			return "-";
		}
	}

	private String unwrap(IntOrString intOrString) {
		if (intOrString == null) {
			return null;
		}

		if (intOrString.getStrVal() != null) {
			return intOrString.getStrVal();
		} else {
			return String.valueOf(intOrString.getIntVal());
		}
	}

	private String namespace(ObjectMeta objectMeta) {
		return objectMeta.getNamespace() == null ? "-" : objectMeta.getNamespace();
	}

	private K8sApp clean(K8sApp k8sApp) {
		if (k8sApp.getImage() == null) {
			return k8sApp;
		}
		return k8sApp.withImage(utils.dockerImageClean(k8sApp.getImage()));
	}

	public List<HasMetadata> parse(Path ymlFile) {
		try (KubernetesClient client = new KubernetesClientBuilder().build()) {
			return client.load(Files.newInputStream(ymlFile)).items();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
