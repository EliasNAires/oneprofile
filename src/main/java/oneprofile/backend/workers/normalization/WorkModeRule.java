package oneprofile.backend.workers.normalization;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.springframework.stereotype.Component;

/**
 * Reads the work mode of an engineering vacancy from what it declares, in the order of evidence of
 * #86: the location text where it names a mode; otherwise the work-mode field its ATS publishes;
 * otherwise the title; otherwise none. The description is not read (#98).
 * <p>
 * The location and the title are read by the work-mode words the title breakdown takes out of a title
 * (docs/title-breakdown-criterion.md, rule 7), one list for both. A text that names more than one mode
 * reads as hybrid where hybrid is among them, since a hybrid vacancy is often written with its office
 * and its remote days ({@code Hybrid (3x in office/week)}), and otherwise as remote, which a vacancy
 * offered remote or onsite allows. A field is read by its whole value, because a tenant's own text can
 * name a mode it is not ({@code Office Worker (NOT Remote)}); a value the vocabulary does not hold is no
 * answer.
 */
@Component
public class WorkModeRule {

	/** The words that name a work mode in a title or a location, with the mode each names. */
	static final Map<String, WorkModeEnum> WORDS = Map.ofEntries(Map.entry("remote", WorkModeEnum.REMOTE),
			Map.entry("fully remote", WorkModeEnum.REMOTE), Map.entry("remote first", WorkModeEnum.REMOTE),
			Map.entry("work from home", WorkModeEnum.REMOTE), Map.entry("wfh", WorkModeEnum.REMOTE),
			Map.entry("telecommute", WorkModeEnum.REMOTE), Map.entry("telework", WorkModeEnum.REMOTE),
			Map.entry("remoto", WorkModeEnum.REMOTE), Map.entry("remota", WorkModeEnum.REMOTE),
			Map.entry("home office", WorkModeEnum.REMOTE), Map.entry("home based", WorkModeEnum.REMOTE),
			Map.entry("hybrid", WorkModeEnum.HYBRID), Map.entry("híbrido", WorkModeEnum.HYBRID),
			Map.entry("hibrido", WorkModeEnum.HYBRID), Map.entry("onsite", WorkModeEnum.ONSITE),
			Map.entry("on site", WorkModeEnum.ONSITE), Map.entry("in office", WorkModeEnum.ONSITE),
			Map.entry("presencial", WorkModeEnum.ONSITE), Map.entry("office based", WorkModeEnum.ONSITE));

	/** Where a work-mode word names what the work is about rather than where it is done. */
	private static final Set<String> SUBJECTS = Set.of("hybrid cloud", "hybrid infrastructure", "hybrid it",
			"hybrid deployment", "hybrid deployments", "remote sensing", "remote access");

	/**
	 * The values of the ATSs' work-mode fields that name a mode, read as words: Ashby's and Lever's
	 * workplace types, and what Workday's tenants write in their remote type.
	 */
	private static final Map<String, WorkModeEnum> FIELD_VALUES = Map.ofEntries(
			Map.entry("remote", WorkModeEnum.REMOTE), Map.entry("fully remote", WorkModeEnum.REMOTE),
			Map.entry("hybrid", WorkModeEnum.HYBRID), Map.entry("hybrid working", WorkModeEnum.HYBRID),
			Map.entry("onsite", WorkModeEnum.ONSITE), Map.entry("on site", WorkModeEnum.ONSITE),
			Map.entry("fully onsite", WorkModeEnum.ONSITE), Map.entry("onsite job", WorkModeEnum.ONSITE),
			Map.entry("office", WorkModeEnum.ONSITE), Map.entry("in office", WorkModeEnum.ONSITE),
			Map.entry("office worker not remote", WorkModeEnum.ONSITE));

	private final TitleCleaningRule cleaning;

	/**
	 * @param cleaning the rule whose reading of a text's words the location, the field and the title get
	 */
	public WorkModeRule(TitleCleaningRule cleaning) {
		this.cleaning = cleaning;
	}

	/**
	 * Reads one vacancy's work mode.
	 * @param location its location text, or null if it has none
	 * @param field the work-mode field its ATS publishes, as the ATS writes it, or null if it has none
	 * @param title its title as the company wrote it
	 * @return its work mode and the step that decided it, or {@link WorkModeReading#NONE}
	 */
	public WorkModeReading read(String location, String field, String title) {
		WorkModeEnum named = namedIn(location);
		if (named != null) {
			return new WorkModeReading(named, WorkModeEvidenceEnum.LOCATION);
		}
		WorkModeEnum published = (field != null) ? FIELD_VALUES.get(words(field)) : null;
		if (published != null) {
			return new WorkModeReading(published, WorkModeEvidenceEnum.FIELD);
		}
		named = namedIn(title);
		return (named != null) ? new WorkModeReading(named, WorkModeEvidenceEnum.TITLE) : WorkModeReading.NONE;
	}

	/** The mode a text names, by its work-mode words, or null if it names none. */
	private WorkModeEnum namedIn(String text) {
		if (text == null) {
			return null;
		}
		String spaced = " " + words(text) + " ";
		for (String subject : SUBJECTS) {
			spaced = spaced.replace(" " + subject + " ", " ");
		}
		Set<WorkModeEnum> modes = EnumSet.noneOf(WorkModeEnum.class);
		for (Map.Entry<String, WorkModeEnum> word : WORDS.entrySet()) {
			if (spaced.contains(" " + word.getKey() + " ")) {
				modes.add(word.getValue());
			}
		}
		for (WorkModeEnum mode : List.of(WorkModeEnum.HYBRID, WorkModeEnum.REMOTE, WorkModeEnum.ONSITE)) {
			if (modes.contains(mode)) {
				return mode;
			}
		}
		return null;
	}

	/** A text's words, lowercased and one space apart, as cleaning keeps them. */
	private String words(String text) {
		return this.cleaning.whitelisted(text).toLowerCase(Locale.ROOT);
	}

}
