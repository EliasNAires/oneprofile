package oneprofile.backend.repository;

import java.util.List;

import oneprofile.backend.model.Ats;
import oneprofile.backend.model.BlacklistedSlug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BlacklistedSlugRepository extends JpaRepository<BlacklistedSlug, Long> {

	@Query("select b.slug from BlacklistedSlug b where b.ats = :ats")
	List<String> findSlugsByAts(Ats ats);

}

