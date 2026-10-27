package oneprofile.backend.storage.rolenearness;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Transient;
import java.io.Serializable;
import org.springframework.data.domain.Persistable;

/**
 * How near one pair of roles is. A run clears the table before writing, so a row is always new: saying
 * so spares a read of each row before it is inserted.
 */
@Entity(name = "RoleNearness")
@IdClass(RoleNearnessEntity.Key.class)
public class RoleNearnessEntity implements Persistable<RoleNearnessEntity.Key> {

	@Id
	private String role;

	@Id
	private String nearRole;

	private double nearness;

	protected RoleNearnessEntity() {
	}

	/** @param nearness the pair and how near it is */
	public RoleNearnessEntity(RoleNearness nearness) {
		this.role = nearness.role();
		this.nearRole = nearness.nearRole();
		this.nearness = nearness.nearness();
	}

	/** The pair and how near it is. */
	public RoleNearness nearness() {
		return new RoleNearness(this.role, this.nearRole, this.nearness);
	}

	@Override
	public Key getId() {
		return new Key(this.role, this.nearRole);
	}

	@Override
	@Transient
	public boolean isNew() {
		return true;
	}

	/**
	 * A row's identity: the pair of roles.
	 *
	 * @param role the slug of the pair's first role
	 * @param nearRole the slug of its second
	 */
	public record Key(String role, String nearRole) implements Serializable {
	}

}
