package art.aelaort.service;

import art.aelaort.models.servers.display.K8sIngressRouteRow;
import art.aelaort.service.s3.ServersManagementS3;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class BookmarksService {
	private final ServersManagementS3 serversManagementS3;
	private final DnsRequestService dnsRequestService;
	private final RestTemplate bookmarksWebdavRestTemplate;

	@Value("${bookmarks.add-file}")
	private String bookmarksAddFile;
	@Value("${bookmarks.s3-key.cloud}")
	private String bookmarksS3KeyCloud;
	@Value("${bookmarks.s3-key.local}")
	private String bookmarksS3KeyLocal;
	@Value("${bookmarks.clusters.cloud}")
	private String[] bookmarksClustersCloud;
	@Value("${bookmarks.clusters.local}")
	private String[] bookmarksClustersLocal;
	@Value("${bookmarks.local-sync.file.local}")
	private Path bookmarksLocalSyncFileLocal;
	@Value("${bookmarks.local-sync.file.cloud}")
	private Path bookmarksLocalSyncFileCloud;
	@Value("${bookmarks.local.webdav.file.local}")
	private String bookmarksLocalWebdavFileLocal;
	@Value("${bookmarks.local.webdav.file.cloud}")
	private String bookmarksLocalWebdavFileCloud;

	private Set<String> bookmarksClustersCloudSet;
	private Set<String> bookmarksClustersLocalSet;

	@PostConstruct
	private void init() {
		bookmarksClustersCloudSet = new HashSet<>(Arrays.asList(bookmarksClustersCloud));
		bookmarksClustersLocalSet = new HashSet<>(Arrays.asList(bookmarksClustersLocal));
	}

	public void saveBookmarks(List<K8sIngressRouteRow> k8sIngressRouteRows) {
		Set<String> addBookmarksCsv = readAddBookmarksCsv();
		List<String> csvLocal = new ArrayList<>();
		List<String> csvCloud = new ArrayList<>();

		for (K8sIngressRouteRow route : k8sIngressRouteRows) {
			String formatted = "%s,%s".formatted(route.name(), route.route());
			if (bookmarksClustersCloudSet.contains(route.cluster())) {
				csvCloud.add(formatted);
			} else if (bookmarksClustersLocalSet.contains(route.cluster())) {
				csvLocal.add(formatted);
			} else {
				log(wrapRed("bookmarks - неизвестный кластер " + route.cluster()));
			}
		}

		for (String addBookmark : addBookmarksCsv) {
			String[] split = addBookmark.split(",");
			if (split.length != 3) {
				log(wrapRed("bookmarks (add) - skipping, wrong csv: '%s'".formatted(addBookmark)));
				continue;
			}

			String cluster = split[0];
			String row = "%s,%s".formatted(split[1], split[2]);
			if (bookmarksClustersCloudSet.contains(cluster)) {
				csvCloud.add(row);
			} else if (bookmarksClustersLocalSet.contains(cluster)) {
				csvLocal.add(row);
			} else {
				log(wrapRed("bookmarks (add) - неизвестный кластер " + cluster));
			}
		}

		for (String dnsRecord : dnsRequestService.requestDomains()) {
			csvLocal.add("%s,http://%s".formatted(dnsRecord, dnsRecord));
		}

		csvLocal.sort(String.CASE_INSENSITIVE_ORDER);
		csvCloud.sort(String.CASE_INSENSITIVE_ORDER);

		List<String> numberedCsvLocal = IntStream.range(0, csvLocal.size())
				.mapToObj(i -> (i + 1) + "," + csvLocal.get(i))
				.toList();
		List<String> numberedCsvCloud = IntStream.range(0, csvCloud.size())
				.mapToObj(i -> (i + 1) + "," + csvCloud.get(i))
				.toList();


		save(numberedCsvLocal, numberedCsvCloud);
	}

	private void save(List<String> csvLocal, List<String> csvCloud) {
		String header = "id,name,route\n";
		String resultCsvLocal = header + String.join("\n", csvLocal);
		serversManagementS3.uploadBookmarks(bookmarksS3KeyLocal, resultCsvLocal);
		saveTextFileToLocal(bookmarksLocalSyncFileLocal, resultCsvLocal);
		uploadTextFileToLocalInstance(bookmarksLocalWebdavFileLocal, resultCsvLocal);

		String resultCsvCloud = header + String.join("\n", csvCloud);
		serversManagementS3.uploadBookmarks(bookmarksS3KeyCloud, resultCsvCloud);
		saveTextFileToLocal(bookmarksLocalSyncFileCloud, resultCsvCloud);
		uploadTextFileToLocalInstance(bookmarksLocalWebdavFileCloud, resultCsvCloud);
	}

	private void uploadTextFileToLocalInstance(String file, String text) {
		bookmarksWebdavRestTemplate.put("/" + file, text);
	}

	@SneakyThrows
	private void saveTextFileToLocal(Path file, String text) {
		Files.writeString(file, text);
	}

	private Set<String> readAddBookmarksCsv() {
		try {
			try (Stream<String> lines = Files.lines(Paths.get(bookmarksAddFile))) {
				return lines.collect(Collectors.toSet());
			}
		} catch (IOException e) {
			return Set.of();
		}
	}
}
