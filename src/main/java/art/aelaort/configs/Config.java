package art.aelaort.configs;

import art.aelaort.DefaultS3Params;
import art.aelaort.S3Params;
import art.aelaort.properties.S3Properties;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

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

	@Bean
	public RestTemplate tabbyDecoder(RestTemplateBuilder restTemplateBuilder,
									 @Value("${tabby.decode.service.url}") String url) {
		return restTemplateBuilder
				.rootUri(url)
				.defaultHeader(CONTENT_TYPE, TEXT_PLAIN_VALUE)
				.build();
	}
}
