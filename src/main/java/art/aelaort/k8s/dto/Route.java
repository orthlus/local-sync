package art.aelaort.k8s.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Route {
	private List<Service> services;
	private String match;
}
