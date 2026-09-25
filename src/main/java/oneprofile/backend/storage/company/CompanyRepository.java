package oneprofile.backend.storage.company;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

/** The companies that are held. */
public interface CompanyRepository extends ListCrudRepository<CompanyEntity, Long> {

	/**
	 * The slugs held for one ATS.
	 * @param ats the ATS to read
	 * @return every slug held for it
	 */
	@Query("select c.slug from Company c where c.ats = :ats")
	Set<String> slugsOf(AtsEnum ats);

	/**
	 * The slugs held for one ATS whose board was last found to be of one kind.
	 * @param ats the ATS to read
	 * @param boardStatus what their boards were last found to be
	 * @return every slug held for it with that board status
	 */
	@Query("select c.slug from Company c where c.ats = :ats and c.boardStatus = :boardStatus")
	Set<String> slugsOf(AtsEnum ats, BoardStatusEnum boardStatus);

	/**
	 * The company a slug names within one ATS.
	 * @param ats the ATS the slug belongs to
	 * @param slug the slug to read
	 * @return the company, or empty if none is held under that slug
	 */
	Optional<CompanyEntity> findByAtsAndSlug(AtsEnum ats, String slug);

}
