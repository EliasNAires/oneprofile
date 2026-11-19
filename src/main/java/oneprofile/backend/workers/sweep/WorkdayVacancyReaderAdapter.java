package oneprofile.backend.workers.sweep;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.normalizedvacancy.Classification;
import oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.PublishedWorkdayVacancy;
import oneprofile.backend.storage.vacancy.VacancyStore;
import oneprofile.backend.workers.WorkdayApiPort;
import oneprofile.backend.workers.classification.TitleClassificationRule;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Reads every vacancy of a Workday board from its careers-site API, as
 * docs/measurements/2026-10-workday.md measured it.
 * <p>
 * The board's list is paged 20 at a time and names each vacancy's title, path and location, with no
 * description; the board's {@code total} comes on the first page, on some boards on no other. Workday
 * stops paging at an offset of 2000, so a board of 2000 or more is read in parts: narrowed by the
 * facet whose values take the fewest pages, then add up to the fewest vacancies; a part still too
 * large is narrowed by a further facet. A vacancy found in two parts, as one in two locations is, is
 * held once.
 * <p>
 * A first, unfaceted list page answering {@code 400}, {@code 401}, {@code 403}, {@code 404} or
 * {@code 422} means no board is published under the slug and throws {@link BoardNotFoundException}.
 * Other failures to read a list page throw {@link IOException}, including those answers on later
 * pages or under facets.
 * <p>
 * A description takes a request a vacancy, so it is asked for only where the title classification
 * leaves the vacancy {@code IN}, or {@code UNKNOWN} for a reason the body pass reads it for: a million
 * descriptions would take 18 hours, and the body pass never reads the rest (decided with Elias on
 * 2026-10-23 and 2026-10-24). The title is classified with the rules as they stand when the sweep
 * runs; a vacancy they rule in later has its description read by the next sweep. Before reading a
 * board, its held vacancies with descriptions are read by external id. One whose id and title still
 * match the list is returned as held without asking for its page, with no company name because the
 * vacancy holds none. A vacancy whose page answers {@code 404} has closed since the list was read,
 * and is left out; one whose page Workday answers with another error of its own is held as the list
 * names it, so that one vacancy does not cost its board. Its page also names the company, which the
 * sweep takes as its name.
 */
@Component
public class WorkdayVacancyReaderAdapter implements VacancyReaderPort {

	private static final Log logger = LogFactory.getLog(WorkdayVacancyReaderAdapter.class);

	private static final String VACANCY_URL = "https://%s.%s.myworkdayjobs.com/%s%s";

	/** The most Workday answers a page with. */
	private static final int PAGE_SIZE = 20;

	/** The offset Workday stops paging at. */
	private static final int PAGING_LIMIT = 2000;

	/** What Workday answers when no public board is published, as in the probe. */
	private static final Set<Integer> NO_BOARD = Set.of(400, 401, 403, 404, 422);

	private final WorkdayApiPort api;

	private final VacancyStore vacancies;

	private final TitleCleaningRule titleCleaning;

	private final TitleClassificationRule titleClassification;

	private final int pageSize;

	private final int pagingLimit;

	/**
	 * Reads from Workday, asking for the descriptions the title rules call for.
	 * @param api what asks Workday
	 * @param vacancies what holds the descriptions already read
	 * @param titleCleaning cleans a title before it is classified
	 * @param titleClassification classifies a cleaned title
	 */
	@Autowired
	public WorkdayVacancyReaderAdapter(WorkdayApiPort api, VacancyStore vacancies, TitleCleaningRule titleCleaning,
			TitleClassificationRule titleClassification) {
		this(api, vacancies, titleCleaning, titleClassification, PAGE_SIZE, PAGING_LIMIT);
	}

	/**
	 * @param api what asks Workday
	 * @param vacancies what holds the descriptions already read
	 * @param titleCleaning cleans a title before it is classified
	 * @param titleClassification classifies a cleaned title
	 * @param pageSize how many vacancies a page asks for
	 * @param pagingLimit the offset Workday stops paging at
	 */
	WorkdayVacancyReaderAdapter(WorkdayApiPort api, VacancyStore vacancies, TitleCleaningRule titleCleaning,
			TitleClassificationRule titleClassification, int pageSize, int pagingLimit) {
		this.api = api;
		this.vacancies = vacancies;
		this.titleCleaning = titleCleaning;
		this.titleClassification = titleClassification;
		this.pageSize = pageSize;
		this.pagingLimit = pagingLimit;
	}

