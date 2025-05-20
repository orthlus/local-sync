package art.aelaort.service.output.mapper;

import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.display.ServerRow;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServerRowMapper {
	public List<ServerRow> mapToServerRow(List<Server> servers) {
		return servers.stream()
				.map(server -> new ServerRow(
						server.getId(),
						server.getName(),
						server.getIp(),
						server.getPort(),
						server.isMonitoring(),
						server.getPrice(),
						server.getK8s(),
						server.getSshKey(),
						Server.servicesStr(server.getServices())
				))
				.toList();
	}
}
