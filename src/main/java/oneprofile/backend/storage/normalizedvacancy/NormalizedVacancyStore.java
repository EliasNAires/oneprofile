package oneprofile.backend.storage.normalizedvacancy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the facts the rules derive from the corpus. A pass over the corpus is re-runnable, so what
 * it records for a vacancy replaces what the last run recorded rather than adding a second row.
 */
@Component
public class NormalizedVacancyStore {

	private final NormalizedVacancyRepository repository;

	public NormalizedVacancyStore(NormalizedVacancyRepository repository) {
		this.repository = repository;
	}

	/**
	 * The cleaned titles held after one vacancy id, in id order. A pass that reads titles rather
	 * than bodies walks the corpus with this, batch by batch, resuming from the last id it read.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return their vacancy ids and cleaned titles, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<ClassificationVacancy> cleanedTitlesAfter(long after, int batch) {
		return this.repository.cleanedTitlesAfter(after, Limit.of(batch));
	}

	/**
	 * Records what classification made of a batch of cleaned titles. A vacancy cleaning has not
	 * reached has nothing to classify, so it is not here and nothing is written for it.
	 * @param classified what the rules decided for each vacancy, by vacancy id
	 * @return how many vacancies this recorded a classification for
	 */
	@Transactional
	public int recordClassifications(Map<Long, Classification> classified) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(classified.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(classified.keySet())) {
			normalized.classifiedAs(classified.get(normalized.vacancyId()));
			recorded.add(normalized);
		}
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * The pile held after one vacancy id, in id order, each with its language and its segments: the
	 * vacancies the body pass reads, walked batch by batch like the titles are. Those it skipped for
	 * their language are read too, with the reason that says so.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return the pile's vacancies, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<PileVacancy> pileAfter(long after, int batch) {
		return this.repository.pileAfter(Classification.PILE_HELD_REASONS, after, Limit.of(batch));
	}

	/**
	 * What skill extraction reads after one vacancy id, in id order, each with its title and segments:
	 * the vacancies the title decided in, and the pile without those in a language the body pass does
	 * not read (ADR-0017), walked batch by batch like the titles are.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return the vacancies, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<ExtractionVacancy> extractionScopeAfter(long after, int batch) {
		return this.repository.extractionScopeAfter(Classification.PILE_REASONS, PileVacancy.SUPPORTED_LANGUAGES,
				after, Limit.of(batch));
	}

	/**
	 * Puts the pile back to what the title stage left it, so the body pass can be run again without
	 * the title pass: every vacancy of it unknown, decided by the title, with its reason kept. One the
	 * body pass skipped for its language has lost its title's reason, and is left as it is.
	 * @return how many vacancies the pile holds
	 */
	@Transactional
	public int resetPile() {
		return this.repository.resetPile(Classification.PILE_REASONS);
	}

	/**
	 * Records what the body decided of a batch of the pile. Only decided vacancies are given; one
	 * the body leaves unknown keeps what the title left it.
	 * @param decided what the body decided, {@code IN} or {@code OUT}, by vacancy id
	 * @return how many vacancies this recorded a decision for
	 */
	@Transactional
	public int recordBodyDecisions(Map<Long, ClassificationStateEnum> decided) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(decided.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(decided.keySet())) {
			normalized.decidedByBody(decided.get(normalized.vacancyId()));
			recorded.add(normalized);
		}
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * Records that the body pass skipped vacancies of the pile for the language they are written in.
	 * @param skipped the vacancies it skipped
	 * @return how many vacancies this recorded it for
	 */
	@Transactional
	public int recordUnsupportedLanguage(Set<Long> skipped) {
		List<NormalizedVacancyEntity> recorded = this.repository.findByVacancyIdIn(skipped);
		recorded.forEach((normalized) -> normalized.classifiedAs(Classification.unsupportedLanguage()));
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * The engineering subset held after one vacancy id, in id order, each with its title as published
	 * and its location field: what the title breakdown reads, walked batch by batch like the titles are.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return the vacancies, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<BreakdownVacancy> breakdownScopeAfter(long after, int batch) {
		return this.repository.breakdownScopeAfter(after, Limit.of(batch));
	}

	/**
	 * Lets go of what every title breakdown recorded, so that after a run only the vacancies it read
	 * hold one: a vacancy that has left the engineering subset keeps none.
	 * @return how many vacancies had been broken down
	 */
	@Transactional
	public int clearTitleBreakdowns() {
		this.repository.clearGuardedTitleSeniorities();
		return this.repository.clearTitleBreakdowns();
	}

	/**
	 * Records what the title breakdown made of a batch of vacancies.
	 * @param brokenDown the breakdown of each vacancy's title, by vacancy id
	 * @return how many vacancies this recorded a breakdown for
	 */
	@Transactional
	public int recordTitleBreakdowns(Map<Long, TitleBreakdown> brokenDown) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(brokenDown.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(brokenDown.keySet())) {
			normalized.brokenDownAs(brokenDown.get(normalized.vacancyId()));
			recorded.add(normalized);
		}
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * The engineering subset held after one vacancy id, in id order, each with the normalized titles the
	 * title breakdown wrote for it: what the role assignment reads, walked batch by batch like the titles
	 * are. A vacancy the breakdown has not reached is left out.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return the vacancies, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<RoleVacancy> roleScopeAfter(long after, int batch) {
		return this.repository.roleScopeAfter(after, Limit.of(batch));
	}

	/**
	 * Lets go of the roles every assignment recorded, so that after a run only the vacancies it read hold
	 * any: a vacancy that has left the engineering subset keeps none.
	 * @return how many vacancies had been given roles
	 */
	@Transactional
	public int clearRoles() {
		return this.repository.clearRoles();
	}

	/**
	 * Records the roles the role rule gave a batch of vacancies.
	 * @param assigned the slugs of each vacancy's roles, empty for one with none, by vacancy id
	 * @return how many vacancies this recorded roles for
	 */
	@Transactional
	public int recordRoles(Map<Long, List<String>> assigned) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(assigned.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(assigned.keySet())) {
			normalized.assignedRoles(assigned.get(normalized.vacancyId()));
			recorded.add(normalized);
		}
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * Records what cleaning made of a batch of vacancies. A batch is one transaction, because a pass
	 * over the corpus is too large to be one and is re-runnable anyway.
	 * @param cleaned the cleaned title and description segments of each vacancy, by vacancy id
	 * @return how many vacancies this recorded a cleaning for
	 */
	@Transactional
	public int recordCleanedVacancies(Map<Long, CleanedVacancy> cleaned) {
		Map<Long, NormalizedVacancyEntity> held = new HashMap<>();
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(cleaned.keySet())) {
			held.put(normalized.vacancyId(), normalized);
		}
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(cleaned.size());
		cleaned.forEach((vacancyId, vacancy) -> {
			NormalizedVacancyEntity normalized = held.computeIfAbsent(vacancyId, NormalizedVacancyEntity::new);
			normalized.cleanedAs(vacancy);
			recorded.add(normalized);
		});
		this.repository.saveAll(recorded);
		return recorded.size();
	}

}
