package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
import org.junit.jupiter.api.Test;

/** The Wayback Machine is a miniature: one single-chunk answer per prefix asked. */
class WaybackDiscoveryRunTest {

	private static final Map<String, String> ARCHIVE = Map.of("boards.greenhouse.io/", """
			[["original"],
			["https://boards.greenhouse.io/acme/jobs/1"],
			["https://boards.greenhouse.io/Acme"],
			["http://boards.greenhouse.io/oldslug"]]
			""", "job-boards.greenhouse.io/", """
			[["original"],
			["https://job-boards.greenhouse.io/embed/job_app?for=eikontherapeutics&token=1"]]
			""", "boards.eu.greenhouse.io/", "[]", "job-boards.eu.greenhouse.io/", """
			[["original"],
			["https://job-boards.eu.greenhouse.io/decathlontechnology/jobs/4917685101"]]
			""", "jobs.ashbyhq.com/", """
			[["original"],
			["https://jobs.ashbyhq.com/linear/2b5c8a1e"],
			["https://jobs.ashbyhq.com/Ramp/application"]]
			""");

	private final List<String> prefixesAsked = new ArrayList<>();

	private final CompanyStore companies = mock(CompanyStore.class);

	@Test
	void recordsTheSlugOfEveryGreenhouseBoardCapturedOnAnyOfItsHosts() throws IOException {
		given(this.companies.record(eq(AtsEnum.GREENHOUSE),
				eq(new TreeSet<>(List.of("acme", "decathlontechnology", "eikontherapeutics")))))
			.willReturn(2);

		assertThat(runOver(this::chunkOf).discover(AtsEnum.GREENHOUSE))
			.isEqualTo(new WaybackDiscoveryRun.Report(AtsEnum.GREENHOUSE, 5, 3, 2));
		assertThat(this.prefixesAsked).containsExactly("boards.greenhouse.io/", "job-boards.greenhouse.io/",
				"boards.eu.greenhouse.io/", "job-boards.eu.greenhouse.io/");
	}

	@Test
	void recordsTheSlugOfEveryAshbyBoardCaptured() throws IOException {
		given(this.companies.record(eq(AtsEnum.ASHBY), eq(new TreeSet<>(List.of("linear", "ramp"))))).willReturn(1);

		assertThat(runOver(this::chunkOf).discover(AtsEnum.ASHBY))
			.isEqualTo(new WaybackDiscoveryRun.Report(AtsEnum.ASHBY, 2, 2, 1));
		assertThat(this.prefixesAsked).containsExactly("jobs.ashbyhq.com/");
	}

	@Test
	void recordsNothingUnlessEveryPrefixWasReadWhole() {
		WaybackDiscoveryRun discovery = runOver((urlPrefix, resumeKey) -> {
			if (urlPrefix.equals("job-boards.eu.greenhouse.io/")) {
				throw new IOException("Gave up after 10 attempts: answered 503");
			}
			return chunkOf(urlPrefix, resumeKey);
		});

		assertThatIOException().isThrownBy(() -> discovery.discover(AtsEnum.GREENHOUSE));
		then(this.companies).shouldHaveNoInteractions();
	}

	private String chunkOf(String urlPrefix, Optional<String> resumeKey) {
		this.prefixesAsked.add(urlPrefix);
		return ARCHIVE.get(urlPrefix);
	}

	private WaybackDiscoveryRun runOver(WaybackCdxPort cdx) {
		return new WaybackDiscoveryRun(new WaybackCdxParser(cdx), this.companies);
	}

}
