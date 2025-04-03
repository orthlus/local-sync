package art.aelaort;

import art.aelaort.scan_show.ScanShowServersService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class Entrypoint implements CommandLineRunner {
	private final ScanShowServersService scanShow;

	@Override
	public void run(String... args) {
		if (args.length >= 1) {
			switch (args[0]) {
				case "sync", "s" -> 		 scanShow.sync();
				case "sync-all", "sa" -> 	 scanShow.syncAll();
				default -> log("unknown args\n" + usage());
			}
		} else {
			log("at least one arg required");
			log(usage());
			System.exit(1);
		}
	}

	private String usage() {
		return """
				usage:
					sync, s        - quick sync
					sync-all, sa   - long sync all data""";
	}
}
