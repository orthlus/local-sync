package art.aelaort.service.providers;

import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.service.mappers.ServerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServerProvider {
	private final ServerMapper serverMapper;
	private final DirServerProvider dirServerProvider;
	private final TabbyServerProvider tabbyServerProvider;

	public List<Server> scanAndJoinData(Map<String, String> mapNodesByClusterName) {
		List<DirServer> dirServers = dirServerProvider.scanServersDir();
		List<TabbyServer> tabbyServers = tabbyServerProvider.readRemote();
		return joinDirAndTabbyServers(dirServers, tabbyServers, mapNodesByClusterName);
	}

	private List<Server> joinDirAndTabbyServers(List<DirServer> dirServers, List<TabbyServer> tabbyServers, Map<String, String> mapNodesByClusterName) {
		Map<String, DirServer> dirServersByName = serverMapper.toMapServers(dirServers);
		List<Server> result = new ArrayList<>(tabbyServers.size());

		for (TabbyServer tabbyServer : tabbyServers) {
			Server.ServerBuilder serverBuilder = Server.builder()
					.name(tabbyServer.name())
					.ip(tabbyServer.host())
					.port(tabbyServer.port())
					.sshKey(tabbyServer.keyPath().replace("\\", "/"));

			DirServer dirServer = dirServersByName.get(tabbyServer.name());
			if (dirServer == null) {
				serverBuilder
						.monitoring(false)
						.services(List.of());
			} else {
				serverBuilder
						.monitoring(dirServer.monitoring())
						.services(dirServer.services())
						.price(dirServer.price());
			}
			result.add(serverBuilder.build());
		}

		Map<String, TabbyServer> tabbyServersByName = serverMapper.toMapTabby(tabbyServers);
		for (DirServer dirServer : dirServers) {
			if (!tabbyServersByName.containsKey(dirServer.name())) {
				Server server = Server.builder()
						.name(dirServer.name())
						.monitoring(dirServer.monitoring())
						.services(dirServer.services())
						.price(dirServer.price())
						.build();
				result.add(server);
			}
		}

		if (!mapNodesByClusterName.isEmpty()) {
			result = result.stream()
					.map(s -> s.withK8s(mapNodesByClusterName.get(s.getName())))
					.collect(Collectors.toList());
		}

		return Server.addNumbers(result);
	}
}
