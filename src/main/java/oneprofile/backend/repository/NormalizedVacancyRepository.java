package oneprofile.backend.repository;

import java.util.Collection;
import java.util.List;

import oneprofile.backend.model.NormalizedVacancy;
import oneprofile.backend.model.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NormalizedVacancyRepository extends JpaRepository<NormalizedVacancy, Long> {

	/** What a page of vacancies already has normalized, to update it instead of adding a second row. */
	List<NormalizedVacancy> findByVacancyIn(Collection<Vacancy> vacancies);
}
