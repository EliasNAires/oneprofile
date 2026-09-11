package oneprofile.backend.repository;

import java.util.List;
import java.util.Optional;

import oneprofile.backend.model.Ats;
import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompanyRepository extends JpaRepository<Company, Long> {

	@Query("select c.slug from Company c where c.ats = :ats")
	List<String> findSlugsByAts(Ats ats);

	/** Whole entities, unlike {@link #findSlugsByAts}: the probe updates them. */
	List<Company> findByAts(Ats ats);

	Optional<Company> findByAtsAndSlug(Ats ats, String slug);

	/** Slugs again: the vacancy sweep asks for one board at a time and needs nothing else. */
	@Query("select c.slug from Company c where c.ats = :ats and c.boardStatus = :status")
	List<String> findSlugsByAtsAndBoardStatus(Ats ats, BoardStatus status);
}
