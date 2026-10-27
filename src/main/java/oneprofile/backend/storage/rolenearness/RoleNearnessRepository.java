package oneprofile.backend.storage.rolenearness;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

/** The nearness of each pair of roles that is held. */
public interface RoleNearnessRepository extends ListCrudRepository<RoleNearnessEntity, RoleNearnessEntity.Key> {

	/**
	 * Deletes every row in one statement, rather than one per row, and lets go of the rows already read,
	 * so the pairs saved after it are not taken for them.
	 * @return how many rows it deleted
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from RoleNearness")
	int deleteEveryRow();

}
