package oneprofile.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import oneprofile.backend.model.Ats;
import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.model.Company;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.service.GreenhouseBoardClient.BoardProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Probes the board of every company known to use Greenhouse and takes down what it
 * answered, so that later only the companies worth asking are asked for openings.
 */
@Service
public class GreenhouseBoardProbeService {

	private static final Logger logger = LoggerFactory.getLogger(GreenhouseBoardProbeService.class);

	/** Roughly five boards a second: thousands of requests against someone else's API. */
	private static final Duration PAUSE_BETWEEN_BOARDS = Duration.ofMillis(200);

	private final GreenhouseBoardClient boardClient;

	private final CompanyRepository companyRepository;

	private final Duration pauseBetweenBoards;

	/** Marked so Spring picks this one: the other is there only for the test. */
	@Autowired
	public GreenhouseBoardProbeService(GreenhouseBoardClient boardClient, CompanyRepository companyRepository) {
		this(boardClient, companyRepository, PAUSE_BETWEEN_BOARDS);
	}

	GreenhouseBoardProbeService(GreenhouseBoardClient boardClient, CompanyRepository companyRepository,
			Duration pauseBetweenBoards) {
		this.boardClient = boardClient;
		this.companyRepository = companyRepository;
		this.pauseBetweenBoards = pauseBetweenBoards;
	}

	/**
	 * Deliberately not transactional: a run takes minutes, and one long transaction
	 * would hold a connection the whole time and lose everything if the run is cut
	 * short. Each save opens its own short transaction instead, so an interrupted run
	 * keeps what it already probed.
	 */
	public ProbeResult probeAll() {
		List<Company> companies = this.companyRepository.findByAts(Ats.GREENHOUSE);
		Map<BoardStatus, Integer> counts = new EnumMap<>(BoardStatus.class);
		int failed = 0;

		boolean first = true;
		for (Company company : companies) {
			if (!first) {
				pause();
			}
			first = false;
			try {
				BoardProbe probe = this.boardClient.probe(company.getSlug());
				company.recordProbe(probe.status(), probe.companyName(), Instant.now());
				this.companyRepository.save(company);
				counts.merge(probe.status(), 1, Integer::sum);
			}
			catch (RuntimeException ex) {
				// One unreachable board must not throw away the whole run: the company
				// keeps its previous state and the next run picks it up again.
				logger.warn("Probing the Greenhouse board of {} failed: {}", company.getSlug(), ex.getMessage());
				failed++;
			}
		}

		return new ProbeResult(companies.size(), counts.getOrDefault(BoardStatus.NOT_FOUND, 0),
				counts.getOrDefault(BoardStatus.EMPTY, 0), counts.getOrDefault(BoardStatus.ACTIVE, 0), failed);
	}

	private void pause() {
		if (this.pauseBetweenBoards.isZero()) {
			return;
		}
		try {
			Thread.sleep(this.pauseBetweenBoards);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while probing Greenhouse boards", ex);
		}
	}

	public record ProbeResult(int companies, int notFound, int empty, int active, int failed) {
	}
}
