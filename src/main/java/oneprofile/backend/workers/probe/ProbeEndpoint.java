package oneprofile.backend.workers.probe;

import oneprofile.backend.storage.company.AtsEnum;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Probes every board of one ATS held and records what each one is.
 * <p>
 * A run asks the ATS about every company discovery has found and is paced, so a call takes as long
 * as those requests take.
 */
@RestController
class ProbeEndpoint {

	private final BoardProbeRun probe;

	ProbeEndpoint(BoardProbeRun probe) {
		this.probe = probe;
	}

	@PostMapping("/probes/{ats}")
	BoardProbeRun.Report probe(@PathVariable String ats) {
		return this.probe.probeAll(AtsEnum.named(ats));
	}

}
