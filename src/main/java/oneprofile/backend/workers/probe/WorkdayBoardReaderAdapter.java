package oneprofile.backend.workers.probe;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.workers.WorkdayApiPort;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Reads Workday boards from their careers-site API, one request a board: a page of one vacancy says
 * whether a board has any (docs/measurements/2026-10-workday.md).
 * <p>
 * Workday says a board cannot be read with an error of its own: {@code 422} for a tenant or site that
 * does not exist, {@code 404} for a site it does not know, {@code 401} and {@code 403} for one that
 * is not public, and {@code 400} for a site read from a URL that is no site. None is a refusal, and
 * each means no public board is there. Workday names the company only in a vacancy, which a probe
 * does not read, so a probe brings no name. The region is not read: Workday's hosts are not regions.
 */
@Component
public class WorkdayBoardReaderAdapter implements BoardReaderPort {

	private static final Set<Integer> NO_BOARD = Set.of(400, 401, 403, 404, 422);

	private final WorkdayApiPort api;

	public WorkdayBoardReaderAdapter(WorkdayApiPort api) {
		this.api = api;
	}

	@Override
	public BoardReading read(String slug, RegionEnum region, Runnable onRefusal) throws IOException {
		WorkdayApiPort.Answer answer = this.api.jobs(slug, Map.of(), 0, 1, onRefusal);
		if (NO_BOARD.contains(answer.status())) {
			return new BoardReading(BoardStatusEnum.NOT_FOUND, null);
		}
		if (answer.status() != 200) {
			throw new IOException("Asked for the board of %s and got %d".formatted(slug, answer.status()));
		}
		JsonNode postings = answer.body().path("jobPostings");
		if (!postings.isArray()) {
			throw new IOException("The answer for the board of %s named no vacancies".formatted(slug));
		}
		return new BoardReading(postings.isEmpty() ? BoardStatusEnum.EMPTY : BoardStatusEnum.ACTIVE, null);
	}

}
