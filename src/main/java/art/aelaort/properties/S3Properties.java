package art.aelaort.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("s3")
public class S3Properties {
	String endpoint;
	String region;
	Tabby tabby;
	ServersManagement serversManagement;

	@Getter
	@Setter
	public static class Tabby {
		String id;
		String key;
		String bucket;
		String file;
	}

	@Getter
	@Setter
	public static class ServersManagement {
		String id;
		String key;
		String bucket;
	}
}
