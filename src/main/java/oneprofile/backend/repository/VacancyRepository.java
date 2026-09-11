package oneprofile.backend.repository;

import java.util.List;

import oneprofile.backend.model.Company;
import oneprofile.backend.model.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VacancyRepository extends JpaRepository<Vacancy, Long> {

	/** Everything already stored for a company, to tell a new opening from a known one. */
	List<Vacancy> findByCompany(Company company);
}
