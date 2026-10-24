package oneprofile.backend.workers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Asks Workday's careers-site API about one board, named by its slug ({@code <tenant>.<host>/<site>}):
 * a page of its vacancies, or one vacancy. The one API a Workday probe and sweep read, and the one
 * pace they share (docs/measurements/2026-10-workday.md).
 */
public interface WorkdayApiPort {

	/**
	 * Asks for one page of a board's vacancies: their titles and paths, the board's {@code total}, and
	 * the facets that can narrow it. No description.
	 * @param slug the board's slug
	 * @param appliedFacets the facet values the page is narrowed to, by facet, empty for the whole board
	 * @param offset how many vacancies to skip
	 * @param limit how many to read, 20 at most
	 * @param onRefusal run each time Workday's limit refuses a request, retried or not, so the run
	 * asking slows down
	 * @return Workday's answer: a page, or the error with which it says the board cannot be read
	 * @throws IOException if Workday gave no answer about the board
	 */
	Answer jobs(String slug, Map<String, List<String>> appliedFacets, int offset, int limit, Runnable onRefusal)
			throws IOException;

	/**
	 * Asks for one vacancy: its description, locations and dates, and the company's name.
	 * @param slug the board's slug
	 * @param externalPath the vacancy's path as a page names it, such as {@code /job/Santa-Clara/Engineer_JR1}
	 * @param onRefusal run each time Workday's limit refuses a request, retried or not
	 * @return Workday's answer: the vacancy, or the error with which it says it is not there
	 * @throws IOException if Workday gave no answer about the vacancy
	 */
	Answer job(String slug, String externalPath, Runnable onRefusal) throws IOException;

	/**
	 * What Workday answered.
	 *
	 * @param status its HTTP status
	 * @param body its JSON, or a missing node where an error came without any
	 */
	record Answer(int status, JsonNode body) {
	}

}
