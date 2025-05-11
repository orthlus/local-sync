package art.aelaort.k8s;

import art.aelaort.k8s.dto.IngressRouteSpec;
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

	public Map<String, IngressRouteSpec> getMapRoutesByServiceName(List<HasMetadata> k8sObjects) {
		return getIngressRoutes(k8sObjects)
				.stream()
				.collect(Collectors.toMap(
						irs -> irs.getRoutes().get(0).getServices().get(0).getName(),
						Function.identity()
				));
	}

	private List<IngressRouteSpec> getIngressRoutes(List<HasMetadata> k8sObjects) {
		return k8sObjects.stream()
				.filter(GenericKubernetesResource.class::isInstance)
				.map(GenericKubernetesResource.class::cast)
				.filter(this::isIngressRoute)
				.map(this::parseIngressRoute)
				.filter(Optional::isPresent).map(Optional::get)
				.toList();
	}

	private boolean isIngressRoute(GenericKubernetesResource resource) {
		return "IngressRoute".equals(resource.getKind())
			   && "traefik.containo.us/v1alpha1".equals(resource.getApiVersion());
	}

	private Optional<IngressRouteSpec> parseIngressRoute(GenericKubernetesResource genericIngressRoute) {
		Object spec = genericIngressRoute.getAdditionalProperties().get("spec");
		if (spec == null) {
			return Optional.empty();
		}
		IngressRouteSpec ingressRouteSpec = yamlMapper.convertValue(spec, IngressRouteSpec.class);
		return Optional.of(ingressRouteSpec);
	}
}
