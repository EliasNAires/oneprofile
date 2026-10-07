package oneprofile.backend.workers.stoplist;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;

/**
 * The guard set: the single words that are known skills, against which the stoplist's mechanism is
 * tested. No word is kept off the stoplist for being in it; a cutoff or a bar that would stoplist one
 * of its words is too loose.
 */
final class GuardSetRule {

	/**
	 * The bare forms whose decisions 6bfc29c removed from the decision record, once a bare form became
	 * the skill it names (ADR-0015). Kept here because the record no longer holds them.
	 */
	static final Set<String> REOPENED = Set.of("make", "access", "go", "catalyst", "apex", "flux", "salt", "emr",
			"nix", "hcl", "arc", "alloy", "lex", "travis", "shiny", "julia", "rac", "chakra", "drizzle", "copilot",
			"fabric", "transformers", "aurora", "glue", "compose", "foundry", "tempo", "sentinel", "triton", "jetpack",
			"beam", "einstein", "unreal", "apollo", "cortex", "gatekeeper", "autopilot", "swarm", "neptune", "spanner",
			"arrow", "rocky", "strands", "radix", "sonnet", "ucs", "joule");

	private GuardSetRule() {
	}

	/**
	 * The names that are one word as {@link Segment} cuts words, so the word a piece of it would be.
	 * @param names names as they are written
	 * @return the single words among them, lowercased
	 */
	static Set<String> words(Collection<String> names) {
		return names.stream()
			.map((name) -> new Segment(SegmentKindEnum.SENTENCE, null, name, false).tokens())
			.filter((tokens) -> tokens.size() == 1)
			.map(List::getFirst)
			.collect(Collectors.toSet());
	}

}
