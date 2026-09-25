package oneprofile.backend.workers.cleaning;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cleans the title of every vacancy in the corpus.
 * <p>
 * The rules are pure functions over a title, so a run takes as long as reading the corpus and
 * writing a row per vacancy takes, and running it again over an unchanged corpus writes what the
 * last run wrote.
 */
@RestController
class CleaningEndpoint {

	private final CorpusCleaningRun cleaning;

	CleaningEndpoint(CorpusCleaningRun cleaning) {
		this.cleaning = cleaning;
	}

	@PostMapping("/cleanings")
	CorpusCleaningRun.Report clean() {
		return this.cleaning.cleanAll();
	}

}
