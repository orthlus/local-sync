package art.aelaort.configs;

import art.aelaort.DefaultS3Params;
import art.aelaort.S3Params;
import art.aelaort.properties.S3Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
	public ObjectMapper prettyObjectMapper() {
		return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
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
