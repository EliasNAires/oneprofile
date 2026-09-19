package oneprofile.backend.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import oneprofile.backend.company.Ats;
import oneprofile.backend.company.BoardStatus;
import oneprofile.backend.company.Companies;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

class BoardProbeTest {

	private static final Instant PROBED_AT = Instant.parse("2026-09-19T10:15:30Z");

	private final Companies companies = mock(Companies.class);

	@Test
	void recordsAgainstEveryCompanyWhatItsBoardIs() {
		heldSlugs("stripe", "notion", "gone");
		BoardProbe probe = probeOf(Map.of("stripe", new BoardReading(BoardStatus.ACTIVE, "Stripe"), "notion",
				new BoardReading(BoardStatus.EMPTY, null), "gone", new BoardReading(BoardStatus.NOT_FOUND, null)));

		assertThat(probe.probeAll()).isEqualTo(new BoardProbe.Report(1, 1, 1, 0));

		then(this.companies).should()
			.recordProbe(Ats.GREENHOUSE, "stripe", BoardStatus.ACTIVE, "Stripe", PROBED_AT);
		then(this.companies).should().recordProbe(Ats.GREENHOUSE, "notion", BoardStatus.EMPTY, null, PROBED_AT);
		then(this.companies).should().recordProbe(Ats.GREENHOUSE, "gone", BoardStatus.NOT_FOUND, null, PROBED_AT);
	}

	@Test
	void leavesUnprobedTheCompaniesWhoseBoardItCouldNotRead() {
		heldSlugs("stripe", "unreachable");
		Map<String, BoardReading> boards = new LinkedHashMap<>();
		boards.put("stripe", new BoardReading(BoardStatus.ACTIVE, "Stripe"));
		boards.put("unreachable", null);

		assertThat(probeOf(boards).probeAll()).isEqualTo(new BoardProbe.Report(1, 0, 0, 1));

		then(this.companies).should().recordProbe(Ats.GREENHOUSE, "stripe", BoardStatus.ACTIVE, "Stripe", PROBED_AT);
		then(this.companies).should(BDDMockito.never())
			.recordProbe(Ats.GREENHOUSE, "unreachable", null, null, PROBED_AT);
	}

	@Test
	void waitsBetweenRequestsSoThatABoardIsNotAskedForTooOften() {
		heldSlugs("stripe", "notion", "figma");
		BoardProbe probe = new BoardProbe(Ats.GREENHOUSE, reader(Map.of("stripe",
				new BoardReading(BoardStatus.EMPTY, null), "notion", new BoardReading(BoardStatus.EMPTY, null), "figma",
				new BoardReading(BoardStatus.EMPTY, null))), this.companies, clock(), Duration.ofMillis(50));

		Instant before = Instant.now();
		probe.probeAll();

		assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(Duration.ofMillis(100));
	}

	private void heldSlugs(String... slugs) {
		BDDMockito.given(this.companies.slugsOf(Ats.GREENHOUSE)).willReturn(new TreeSet<>(List.of(slugs)));
	}

	private BoardProbe probeOf(Map<String, BoardReading> boards) {
		return new BoardProbe(Ats.GREENHOUSE, reader(boards), this.companies, clock(), Duration.ZERO);
	}

	/** Answers what the map holds, and fails to read the slugs it maps to null. */
	private BoardReader reader(Map<String, BoardReading> boards) {
		return (slug) -> {
			BoardReading reading = boards.get(slug);
			if (reading == null) {
				throw new IOException("boards-api.greenhouse.io answered 503");
			}
			return reading;
		};
	}

	private Clock clock() {
		return Clock.fixed(PROBED_AT, ZoneOffset.UTC);
	}

}
