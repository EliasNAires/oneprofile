package oneprofile.backend.storage.normalizedvacancy;

import jakarta.persistence.Tuple;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.vacancy.SecondaryLocation;
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
	 * Lets go of vacancy groups and duplicate marks outside the engineering subset, so a vacancy that
	 * has left it keeps neither. Vacancies still in the subset keep what the last run recorded.
	 * @return how many vacancies outside the subset held a group or duplicate mark
	 */
	@Transactional
	public int clearVacancyGroupsOutsideSubset() {
		return this.repository.clearVacancyGroupsOutsideSubset();
	}

	/**
	 * Records the vacancy groups and duplicates a run found, replacing each vacancy's previous mark.
	 * @param marks the group or the vacancy repeated, by vacancy id
	 * @return how many vacancies this recorded a group or duplicate mark for
	 */
	@Transactional
	public int recordVacancyGroups(Map<Long, GroupMark> marks) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(marks.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(marks.keySet())) {
			normalized.groupedAs(marks.get(normalized.vacancyId()));
			recorded.add(normalized);
		}
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * Reserves ids for new vacancy groups, independent of the vacancies that will belong to them.
	 * @param count how many new groups need ids
	 * @return fresh ids in increasing order, empty when none are needed
	 */
	@Transactional
	public List<Long> nextGroupIds(int count) {
		return (count == 0) ? List.of() : this.repository.nextGroupIds(count);
	}

	/**
	 * How many open vacancies each company holds: a vacancy is held only while it remains on its board.
	 * @return the count by company id, including zero for a board holding none
	 */
	@Transactional(readOnly = true)
	public Map<Long, Integer> openVacanciesByCompany() {
		Map<Long, Integer> counts = new HashMap<>();
		for (Tuple row : this.repository.openVacanciesByCompany()) {
			counts.put(row.get(0, Long.class), Math.toIntExact(row.get(1, Long.class)));
		}
		return counts;
	}

	/**
	 * The description segments of the given vacancies, read apart from the light vacancy-group scope.
	 * @param vacancyIds the vacancies whose descriptions the pass needs
	 * @return their segments by vacancy id, empty where cleaning has cut none
	 */
	@Transactional(readOnly = true)
	public Map<Long, List<Segment>> segmentsOf(Collection<Long> vacancyIds) {
		Map<Long, List<Segment>> segments = new HashMap<>();
		if (vacancyIds.isEmpty()) {
			return segments;
		}
		vacancyIds.forEach((vacancyId) -> segments.put(vacancyId, List.of()));
		for (Tuple row : this.repository.segmentsOf(vacancyIds)) {
			@SuppressWarnings("unchecked")
			List<Segment> held = (List<Segment>) row.get(1);
			segments.put(row.get(0, Long.class), (held != null) ? held : List.of());
		}
		return segments;
	}

	/**
	 * The whole engineering subset in vacancy id order, with the title evidence and declared locations
	 * the vacancy-group pass reads. Descriptions are read apart, only for the vacancies it needs.
	 * @return every vacancy of the subset, with its stored group where it has one
	 */
	@Transactional(readOnly = true)
	public List<GroupVacancy> vacancyGroupScope() {
		List<Tuple> read = this.repository.vacancyGroupScope();
		if (read.isEmpty()) {
			return List.of();
		}
		Map<Long, Set<SeniorityLevelEnum>> named = new HashMap<>();
		for (Tuple row : this.repository.vacancyGroupTitleSeniorities()) {
			named.computeIfAbsent(row.get(0, Long.class), (vacancyId) -> EnumSet.noneOf(SeniorityLevelEnum.class))
				.add(SeniorityLevelEnum.valueOf(row.get(1, String.class)));
		}
		List<GroupVacancy> scope = new ArrayList<>(read.size());
		for (Tuple row : read) {
			long vacancyId = row.get(0, Long.class);
			String[] titles = row.get(10, String[].class);
			scope.add(new GroupVacancy(vacancyId, row.get(7, Long.class), row.get(1, AtsEnum.class),
					row.get(8, String.class), row.get(9, String.class), (titles != null) ? List.of(titles) : null,
					row.get(11, ManagementLevelEnum.class), named.getOrDefault(vacancyId, Set.of()),
					declaredLocations(row).stream().map(LocationString::location).toList(),
					WorkModeEnum.setOf(row.get(12, String[].class)), row.get(13, Instant.class), row.get(14, Long.class)));
		}
		return scope;
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
	 * The engineering subset held after one vacancy id, in id order, each with its ATS, title, location,
	 * work-mode field and description segments: what the work-mode run reads, walked batch by batch like the titles are.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return the vacancies, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<WorkModeVacancy> workModeScopeAfter(long after, int batch) {
		return this.repository.workModeScopeAfter(after, Limit.of(batch));
	}

	/**
	 * Lets go of the work modes every run recorded, so that after a run only the vacancies it gave one hold
	 * one: a vacancy that has left the engineering subset keeps none.
	 * @return how many vacancies had a work mode
	 */
	@Transactional
	public int clearWorkModes() {
		return this.repository.clearWorkModes();
	}

	/**
	 * Records the work modes the work-mode rule read for a batch of vacancies. A vacancy that declares none
	 * is not given, and keeps none.
	 * @param read the work modes of each vacancy that declares one, every mode it offers a choice of, by
	 * vacancy id
	 * @return how many vacancies this recorded work modes for
	 */
	@Transactional
	public int recordWorkModes(Map<Long, Set<WorkModeEnum>> read) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(read.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(read.keySet())) {
			normalized.workingAs(read.get(normalized.vacancyId()));
			recorded.add(normalized);
		}
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * The engineering subset held after one vacancy id, in id order, each with every level its title names
	 * and its description's segments: what the seniority pass reads, walked batch by batch like the titles
	 * are.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return the vacancies, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<SeniorityVacancy> seniorityScopeAfter(long after, int batch) {
		// The levels a title names are two collections, which no constructor expression can carry beside the
		// segments, so they are read for the whole batch apart and joined here.
		List<Tuple> read = this.repository.seniorityScopeAfter(after, Limit.of(batch));
		if (read.isEmpty()) {
			return List.of();
		}
		Map<Long, Set<SeniorityLevelEnum>> named = new HashMap<>();
		List<Long> vacancyIds = read.stream().map((row) -> row.get(0, Long.class)).toList();
		for (Tuple row : this.repository.titleSenioritiesOf(vacancyIds)) {
			named.computeIfAbsent(row.get(0, Long.class), (vacancyId) -> EnumSet.noneOf(SeniorityLevelEnum.class))
				.add(SeniorityLevelEnum.valueOf(row.get(1, String.class)));
		}
		List<SeniorityVacancy> scope = new ArrayList<>(read.size());
		for (Tuple row : read) {
			long vacancyId = row.get(0, Long.class);
			@SuppressWarnings("unchecked")
			List<Segment> segments = (List<Segment>) row.get(2);
			scope.add(new SeniorityVacancy(vacancyId, row.get(1, String.class), named.getOrDefault(vacancyId, Set.of()),
					(segments != null) ? segments : List.of()));
		}
		return scope;
	}

	/**
	 * Lets go of the seniority every run recorded, so that after a run only the vacancies it read hold one:
	 * a vacancy that has left the engineering subset keeps none.
	 * @return how many vacancies had a seniority level or required years
	 */
	@Transactional
	public int clearSeniorities() {
		this.repository.clearExperienceRequirements();
		return this.repository.clearSeniorities();
	}

	/**
	 * Records the seniority the seniority pass derived for a batch of vacancies.
	 * @param derived the seniority of each vacancy, by vacancy id
	 * @return how many vacancies this recorded a seniority for
	 */
	@Transactional
	public int recordSeniorities(Map<Long, VacancySeniority> derived) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(derived.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(derived.keySet())) {
			normalized.placedAt(derived.get(normalized.vacancyId()));
			recorded.add(normalized);
		}
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * The engineering subset held after one vacancy id, in id order, each with every string of its declared
	 * location: what the place run reads, walked batch by batch like the titles are. The location field comes
	 * first, with the country Ashby gives its address; then Ashby's secondary locations, each with its
	 * country, or Lever's and Workday's every location past the first, which is the location field.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return the vacancies, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<PlaceVacancy> placeScopeAfter(long after, int batch) {
		List<PlaceVacancy> scope = new ArrayList<>();
		for (Tuple row : this.repository.placeScopeAfter(after, Limit.of(batch))) {
			scope.add(new PlaceVacancy(row.get(0, Long.class), row.get(1, AtsEnum.class), declaredLocations(row)));
		}
		return scope;
	}

	/**
	 * Assembles the location field and the ATS's location lists in publication order.
	 * @param row the tuple with the location field and ATS location fields at positions 2 through 6
	 * @return every declared location string, skipping the first list entry when it repeats the field
	 */
	private static List<LocationString> declaredLocations(Tuple row) {
		List<LocationString> locations = new ArrayList<>();
		String location = row.get(2, String.class);
		if (location != null && !location.isBlank()) {
			locations.add(new LocationString(LocationListEnum.LOCATION, 0, location, row.get(3, String.class)));
		}
		@SuppressWarnings("unchecked")
		List<SecondaryLocation> secondary = (List<SecondaryLocation>) row.get(4);
		if (secondary != null) {
			for (int i = 0; i < secondary.size(); i++) {
				locations.add(new LocationString(LocationListEnum.SECONDARY_LOCATIONS, i,
						secondary.get(i).location(), secondary.get(i).country()));
			}
		}
		@SuppressWarnings("unchecked")
		List<String> all = (List<String>) ((row.get(5) != null) ? row.get(5) : row.get(6));
		if (all != null) {
			for (int i = 0; i < all.size(); i++) {
				if (i > 0 || !all.get(i).equals(location)) {
					locations.add(new LocationString(LocationListEnum.ALL_LOCATIONS, i, all.get(i), null));
				}
			}
		}
		return locations;
	}

	/**
	 * Lets go of the places every run recorded, so that after a run only the vacancies it read hold any: a
	 * vacancy that has left the engineering subset keeps none.
	 * @return how many location strings had been read
	 */
	@Transactional
	public int clearPlaces() {
		return this.repository.clearPlaces();
	}

	/**
	 * Records the places the place rule read in the location strings of a batch of vacancies.
	 * @param read each vacancy's location strings with their places, by vacancy id
	 * @return how many vacancies this recorded places for
	 */
	@Transactional
	public int recordPlaces(Map<Long, List<LocationPlaces>> read) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(read.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(read.keySet())) {
			normalized.locatedAt(read.get(normalized.vacancyId()));
			recorded.add(normalized);
		}
		this.repository.saveAll(recorded);
		return recorded.size();
	}

	/**
	 * The engineering subset held after one vacancy id, in id order, each with its work modes, title, the places
	 * of its location strings and its description's segments: what the country-set run reads, walked batch by
	 * batch like the titles are. It reads the places the last place run recorded.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param batch how many to read at most
	 * @return the vacancies, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<CountrySetVacancy> countrySetScopeAfter(long after, int batch) {
		// The location strings are a collection, which no constructor expression can carry beside the segments,
		// so they are read for the whole batch apart and joined here.
		List<Tuple> read = this.repository.countrySetScopeAfter(after, Limit.of(batch));
		if (read.isEmpty()) {
			return List.of();
		}
		Map<Long, List<LocationPlaces>> located = new HashMap<>();
		List<Long> vacancyIds = read.stream().map((row) -> row.get(0, Long.class)).toList();
		for (Tuple row : this.repository.locationsOf(vacancyIds)) {
			located.computeIfAbsent(row.get(0, Long.class), (vacancyId) -> new ArrayList<>())
				.add(row.get(1, LocationPlaces.class));
		}
		List<CountrySetVacancy> scope = new ArrayList<>(read.size());
		for (Tuple row : read) {
			long vacancyId = row.get(0, Long.class);
			List<LocationPlaces> locations = new ArrayList<>(located.getOrDefault(vacancyId, List.of()));
			locations.sort(Comparator.comparing(LocationPlaces::list).thenComparingInt(LocationPlaces::position));
			@SuppressWarnings("unchecked")
			List<Segment> segments = (List<Segment>) row.get(3);
			scope.add(new CountrySetVacancy(vacancyId, WorkModeEnum.setOf(row.get(1, String[].class)), row.get(2, String.class),
					locations, (segments != null) ? segments : List.of()));
		}
		return scope;
	}

	/**
	 * Lets go of the country set every run recorded, so that after a run only the vacancies it read hold one: a
	 * vacancy that has left the engineering subset keeps none.
	 * @return how many vacancies had been read
	 */
	@Transactional
	public int clearCountrySets() {
		this.repository.clearStatedSets();
		return this.repository.clearOffers();
	}

	/**
	 * Records the country sets the country-set pass read for a batch of vacancies.
	 * @param read the country set of each vacancy, by vacancy id
	 * @return how many vacancies this recorded a country set for
	 */
	@Transactional
	public int recordCountrySets(Map<Long, CountrySet> read) {
		List<NormalizedVacancyEntity> recorded = new ArrayList<>(read.size());
		for (NormalizedVacancyEntity normalized : this.repository.findByVacancyIdIn(read.keySet())) {
			normalized.statingAs(read.get(normalized.vacancyId()));
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
