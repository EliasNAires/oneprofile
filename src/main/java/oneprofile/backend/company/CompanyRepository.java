package oneprofile.backend.company;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

/** The companies that are held. */
public interface CompanyRepository extends ListCrudRepository<Company, Long> {

	/**
	 * The slugs held for one ATS.
	 * @param ats the ATS to read
	 * @return every slug held for it
	 */
	@Query("select c.slug from Company c where c.ats = :ats")
	Set<String> slugsOf(Ats ats);

	/**
	 * The company a slug names within one ATS.
	 * @param ats the ATS the slug belongs to
	 * @param slug the slug to read
	 * @return the company, or empty if none is held under that slug
	 */
	Optional<Company> findByAtsAndSlug(Ats ats, String slug);

}
