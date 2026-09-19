package oneprofile.backend.probe;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Probes every Greenhouse board held and records what each one is.
 * <p>
 * A run asks the ATS about every company discovery has found and is paced, so a call takes as long
 * as those requests take.
 */
@RestController
class GreenhouseProbeEndpoint {

	private final BoardProbe probe;

	GreenhouseProbeEndpoint(BoardProbe probe) {
		this.probe = probe;
	}

	@PostMapping("/probes/greenhouse")
	BoardProbe.Report probe() {
		return this.probe.probeAll();
	}

}
