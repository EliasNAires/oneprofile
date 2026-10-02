package oneprofile.backend.workers.classification;

import java.nio.file.Path;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Writes the pile, with its cleaned descriptions, to a file the labeller reads.
 */
@RestController
class PileExportEndpoint {

	private final PileExportRun export;

	PileExportEndpoint(PileExportRun export) {
		this.export = export;
	}

	@PostMapping("/pile-exports")
	PileExportRun.Report export(@RequestParam String file) {
		return this.export.exportTo(Path.of(file));
	}

}
