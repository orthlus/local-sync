package art.aelaort.configs;

import art.aelaort.DefaultS3Params;
import art.aelaort.S3Params;
import art.aelaort.properties.S3Properties;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class Config {
	@Autowired
	private S3Properties properties;

	@Bean
	public YAMLMapper yamlMapper() {
		return new YAMLMapper();
	}

	@Bean
	@Primary
	public JsonMapper jsonMapper() {
		return new JsonMapper();
	}

	@Bean
	public S3Params tabbyS3Params() {
		return new DefaultS3Params(
				properties.getTabby().getId(),
				properties.getTabby().getKey(),
				properties.getEndpoint(),
				properties.getRegion());
	}

	@Bean
	public S3Params serversManagementS3Params() {
		return new DefaultS3Params(
				properties.getServersManagement().getId(),
				properties.getServersManagement().getKey(),
				properties.getEndpoint(),
				properties.getRegion());
	}
}
