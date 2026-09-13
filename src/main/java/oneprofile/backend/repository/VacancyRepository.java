package oneprofile.backend.repository;

import java.util.List;

import oneprofile.backend.model.Company;
import oneprofile.backend.model.Vacancy;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VacancyRepository extends JpaRepository<Vacancy, Long> {

	/** Everything already stored for a company, to tell a new opening from a known one. */
	List<Vacancy> findByCompany(Company company);

	/** The next page of every vacancy, walked by id so a run never rereads or skips one. */
	List<Vacancy> findByIdGreaterThanOrderById(Long after, Limit limit);

	/**
	 * The next page of the vacancies with no normalized row. Walked by id and not by
	 * offset: the set shrinks while it is walked, and an offset would skip rows.
	 */
	@Query("""
			select v from Vacancy v
			where v.id > :after
			and not exists (select n from NormalizedVacancy n where n.vacancy = v)
			order by v.id""")
	List<Vacancy> findNotNormalizedByIdGreaterThan(Long after, Limit limit);
}
