package art.aelaort.service.providers;

import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.ssh.SshConfigServer;
import art.aelaort.models.servers.ssh.SshServer;
import art.aelaort.service.k8s.K8sClusterProvider;
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
	private final SshConfigServerProvider sshConfigServerProvider;
	private final K8sClusterProvider k8sClusterProvider;

	public List<Server> scanAndJoinData() {
		List<DirServer> dirServers = dirServerProvider.scanServersDir();
		List<SshConfigServer> sshConfigServers = sshConfigServerProvider.readLocal();
		Map<String, String> mapNodesByClusterName = k8sClusterProvider.getMapClusterNameByNode();
		return joinDirAndSshServers(dirServers, sshConfigServers, mapNodesByClusterName);
	}

	private List<Server> joinDirAndSshServers(List<DirServer> dirServers, List<? extends SshServer> sshServers, Map<String, String> mapNodesByClusterName) {
		Map<String, DirServer> dirServersByName = serverMapper.toMapServers(dirServers);
		List<Server> result = new ArrayList<>(sshServers.size());

		for (SshServer sshServer : sshServers) {
			Server.ServerBuilder serverBuilder = Server.builder()
					.name(sshServer.name())
					.ip(sshServer.host())
					.port(sshServer.port())
					.sshKey(sshServer.keyPath().replace("\\", "/"));

			DirServer dirServer = dirServersByName.get(sshServer.name());
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

		Map<String, SshServer> sshServersByName = serverMapper.toMapSshServer(sshServers);
		for (DirServer dirServer : dirServers) {
			if (!sshServersByName.containsKey(dirServer.name())) {
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
