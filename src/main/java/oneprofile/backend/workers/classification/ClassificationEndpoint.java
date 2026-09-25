package oneprofile.backend.workers.classification;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Classifies every cleaned title in the corpus.
 * <p>
 * The rules are pure functions over a cleaned title, so running it again over an unchanged corpus
 * writes what the last run wrote, and a rule change is measured by running it again.
 */
@RestController
class ClassificationEndpoint {

	private final CorpusClassificationRun classification;

	ClassificationEndpoint(CorpusClassificationRun classification) {
		this.classification = classification;
	}

	@PostMapping("/classifications")
	CorpusClassificationRun.Report classify() {
		return this.classification.classifyAll();
	}

}
