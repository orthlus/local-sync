package art.aelaort.service.k8s;

import art.aelaort.models.servers.k8s.K8sHelmChart;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HelmChartParser {
	private final YAMLMapper yamlMapper;

	public List<K8sHelmChart> getChartsList(List<HasMetadata> k8sObjects) {
		return getHelmCharts(k8sObjects);
	}
	private List<K8sHelmChart> getHelmCharts(List<HasMetadata> k8sObjects) {
		return k8sObjects.stream()
				.filter(GenericKubernetesResource.class::isInstance)
				.map(GenericKubernetesResource.class::cast)
				.filter(this::isHelmChart)
				.map(this::parseHelmChartSpec)
				.filter(Optional::isPresent).map(Optional::get)
				.toList();
	}

	private boolean isHelmChart(GenericKubernetesResource resource) {
		return "HelmChart".equals(resource.getKind()) && "helm.cattle.io/v1".equals(resource.getApiVersion());
	}

	private Optional<K8sHelmChart> parseHelmChartSpec(GenericKubernetesResource genericHelmChartSpec) {
		Object spec = genericHelmChartSpec.getAdditionalProperties().get("spec");
		if (spec == null) {
			return Optional.empty();
		}
		K8sHelmChart helmChartSpec = yamlMapper.convertValue(spec, K8sHelmChart.class);
		return Optional.of(helmChartSpec);
	}
}
