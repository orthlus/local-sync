package art.aelaort.k8s;

import art.aelaort.models.servers.K8sApp;
import art.aelaort.models.servers.K8sService;
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

import static art.aelaort.k8s.K8sUtils.unwrap;

@Component
@RequiredArgsConstructor
public class K8sYamlParser {
	private final Utils utils;

	public List<K8sService> parseK8sYmlFileForServices(Path ymlFile) {
		List<HasMetadata> k8sObjects = parse(ymlFile);
		List<K8sService> result = new ArrayList<>(k8sObjects.size());

		for (HasMetadata k8sObject : k8sObjects) {
			K8sService obj;
			if (k8sObject instanceof Service o) {
				obj = convert(o);
			} else {
				continue;
			}
			result.add(obj);
		}

		return result;
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

		if (ports.size() > 1) {
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

	public List<K8sApp> parseK8sYmlFileForApps(Path ymlFile) {
		List<HasMetadata> k8sObjects = parse(ymlFile);
		List<K8sApp> result = new ArrayList<>(k8sObjects.size());

		for (HasMetadata k8sObject : k8sObjects) {
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

	private String namespace(ObjectMeta objectMeta) {
		return objectMeta.getNamespace() == null ? "-" : objectMeta.getNamespace();
	}

	private K8sApp clean(K8sApp k8sApp) {
		if (k8sApp.getImage() == null) {
			return k8sApp;
		}
		return k8sApp.withImage(utils.dockerImageClean(k8sApp.getImage()));
	}

	private List<HasMetadata> parse(Path ymlFile) {
		try (KubernetesClient client = new KubernetesClientBuilder().build()) {
			return client.load(Files.newInputStream(ymlFile)).items();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
