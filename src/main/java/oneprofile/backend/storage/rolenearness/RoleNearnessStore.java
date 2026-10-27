package oneprofile.backend.storage.rolenearness;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps how near each pair of roles is (ADR-0024): what the Finder's roles near the ones picked read, and
 * what matching will. After a run the table holds what that run computed and nothing else, so a role that
 * has left the list or lost its vacancies leaves no pair behind.
 */
@Component
public class RoleNearnessStore {

	private final RoleNearnessRepository repository;

	public RoleNearnessStore(RoleNearnessRepository repository) {
		this.repository = repository;
	}

	/**
	 * Replaces every pair held with those of one run, in one transaction, so nothing reads a half-written
	 * set.
	 * @param pairs each pair of roles once, its roles in order of slug
	 * @return how many pairs this recorded
	 */
	@Transactional
	public int replace(List<RoleNearness> pairs) {
		this.repository.deleteEveryRow();
		this.repository.saveAll(pairs.stream().map(RoleNearnessEntity::new).toList());
		return pairs.size();
	}

}
