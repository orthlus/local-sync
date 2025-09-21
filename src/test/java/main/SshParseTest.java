package main;

import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SshParseTest {
	@Test
	public void parseConfig() throws IOException {
		URL resource = getClass().getClassLoader().getResource("ssh-config");
		List<HostConfigEntry> hostConfigEntries = HostConfigEntry.readHostConfigEntries(resource);
		hostConfigEntries.forEach(System.out::println);
		assertEquals(3, hostConfigEntries.size());
		assertThat(hostConfigEntries.get(0).getHostName()).isEqualTo("192.168.1.3");
	}
}
