package oneprofile.backend.workers.role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import oneprofile.backend.storage.rolenearness.RoleNearness;
import oneprofile.backend.storage.vacancyskill.SkillProfile;
import org.springframework.stereotype.Component;

/**
 * How near each pair of roles is, from the skills their vacancies ask for (ADR-0024; #25, section 5). A
 * role's vector holds, for each skill, the share of its vacancies that name it, weighted by how few
 * vacancies of the engineering subset name it, the logarithm of the subset over those, so Python and SQL
 * count less and a skill every vacancy names counts nothing. Two roles' nearness is the cosine of their
 * vectors. It never moves a title into a role.
 */
@Component
public class RoleNearnessRule {

	/**
	 * The nearness of every pair of roles. A role whose vacancies name no skill with a weight has no
	 * direction to be near anything in, and is left out.
	 * @param subset the skills the engineering subset asks for
	 * @param roles the skills each role's vacancies ask for, by slug
	 * @return each pair once, its roles in order of slug
	 */
	public List<RoleNearness> nearness(SkillProfile subset, Map<String, SkillProfile> roles) {
		Map<String, Map<String, Double>> vectors = new TreeMap<>();
		roles.forEach((role, profile) -> {
			Map<String, Double> vector = vector(subset, profile);
			if (!vector.isEmpty()) {
				vectors.put(role, vector);
			}
		});
		List<String> slugs = List.copyOf(vectors.keySet());
		List<RoleNearness> pairs = new ArrayList<>();
		for (int i = 0; i < slugs.size(); i++) {
			for (int j = i + 1; j < slugs.size(); j++) {
				pairs.add(new RoleNearness(slugs.get(i), slugs.get(j),
						dot(vectors.get(slugs.get(i)), vectors.get(slugs.get(j)))));
			}
		}
		return pairs;
	}

	/** A role's weighted shares, of length one; empty where no skill it names has a weight. */
	private static Map<String, Double> vector(SkillProfile subset, SkillProfile role) {
		Map<String, Double> vector = new HashMap<>();
		role.vacanciesNaming().forEach((skill, naming) -> {
			double weight = (double) naming / role.vacancies()
					* Math.log((double) subset.vacancies() / subset.vacanciesNaming().get(skill));
			if (weight > 0) {
				vector.put(skill, weight);
			}
		});
		double norm = Math.sqrt(vector.values().stream().mapToDouble((weight) -> weight * weight).sum());
		vector.replaceAll((skill, weight) -> weight / norm);
		return vector;
	}

	private static double dot(Map<String, Double> one, Map<String, Double> other) {
		return one.entrySet().stream().mapToDouble((entry) -> entry.getValue() * other.getOrDefault(entry.getKey(), 0.0))
			.sum();
	}

}
