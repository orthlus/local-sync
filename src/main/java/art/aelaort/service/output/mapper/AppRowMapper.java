package art.aelaort.service.output.mapper;

import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.ServiceDto;
import art.aelaort.models.servers.display.AppRow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AppRowMapper {
	public List<AppRow> mapToAppRows(List<Server> servers) {
		List<AppRow> res = new ArrayList<>();
		for (Server server : servers) {
			for (ServiceDto service : server.getServices()) {
				String image = service.getDockerImageName();
				String appName = getAppName(service);
				AppRow appRow = new AppRow(null, server.getName(), image, appName, service.getYmlName());
				res.add(appRow);
			}
		}
		return res;
	}

	private String getAppName(ServiceDto service) {
		return service.getDockerName() == null ?
				service.getService() :
				service.getDockerName() + " - " + service.getService();
	}
}
