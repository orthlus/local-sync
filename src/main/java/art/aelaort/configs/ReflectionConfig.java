package art.aelaort.configs;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@RegisterReflectionForBinding({
		art.aelaort.models.build.Job.class,
		art.aelaort.models.build.PomModel.class,
		art.aelaort.models.build.PomModel.Properties.class,
		art.aelaort.models.servers.Server.class,
		art.aelaort.models.servers.yaml.TabbyFile.class,
		art.aelaort.models.servers.yaml.CustomFile.class,
		art.aelaort.models.servers.yaml.DockerComposeFile.class,
		art.aelaort.models.servers.K8sCluster.class,
		art.aelaort.models.servers.K8sApp.class,
		art.aelaort.models.servers.K8sService.class,
		com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.class,
})
public class ReflectionConfig {
}
