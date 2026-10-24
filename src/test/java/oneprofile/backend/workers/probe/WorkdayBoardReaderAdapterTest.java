package oneprofile.backend.workers.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.workers.WorkdayApiPort;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;

class WorkdayBoardReaderAdapterTest {

	private final List<String> asked = new ArrayList<>();

	@Test
	void findsABoardWithAVacancyActiveFromOnePageOfOne() throws IOException {
		WorkdayBoardReaderAdapter reader = readerAnswering(200, """
				{"total":2743,"jobPostings":[{"title":"RTL Design Engineer, DFT",
				"externalPath":"/job/Israel-Yokneam/RTL-Design-Engineer--DFT_JR2028303"}],"facets":[]}""");

		assertThat(reader.read("nvidia.wd5/nvidiaexternalcareersite", RegionEnum.US, () -> {
		})).isEqualTo(new BoardReading(BoardStatusEnum.ACTIVE, null));
		assertThat(this.asked).containsExactly("nvidia.wd5/nvidiaexternalcareersite {} 0 1");
	}

	@Test
	void findsABoardWithNoVacancyEmpty() throws IOException {
		WorkdayBoardReaderAdapter reader = readerAnswering(200, "{\"total\":0,\"jobPostings\":[],\"facets\":[]}");

		assertThat(reader.read("acme.wd1/careers", RegionEnum.US, () -> {
		})).isEqualTo(new BoardReading(BoardStatusEnum.EMPTY, null));
	}

	@Test
	void findsNoBoardWhereWorkdaySaysItCannotBeRead() throws IOException {
		for (int status : List.of(400, 401, 403, 404, 422)) {
			assertThat(readerAnswering(status, "{\"errorCode\":\"S22\",\"httpStatus\":%d}".formatted(status))
				.read("acme.wd1/job", RegionEnum.US, () -> {
				})).isEqualTo(new BoardReading(BoardStatusEnum.NOT_FOUND, null));
		}
	}

	@Test
	void failsOnAnyOtherAnswer() {
		assertThatIOException().isThrownBy(() -> readerAnswering(500, "{}").read("acme.wd1/careers", RegionEnum.US, () -> {
		}));
		assertThatIOException().isThrownBy(() -> readerAnswering(200, "{\"total\":1}").read("acme.wd1/careers",
				RegionEnum.US, () -> {
				}));
	}

	private WorkdayBoardReaderAdapter readerAnswering(int status, String body) {
		JsonNode json = JsonMapper.builder().build().readTree(body);
		return new WorkdayBoardReaderAdapter(new WorkdayApiPort() {

			@Override
			public Answer jobs(String slug, Map<String, List<String>> appliedFacets, int offset, int limit,
					Runnable onRefusal) {
				WorkdayBoardReaderAdapterTest.this.asked
					.add("%s %s %d %d".formatted(slug, appliedFacets, offset, limit));
				return new Answer(status, json);
			}

			@Override
			public Answer job(String slug, String externalPath, Runnable onRefusal) {
				return new Answer(404, MissingNode.getInstance());
			}

		});
	}

}
