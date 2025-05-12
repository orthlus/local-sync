package art.aelaort.k8s;

import art.aelaort.models.servers.k8s.K8sIngressRoute;
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
class IngressRouteParser {
	private final YAMLMapper yamlMapper;

	public Map<String, K8sIngressRoute> getMapRoutesByServiceName(List<HasMetadata> k8sObjects) {
		return getIngressRoutes(k8sObjects)
				.stream()
				.collect(Collectors.toMap(
						irs -> irs.getRoutes().get(0).getServices().get(0).getName(),
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
		K8sIngressRoute ingressRoute0 = yamlMapper.convertValue(spec, K8sIngressRoute.class);
		K8sIngressRoute ingressRoute = ingressRoute0.toBuilder()
				.name(genericIngressRoute.getMetadata().getName())
				.namespace(genericIngressRoute.getMetadata().getNamespace())
				.hasTls(ingressRoute0.getTls() != null)
				.tls(null)
				.build();
		return Optional.of(ingressRoute);
	}
}
