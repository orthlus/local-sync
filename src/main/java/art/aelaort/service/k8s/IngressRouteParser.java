package art.aelaort.service.k8s;

import art.aelaort.models.servers.k8s.K8sIngressRoute;
import art.aelaort.models.servers.k8s.input.IngressRouteSpec;
import art.aelaort.service.mappers.K8sIngressRouteMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IngressRouteParser {
	private final YAMLMapper yamlMapper;

	public Map<String, K8sIngressRoute> getMapRoutesByServiceName(List<HasMetadata> k8sObjects) {
		return getIngressRoutes(k8sObjects)
				.stream()
				.collect(Collectors.toMap(
						K8sIngressRoute::getServiceName,
						Function.identity()
				));
	}

	public List<K8sIngressRoute> getIngressRoutes(List<HasMetadata> k8sObjects) {
		return k8sObjects.stream()
				.filter(GenericKubernetesResource.class::isInstance)
				.map(GenericKubernetesResource.class::cast)
				.filter(this::isIngressRoute)
				.map(this::parseIngressRoute)
				.filter(Optional::isPresent).map(Optional::get)
				.toList();
	}

	private boolean isIngressRoute(GenericKubernetesResource resource) {
		if ("IngressRoute".equals(resource.getKind())) {
			String api = resource.getApiVersion();
			return "traefik.containo.us/v1alpha1".equals(api) || "traefik.io/v1alpha1".equals(api);
		}
		return false;
	}

	private Optional<K8sIngressRoute> parseIngressRoute(GenericKubernetesResource genericIngressRoute) {
		Object spec = genericIngressRoute.getAdditionalProperties().get("spec");
		if (spec == null) {
			return Optional.empty();
		}
		IngressRouteSpec ingressRouteSpec = yamlMapper.convertValue(spec, IngressRouteSpec.class);
		K8sIngressRoute ingressRoute = K8sIngressRouteMapper.map(ingressRouteSpec, genericIngressRoute);
		return Optional.of(ingressRoute);
	}
}