	/** Reads a board and logs its request counts, including a read that fails. */
	@Override
	public List<PublishedVacancy> read(String slug, RegionEnum region, Runnable onRefusal) throws IOException {
		Pages pages = new Pages();
		boolean failed = true;
		try {
			Map<String, PublishedWorkdayVacancy> held = this.vacancies.describedWorkdayVacancies(slug);
			Map<String, JsonNode> postings = new LinkedHashMap<>();
			readPart(slug, new LinkedHashMap<>(), postings, onRefusal, pages);
			List<PublishedVacancy> published = new ArrayList<>(postings.size());
			for (JsonNode posting : postings.values()) {
				publishedVacancy(slug, posting, held, onRefusal, pages).ifPresent(published::add);
			}
			failed = false;
			return published;
		}
		finally {
			logger.info("WORKDAY_BOARD slug=%s list_pages=%d description_pages=%d descriptions_held=%d parts=%d failed=%s"
				.formatted(slug, pages.list, pages.description, pages.held, pages.parts, failed));
		}
	}

	/** Reads every vacancy under the facets applied into {@code postings}, by path. */
	private void readPart(String slug, Map<String, List<String>> appliedFacets, Map<String, JsonNode> postings,
			Runnable onRefusal, Pages pages) throws IOException {
		pages.parts++;
		JsonNode first = page(slug, appliedFacets, 0, onRefusal, pages);
		int total = first.path("total").asInt(0);
		if (total >= this.pagingLimit) {
			Optional<Facet> facet = narrowest(first.path("facets"), appliedFacets);
			if (facet.isPresent()) {
				pages.parts--;
				for (String value : facet.get().values()) {
					Map<String, List<String>> narrowed = new LinkedHashMap<>(appliedFacets);
					narrowed.put(facet.get().parameter(), List.of(value));
					readPart(slug, narrowed, postings, onRefusal, pages);
				}
				return;
			}
			logger.warn("The Workday board of %s holds %d or more vacancies under %s and no facet narrows it, so only %d are read"
				.formatted(slug, this.pagingLimit, appliedFacets, this.pagingLimit));
		}
		collect(first, postings);
		int end = Math.min(total, this.pagingLimit);
		for (int offset = this.pageSize; offset < end; offset += this.pageSize) {
			if (!collect(page(slug, appliedFacets, offset, onRefusal, pages), postings)) {
				break;
			}
		}
	}

	/** Asks for a list page, counting the request even if it fails. */
	private JsonNode page(String slug, Map<String, List<String>> appliedFacets, int offset, Runnable onRefusal, Pages pages)
			throws IOException {
		pages.list++;
		WorkdayApiPort.Answer answer = this.api.jobs(slug, appliedFacets, offset, this.pageSize, onRefusal);
		if (offset == 0 && appliedFacets.isEmpty() && NO_BOARD.contains(answer.status())) {
			throw new BoardNotFoundException("Asked for the vacancies of %s under %s from %d and got %d"
				.formatted(slug, appliedFacets, offset, answer.status()));
		}
		if (answer.status() != 200 || !answer.body().path("jobPostings").isArray()) {
			throw new IOException("Asked for the vacancies of %s under %s from %d and got %d".formatted(slug,
					appliedFacets, offset, answer.status()));
		}
		return answer.body();
	}

	/** Adds a page's vacancies by path, and says whether it held any. */
	private static boolean collect(JsonNode page, Map<String, JsonNode> postings) {
		JsonNode jobPostings = page.path("jobPostings");
		for (JsonNode posting : jobPostings) {
			String path = text(posting.path("externalPath"));
			if (path != null) {
				postings.putIfAbsent(path, posting);
			}
		}
		return !jobPostings.isEmpty();
	}

	/**
	 * The facet to narrow a part by: of those not applied yet that split it, into two values or more,
	 * the one whose values take the fewest pages, then the fewest vacancies. A value still too large is
	 * narrowed further, so a facet of a few large values beats one of thousands of small ones: on CVS's
	 * board, a time type of two values takes about 930 pages and its locations 5266 (#90). Failing any
	 * that splits it, the one whose largest value holds fewest. Workday groups its location facets under
	 * one that names no values of its own, which is looked inside.
	 */
	private Optional<Facet> narrowest(JsonNode facets, Map<String, List<String>> appliedFacets) {
		List<Facet> candidates = new ArrayList<>();
		for (JsonNode facet : facets) {
			addFacet(facet, appliedFacets, candidates);
		}
		Comparator<Facet> fewest = Comparator.comparingLong(Facet::sum);
		Optional<Facet> splitting = candidates.stream()
			.filter((facet) -> facet.values().size() > 1)
			.min(Comparator.comparingLong(Facet::pages).thenComparing(fewest));
		return splitting.isPresent() ? splitting
				: candidates.stream().min(Comparator.comparingLong(Facet::largest).thenComparing(fewest));
	}

