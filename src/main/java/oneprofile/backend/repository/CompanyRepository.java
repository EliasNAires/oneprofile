package oneprofile.backend.repository;

import java.util.List;

import oneprofile.backend.model.Ats;
import oneprofile.backend.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompanyRepository extends JpaRepository<Company, Long> {

	@Query("select c.slug from Company c where c.ats = :ats")
	List<String> findSlugsByAts(Ats ats);
}
