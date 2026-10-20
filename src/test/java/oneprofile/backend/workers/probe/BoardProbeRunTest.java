package oneprofile.backend.workers.probe;

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
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.CompanyStore;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

class BoardProbeRunTest {

	private static final Instant PROBED_AT = Instant.parse("2026-09-19T10:15:30Z");

	private final CompanyStore companies = mock(CompanyStore.class);

	@Test
	void recordsAgainstEveryCompanyWhatItsBoardIs() {
		heldSlugs("stripe", "notion", "gone");
		BoardProbeRun probe = probeOf(Map.of("stripe", new BoardReading(BoardStatusEnum.ACTIVE, "Stripe"), "notion",
				new BoardReading(BoardStatusEnum.EMPTY, null), "gone", new BoardReading(BoardStatusEnum.NOT_FOUND, null)));

		assertThat(probe.probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 1, 1, 0));

		then(this.companies).should()
			.recordProbe(AtsEnum.GREENHOUSE, "stripe", BoardStatusEnum.ACTIVE, "Stripe", PROBED_AT);
		then(this.companies).should().recordProbe(AtsEnum.GREENHOUSE, "notion", BoardStatusEnum.EMPTY, null, PROBED_AT);
		then(this.companies).should().recordProbe(AtsEnum.GREENHOUSE, "gone", BoardStatusEnum.NOT_FOUND, null, PROBED_AT);
	}

	@Test
	void leavesUnprobedTheCompaniesWhoseBoardItCouldNotRead() {
		heldSlugs("stripe", "unreachable");
		Map<String, BoardReading> boards = new LinkedHashMap<>();
		boards.put("stripe", new BoardReading(BoardStatusEnum.ACTIVE, "Stripe"));
		boards.put("unreachable", null);

		assertThat(probeOf(boards).probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 0, 0, 1));

		then(this.companies).should().recordProbe(AtsEnum.GREENHOUSE, "stripe", BoardStatusEnum.ACTIVE, "Stripe", PROBED_AT);
		then(this.companies).should(BDDMockito.never())
			.recordProbe(AtsEnum.GREENHOUSE, "unreachable", null, null, PROBED_AT);
	}

	@Test
	void waitsBetweenRequestsSoThatABoardIsNotAskedForTooOften() {
		heldSlugs("stripe", "notion", "figma");
		BoardProbeRun probe = new BoardProbeRun(Map.of(AtsEnum.GREENHOUSE, reader(Map.of("stripe",
				new BoardReading(BoardStatusEnum.EMPTY, null), "notion", new BoardReading(BoardStatusEnum.EMPTY, null), "figma",
				new BoardReading(BoardStatusEnum.EMPTY, null)))), Map.of(AtsEnum.GREENHOUSE, Duration.ofMillis(50)),
				this.companies, clock());

		Instant before = Instant.now();
		probe.probeAll(AtsEnum.GREENHOUSE);

		assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(Duration.ofMillis(100));
	}

	@Test
	void probesTheBoardsOfTheAtsItIsAskedForWithThatAtssReader() {
		BDDMockito.given(this.companies.slugsOf(AtsEnum.ASHBY)).willReturn(new TreeSet<>(List.of("ramp")));
		BoardProbeRun probe = new BoardProbeRun(
				Map.of(AtsEnum.GREENHOUSE, reader(Map.of()), AtsEnum.ASHBY,
						reader(Map.of("ramp", new BoardReading(BoardStatusEnum.ACTIVE, null)))),
				Map.of(AtsEnum.GREENHOUSE, Duration.ZERO, AtsEnum.ASHBY, Duration.ZERO), this.companies, clock());

		assertThat(probe.probeAll(AtsEnum.ASHBY)).isEqualTo(new BoardProbeRun.Report(1, 0, 0, 0));
		then(this.companies).should().recordProbe(AtsEnum.ASHBY, "ramp", BoardStatusEnum.ACTIVE, null, PROBED_AT);
	}

	private void heldSlugs(String... slugs) {
		BDDMockito.given(this.companies.slugsOf(AtsEnum.GREENHOUSE)).willReturn(new TreeSet<>(List.of(slugs)));
	}

	private BoardProbeRun probeOf(Map<String, BoardReading> boards) {
		return new BoardProbeRun(Map.of(AtsEnum.GREENHOUSE, reader(boards)), Map.of(AtsEnum.GREENHOUSE, Duration.ZERO),
				this.companies, clock());
	}

	/** Answers what the map holds, and fails to read the slugs it maps to null. */
	private BoardReaderPort reader(Map<String, BoardReading> boards) {
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