	/** Adds a facet, and each facet nested among its values, to the candidates unless it is applied. */
	private void addFacet(JsonNode facet, Map<String, List<String>> appliedFacets, List<Facet> candidates) {
		String parameter = text(facet.path("facetParameter"));
		List<String> values = new ArrayList<>();
		long sum = 0;
		long pages = 0;
		long largest = 0;
		for (JsonNode value : facet.path("values")) {
			if (value.has("facetParameter")) {
				addFacet(value, appliedFacets, candidates);
				continue;
			}
			long count = value.path("count").asLong(0);
			String id = text(value.path("id"));
			if (id != null && count > 0) {
				values.add(id);
				sum += count;
				pages += (count - 1) / this.pageSize + 1;
				largest = Math.max(largest, count);
			}
		}
		if (parameter != null && !values.isEmpty() && !appliedFacets.containsKey(parameter)) {
			candidates.add(new Facet(parameter, values, sum, largest, pages));
		}
	}

	/** The vacancy, with its page read if its title calls for it, or empty if it has closed since. */
	private Optional<PublishedVacancy> publishedVacancy(String slug, JsonNode posting,
			Map<String, PublishedWorkdayVacancy> held, Runnable onRefusal, Pages pages) throws IOException {
		String path = text(posting.path("externalPath"));
		String externalId = path.substring(path.lastIndexOf('/') + 1);
		String title = text(posting.path("title"));
		PublishedWorkdayVacancy described = held.get(externalId);
		if (described != null && Objects.equals(described.title(), title)) {
			pages.held++;
			return Optional.of(described);
		}
		PublishedWorkdayVacancy listed = new PublishedWorkdayVacancy(externalId, title,
				text(posting.path("locationsText")), null, url(slug, path), null, null, null, null, null, null);
		if (!descriptionCalledFor(title)) {
			return Optional.of(listed);
		}
		pages.description++;
		WorkdayApiPort.Answer answer = this.api.job(slug, path, onRefusal);
		if (answer.status() == 404) {
			return Optional.empty();
		}
		JsonNode info = answer.body().path("jobPostingInfo");
		if (answer.status() != 200 || !info.isObject()) {
			logger.warn("Asked for the vacancy %s of %s and got %d, so it is held as the list names it"
				.formatted(path, slug, answer.status()));
			return Optional.of(listed);
		}
		String location = text(info.path("location"));
		List<String> allLocations = new ArrayList<>();
		if (location != null) {
			allLocations.add(location);
		}
		for (JsonNode additional : info.path("additionalLocations")) {
			if (additional.isString()) {
				allLocations.add(additional.stringValue());
			}
		}
		String url = text(info.path("externalUrl"));
		return Optional.of(new PublishedWorkdayVacancy(externalId, orElse(text(info.path("title")), title),
				orElse(location, text(posting.path("locationsText"))),
				DescriptionMarkingRule.markedLines(text(info.path("jobDescription"))), orElse(url, url(slug, path)),
				day(text(info.path("startDate"))), text(info.path("timeType")), text(info.path("remoteType")),
				text(info.path("jobRequisitionLocation").path("country").path("alpha2Code")), allLocations,
				text(answer.body().path("hiringOrganization").path("name"))));
	}

	/**
	 * Whether the body pass may read the vacancy's description: its title is {@code IN}, or
	 * {@code UNKNOWN} for a reason that puts it in the pile.
	 */
	private boolean descriptionCalledFor(String title) {
		if (title == null || title.isBlank()) {
			return false;
		}
		Classification classification = this.titleClassification.classify(this.titleCleaning.clean(title).title());
		return classification.state() == ClassificationStateEnum.IN
				|| (classification.state() == ClassificationStateEnum.UNKNOWN
						&& Classification.PILE_REASONS.contains(classification.reason()));
	}

	/** Where a vacancy is published, as its board's own address names it. */
	private static String url(String slug, String path) {
		String[] board = slug.split("[./]", 3);
		return VACANCY_URL.formatted(board[0], board[1], board[2], path);
	}

	/** Workday writes the day a vacancy was posted as a date, taken here as that day's start in UTC. */
	private static Instant day(String date) {
		if (date == null) {
			return null;
		}
		try {
			return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
		}
		catch (DateTimeParseException ex) {
			return null;
		}
	}

	private static String orElse(String text, String otherwise) {
		return (text != null) ? text : otherwise;
	}

	private static String text(JsonNode node) {
		return node.isString() ? node.stringValue() : null;
	}

	/**
	 * One facet a board can be narrowed by.
	 *
	 * @param parameter its name, as a page names it and a request applies it
	 * @param values the id of every value that holds a vacancy
	 * @param sum how many vacancies its values hold, those in several counted in each
	 * @param largest how many its largest value holds
	 * @param pages how many list pages its values take in all
	 */
	private record Facet(String parameter, List<String> values, long sum, long largest, long pages) {
	}

	/** Request counts owned by one read, never shared by concurrent boards. */
	private static final class Pages {

		private int list;

		private int description;

		private int held;

		private int parts;

	}

}
