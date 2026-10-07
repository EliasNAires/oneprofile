package oneprofile.backend.workers.stoplist;

import java.util.Set;

/**
 * The closed-class words of English and Spanish: articles, pronouns, determiners, prepositions,
 * conjunctions and auxiliaries, which no new word ever joins, so they are listed by hand once rather
 * than judged. A word a skill could be is left out even when it is a function word: {@code can}
 * (CAN bus), {@code es} (ES as ECMAScript or Elasticsearch), {@code ha} (HA), {@code os} (OS) and
 * {@code se} (SE). Other languages get
 * no list.
 */
final class ClosedClassRule {

	private static final Set<String> ENGLISH = Set.of(
			// Articles
			"a", "an", "the",
			// Pronouns
			"i", "me", "my", "mine", "myself", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his",
			"himself", "she", "her", "hers", "herself", "it", "its", "itself", "we", "us", "our", "ours", "ourselves",
			"they", "them", "their", "theirs", "themselves", "this", "that", "these", "those", "who", "whom", "whose",
			"which", "what", "whoever", "whatever", "whichever", "there", "someone", "anyone", "everyone", "somebody",
			"anybody", "everybody", "nobody", "something", "anything", "everything", "nothing",
			// Determiners
			"all", "another", "any", "both", "each", "either", "every", "few", "many", "more", "most", "much",
			"neither", "no", "none", "other", "others", "several", "some", "such",
			// Prepositions
			"about", "above", "across", "after", "against", "along", "amid", "among", "around", "at", "before",
			"behind", "below", "beneath", "beside", "besides", "between", "beyond", "by", "despite", "down", "during",
			"except", "for", "from", "in", "inside", "into", "like", "near", "of", "off", "on", "onto", "out",
			"outside", "over", "past", "per", "since", "through", "throughout", "till", "to", "toward", "towards",
			"under", "underneath", "until", "up", "upon", "via", "with", "within", "without",
			// Conjunctions
			"and", "or", "but", "nor", "so", "yet", "if", "unless", "because", "although", "though", "while",
			"whereas", "whether", "as", "than", "when", "where", "why", "how", "whenever", "wherever", "not",
			// Auxiliaries
			"be", "am", "is", "are", "was", "were", "been", "being", "have", "has", "had", "having", "do", "does",
			"did", "doing", "will", "would", "shall", "should", "could", "may", "might", "must", "ought",
			// Contractions of the above
			"i'm", "i've", "i'll", "i'd", "you're", "you've", "you'll", "you'd", "he's", "she's", "it's", "we're",
			"we've", "we'll", "we'd", "they're", "they've", "they'll", "they'd", "that's", "there's", "what's",
			"who's", "let's", "isn't", "aren't", "wasn't", "weren't", "don't", "doesn't", "didn't", "haven't",
			"hasn't", "hadn't", "won't", "wouldn't", "shouldn't", "couldn't", "mustn't", "can't");

	private static final Set<String> SPANISH = Set.of(
			// Articles, and the prepositions contracted with one
			"el", "la", "lo", "los", "las", "un", "una", "unos", "unas", "al", "del",
			// Pronouns
			"yo", "tú", "tu", "usted", "ustedes", "él", "ella", "ello", "ellos", "ellas", "nosotros", "nosotras",
			"vosotros", "vosotras", "me", "te", "nos", "le", "les", "mi", "mis", "tus", "su", "sus", "nuestro",
			"nuestra", "nuestros", "nuestras", "vuestro", "vuestra", "vuestros", "vuestras", "este", "esta", "estos",
			"estas", "esto", "ese", "esa", "esos", "esas", "eso", "aquel", "aquella", "aquellos", "aquellas",
			"aquello", "quien", "quienes", "cual", "cuales", "cuyo", "cuya", "cuyos", "cuyas", "qué", "quién",
			"cuál", "cómo", "dónde", "cuándo",
			// Determiners
			"todo", "toda", "todos", "todas", "cada", "algún", "alguno", "alguna", "algunos", "algunas", "ningún",
			"ninguno", "ninguna", "otro", "otra", "otros", "otras", "mucho", "mucha", "muchos", "muchas", "varios",
			"varias",
			// Prepositions
			"a", "ante", "bajo", "con", "contra", "de", "desde", "durante", "en", "entre", "hacia", "hasta",
			"mediante", "para", "por", "según", "sin", "sobre", "tras",
			// Conjunctions
			"y", "e", "o", "u", "ni", "pero", "sino", "que", "si", "porque", "aunque", "como", "cuando", "donde",
			"mientras", "pues", "no",
			// Auxiliaries: ser, estar, haber, and the modals poder and deber
			"ser", "soy", "eres", "somos", "son", "era", "eran", "fue", "fueron", "será", "serán", "sería",
			"serían", "sea", "sean", "sido", "siendo", "estar", "estoy", "estás", "está", "estamos", "están",
			"estaba", "estaban", "estado", "estando", "estará", "estarán", "esté", "estén", "haber", "he", "has",
			"hemos", "han", "había", "habían", "hay", "habrá", "habrán", "haya", "hayan", "habido",
			"habiendo", "poder", "puede", "pueden", "podrá", "podrán", "podría", "podrían", "deber", "debe",
			"deben", "deberá", "deberán", "debería", "deberían");

	private ClosedClassRule() {
	}

	/**
	 * Whether a word is a closed-class word of English or Spanish.
	 * @param word a word lowercased, with its apostrophes either straight or curly
	 * @return whether it is on either list
	 */
	static boolean closedClass(String word) {
		String straight = word.replace('’', '\'');
		return ENGLISH.contains(straight) || SPANISH.contains(straight);
	}

}
