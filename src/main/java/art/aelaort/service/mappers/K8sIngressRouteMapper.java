package art.aelaort.service.mappers;

import art.aelaort.models.servers.k8s.K8sIngressRoute;
import art.aelaort.models.servers.k8s.input.IngressRouteSpec;
import art.aelaort.models.servers.k8s.input.Route;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;

public class K8sIngressRouteMapper {
	public static K8sIngressRoute map(IngressRouteSpec ingressRouteSpec, GenericKubernetesResource genericKubernetesResource) {
		validate(ingressRouteSpec, genericKubernetesResource);
		Route route = ingressRouteSpec.getRoutes().get(0);
		return K8sIngressRoute.builder()
				.name(genericKubernetesResource.getMetadata().getName())
				.namespace(genericKubernetesResource.getMetadata().getNamespace())
				.hasTls(ingressRouteSpec.getTls() != null)
				.match(route.getMatch())
				.serviceName(route.getServices().get(0).getName())
				.servicePort(route.getServices().get(0).getPort())
				.build();
	}

	private static void validate(IngressRouteSpec ingressRouteSpec, GenericKubernetesResource genericKubernetesResource) {
		if (ingressRouteSpec.getRoutes().size() > 1) {
			log(wrapRed("K8s ingress route spec %s has more than one route".formatted(genericKubernetesResource.getMetadata().getName())));
		}
		for (Route route : ingressRouteSpec.getRoutes()) {
			if (route.getServices().size() > 1) {
				log(wrapRed("K8s ingress route spec %s has more than one service".formatted(genericKubernetesResource.getMetadata().getName())));
			}
		}
	}
}
