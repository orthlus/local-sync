package art.aelaort.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
class RestConfig {
	@Value("${servers.ui.url}")
	private String uiUrl;

	@Bean
	public RestTemplate serversUiRestTemplate(RestTemplateBuilder builder) {
		return builder
				.rootUri(uiUrl)
				.build();
	}
}
