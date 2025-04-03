package art.aelaort.s3;

import art.aelaort.S3Params;
import art.aelaort.properties.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

import static art.aelaort.S3ClientProvider.client;


@Component
@RequiredArgsConstructor
public class TabbyS3 {
	private final S3Params tabbyS3Params;
	private final S3Properties s3Properties;

	public String download() {
		try (S3Client client = client(tabbyS3Params)) {
			return client.getObjectAsBytes(builder -> builder
							.bucket(s3Properties.getTabby().getBucket())
							.key(s3Properties.getTabby().getFile()))
					.asUtf8String();
		}
	}
}
