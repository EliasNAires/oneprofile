package oneprofile.backend.workers.extraction;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Extracts the skills of every vacancy in extraction's scope, replacing what the last run found. */
@RestController
class SkillExtractionEndpoint {

	private final SkillExtractionRun extraction;

	SkillExtractionEndpoint(SkillExtractionRun extraction) {
		this.extraction = extraction;
	}

	@PostMapping("/skill-extractions")
	SkillExtractionRun.Report extract() {
		return this.extraction.extractSkills();
	}

}
