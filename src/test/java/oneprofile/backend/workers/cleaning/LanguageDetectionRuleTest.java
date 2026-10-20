package oneprofile.backend.workers.cleaning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LanguageDetectionRuleTest {

	private final LanguageDetectionRule detection = new LanguageDetectionRule();

	@Test
	void detectsEnglish() {
		assertThat(this.detection.detect("""
				# About the role
				We are looking for a backend engineer to join our team and help us build the platform.
				- You have experience with Java and Kubernetes
				- You will work with the product team on the roadmap""")).contains("en");
	}

	@Test
	void detectsSpanish() {
		assertThat(this.detection.detect("""
				# Sobre el puesto
				Buscamos una persona para el equipo de ingeniería que nos ayude a construir la plataforma.
				- Experiencia con Java y Kubernetes
				- Trabajarás con los equipos de producto en las prioridades del año""")).contains("es");
	}

	@Test
	void detectsTheOtherLatinScriptLanguagesBoardsWriteIn() {
		assertThat(this.detection.detect("""
				Wir suchen eine Entwicklerin oder einen Entwickler für unser Team in Berlin. Du arbeitest mit
				dem Produktteam und bist für die Plattform verantwortlich, die wir gemeinsam aufbauen.""")).contains("de");
		assertThat(this.detection.detect("""
				Nous recherchons un ingénieur pour rejoindre notre équipe à Paris. Vous travaillerez avec les
				équipes produit et vous serez responsable de la plateforme que nous construisons.""")).contains("fr");
		assertThat(this.detection.detect("""
				Estamos procurando uma pessoa para o nosso time de engenharia em São Paulo. Você vai trabalhar com
				o time de produto e não precisa ter experiência com a nossa plataforma.""")).contains("pt");
		assertThat(this.detection.detect("""
				Stiamo cercando un ingegnere per il nostro team di Milano. Lavorerai con il team di prodotto e
				sarai responsabile della piattaforma che stiamo costruendo per i nostri clienti.""")).contains("it");
		assertThat(this.detection.detect("""
				Wij zijn op zoek naar een engineer voor ons team in Amsterdam. Je werkt samen met het
				productteam en bent verantwoordelijk voor het platform dat we bouwen voor onze klanten.""")).contains("nl");
	}

	@Test
	void detectsALanguageByItsScript() {
		assertThat(this.detection.detect("私たちはバックエンドエンジニアを募集しています。チームと一緒にプラットフォームを作りましょう。"))
			.contains("ja");
		assertThat(this.detection.detect("우리는 백엔드 엔지니어를 찾고 있습니다. 팀과 함께 플랫폼을 만들어 주세요.")).contains("ko");
		assertThat(this.detection.detect("我们正在寻找一名后端工程师加入我们的团队，一起构建我们的平台。")).contains("zh");
		assertThat(this.detection.detect("אנחנו מחפשים מהנדס או מהנדסת צד שרת שיצטרפו לצוות שלנו ויעזרו לבנות את הפלטפורמה"))
			.contains("he");
		assertThat(this.detection.detect("Мы ищем инженера в нашу команду, который поможет нам построить платформу.")).contains("ru");
	}

	@Test
	void readsTheLanguageOfTheTextRatherThanOfTheTechnologiesItNames() {
		assertThat(this.detection.detect("""
				バックエンドエンジニア（Java / Kotlin / AWS / Kubernetes / PostgreSQL / Terraform / GitHub Actions）
				私たちと一緒にプラットフォームを開発してくれる方を募集しています。""")).contains("ja");
	}

	@Test
	void detectsNothingInATextTooShortToSay() {
		assertThat(this.detection.detect("Java, Kubernetes, AWS")).isEmpty();
		assertThat(this.detection.detect("")).isEmpty();
	}

}
