package art.aelaort.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.util.List;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class DnsRequestService {
	@Value("${bookmarks.dns.local.zone}")
	private String bookmarksDnsLocalZone;
	@Value("${bookmarks.dns.local.host}")
	private String bookmarksDnsLocalHost;
	@Value("${bookmarks.dns.local.port}")
	private int bookmarksDnsLocalPort;

	public List<String> requestDomains() {
		try {
			return requestDomains0();
		} catch (IOException | ZoneTransferException e) {
			log(wrapRed("dns request error - " + e.getMessage()));
			return List.of();
		}
	}

	private List<String> requestDomains0() throws IOException, ZoneTransferException {
		ZoneTransferIn xfr = buildLocalDnsRequest();
		xfr.run();
		List<Record> records = xfr.getAXFR();
		return records.stream()
				.filter(record -> record.getType() == Type.A)
				.filter(record -> !record.getName().toString().startsWith("*"))
				.filter(record -> !record.getName().toString().startsWith("ns"))
				.map(record -> record.getName().toString())
				.map(str -> str.substring(0, str.length() - 1))
				.toList();
	}

	private ZoneTransferIn buildLocalDnsRequest() throws TextParseException {
		return ZoneTransferIn.newAXFR(
				new Name(bookmarksDnsLocalZone),
				bookmarksDnsLocalHost,
				bookmarksDnsLocalPort,
				null
		);
	}
}
