package oneprofile.backend.workers.sweep;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * Turns the HTML of a description into the marked lines a vacancy holds it as, whichever ATS
 * published it.
 */
final class DescriptionMarkingRule {

	private DescriptionMarkingRule() {
	}

	/**
	 * The description as marked lines, one per block of its HTML.
	 * @param html the description's HTML, unescaped
	 * @return its marked lines, or null if it holds no text
	 */
	static String markedLines(String html) {
		if (html == null || html.isBlank()) {
			return null;
		}
		Lines lines = new Lines();
		NodeTraversor.traverse(lines, Jsoup.parse(html).body());
		return lines.written();
	}

	/**
	 * Walks the HTML of a description into lines. A block element starts and ends a line, so the
	 * paragraphs, headings and list items the company wrote stay apart, and so does what a
	 * {@code <br>} breaks. A line inside a list item is marked {@code - }, and a short one that is
	 * a heading is marked {@code # }: inside a heading tag, or all bold, which is how most
	 * descriptions head their sections. Every line of a block Greenhouse adds to each vacancy of a
	 * board — the company's introduction, its pay transparency and its conclusion — is marked
	 * {@code > }: its headings and lists are the company's rather than the role's. Greenhouse marks
	 * those blocks with classes of its own; Ashby's HTML carries no such class, so nothing of it is
	 * marked boilerplate unless its HTML says so the same way. A {@code >} or
	 * {@code #} the company typed at the start of a line is dropped, so that only these marks say
	 * what a line is.
	 */
	private static final class Lines implements NodeVisitor {

		private static final Set<String> BLOCKS = Set.of("address", "article", "aside", "blockquote", "dd", "div",
				"dl", "dt", "figcaption", "figure", "footer", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "li",
				"main", "nav", "ol", "p", "pre", "section", "table", "td", "th", "tr", "ul");

		private static final Set<String> HEADING_TAGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");

		/** Longer than this, a bold line or a heading tag holds prose rather than a heading. */
		private static final int HEADING_LENGTH = 80;

		private static final Set<String> BOLD_TAGS = Set.of("b", "strong");

		private static final Set<String> BOILERPLATE = Set.of("content-intro", "content-pay-transparency",
				"content-conclusion");

		private static final Pattern WHITESPACE = Pattern.compile("[\\s\u00a0]+", Pattern.UNICODE_CHARACTER_CLASS);

		private static final Pattern TYPED_MARK = Pattern.compile("^[>#]+\\s*");

		private final List<String> lines = new ArrayList<>();

		private final StringBuilder line = new StringBuilder();

		/** The element the text of the line sits in, which says what the line is. */
		private Element context;

		/** Whether every word of the line so far is bold. */
		private boolean bold = true;

		@Override
		public void head(Node node, int depth) {
			if (node instanceof TextNode text) {
				this.line.append(text.getWholeText());
				this.context = (Element) text.parent();
				this.bold &= isBlank(text.getWholeText()) || within((at) -> BOLD_TAGS.contains(at.normalName()));
			}
			else if (node instanceof Element element && (BLOCKS.contains(element.normalName()) || element.nameIs("br"))) {
				end();
			}
		}

		@Override
		public void tail(Node node, int depth) {
			if (node instanceof Element element && BLOCKS.contains(element.normalName())) {
				end();
			}
		}

		private void end() {
			String text = TYPED_MARK.matcher(WHITESPACE.matcher(this.line).replaceAll(" ").trim()).replaceFirst("");
			if (!text.isEmpty()) {
				boolean item = within((at) -> at.nameIs("li"));
				String marked = text;
				if (!item && text.length() <= HEADING_LENGTH
						&& (this.bold || within((at) -> HEADING_TAGS.contains(at.normalName())))) {
					marked = "# " + text;
				}
				else if (item) {
					marked = "- " + text;
				}
				boolean boilerplate = within((at) -> at.classNames().stream().anyMatch(BOILERPLATE::contains));
				this.lines.add(boilerplate ? "> " + marked : marked);
			}
			this.line.setLength(0);
			this.context = null;
			this.bold = true;
		}

		/** Whether the text of the line sits in an element that is, or is inside, one of these. */
		private boolean within(Predicate<Element> element) {
			for (Element at = this.context; at != null; at = at.parent()) {
				if (element.test(at)) {
					return true;
				}
			}
			return false;
		}

		private static boolean isBlank(String text) {
			return WHITESPACE.matcher(text).replaceAll("").isEmpty();
		}

		String written() {
			end();
			return this.lines.isEmpty() ? null : String.join("\n", this.lines);
		}

	}

}
