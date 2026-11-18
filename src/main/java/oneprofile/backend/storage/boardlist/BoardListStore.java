package oneprofile.backend.storage.boardlist;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.company.AtsEnum;

/** The hand-kept list of boards the sweep does not read, grouped by ATS. */
public class BoardListStore {

	private final Map<AtsEnum, Set<String>> unread;

	/** Holds the validated slugs of each ATS. */
	private BoardListStore(Map<AtsEnum, Set<String>> unread) {
		Map<AtsEnum, Set<String>> copy = new HashMap<>();
		unread.forEach((ats, slugs) -> copy.put(ats, Set.copyOf(slugs)));
		this.unread = Map.copyOf(copy);
	}

	/**
	 * Reads the tab-separated ATS, slug and reason columns, skipping the header. The reader is left open.
	 * @param unread the unread boards file
	 * @return the list it holds
	 * @throws IllegalStateException if a row is malformed or lists an ATS and slug twice
	 */
	public static BoardListStore read(Reader unread) {
		Map<AtsEnum, Set<String>> slugs = new HashMap<>();
		new BufferedReader(unread).lines().skip(1).forEach((line) -> {
			String[] fields = line.split("\t", -1);
			if (fields.length != 3 || fields[1].isBlank() || fields[2].isBlank()) {
				throw new IllegalStateException("An unread board row must have an ATS, a slug and a reason");
			}
			AtsEnum ats;
			try {
				ats = AtsEnum.named(fields[0]);
			}
			catch (IllegalArgumentException ex) {
				throw new IllegalStateException("Unknown ATS '" + fields[0] + "'", ex);
			}
			if (!slugs.computeIfAbsent(ats, (key) -> new HashSet<>()).add(fields[1])) {
				throw new IllegalStateException("The board '" + ats + "/" + fields[1] + "' is listed twice");
			}
		});
		return new BoardListStore(slugs);
	}

	/**
	 * The slugs listed for one ATS.
	 * @param ats the ATS whose unread boards to return
	 * @return its listed slugs, or an empty set if none are listed
	 */
	public Set<String> unreadSlugsOf(AtsEnum ats) {
		return this.unread.getOrDefault(ats, Set.of());
	}

}
