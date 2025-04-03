package art.aelaort.s3;

import art.aelaort.S3Params;
import art.aelaort.properties.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static art.aelaort.S3ClientProvider.client;

@Component
@RequiredArgsConstructor
public class ServersManagementS3 {
	private final S3Params serversManagementS3Params;
	private final S3Properties s3Properties;

	public void uploadIps(String ipsText) {
		try (S3Client client = client(serversManagementS3Params)) {
			RequestBody requestBody = RequestBody.fromString(ipsText);
			client.putObject(PutObjectRequest.builder()
					.bucket(s3Properties.getServersManagement().getBucket())
					.key("ips.txt")
					.build(), requestBody);
		}
	}

	public void uploadData(String jsonStr) {
		try (S3Client client = client(serversManagementS3Params)) {
			RequestBody requestBody = RequestBody.fromString(jsonStr);
			client.putObject(PutObjectRequest.builder()
					.bucket(s3Properties.getServersManagement().getBucket())
					.key("data.json")
					.build(), requestBody);
		}
	}

	public void uploadK8sData(String jsonStr) {
		try (S3Client client = client(serversManagementS3Params)) {
			RequestBody requestBody = RequestBody.fromString(jsonStr);
			client.putObject(PutObjectRequest.builder()
					.bucket(s3Properties.getServersManagement().getBucket())
					.key("k8s-data.json")
					.build(), requestBody);
		}
	}
}
