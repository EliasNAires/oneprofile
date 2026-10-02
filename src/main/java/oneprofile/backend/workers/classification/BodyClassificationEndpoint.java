package oneprofile.backend.workers.classification;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Classifies the pile from its descriptions, after putting it back to what the title stage left it.
 */
@RestController
class BodyClassificationEndpoint {

	private final BodyClassificationRun body;

	BodyClassificationEndpoint(BodyClassificationRun body) {
		this.body = body;
	}

	@PostMapping("/body-classifications")
	BodyClassificationRun.Report classify() {
		return this.body.classifyPile();
	}

}
