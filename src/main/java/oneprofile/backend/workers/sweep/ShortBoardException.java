package oneprofile.backend.workers.sweep;

import java.io.IOException;

/** A board's distinct listed vacancies fell short of its known size, so it must not be mirrored. */
public class ShortBoardException extends IOException {

	private final int read;

	private final int size;

	/** Describes how many distinct vacancies were read and how many the board was expected to hold. */
	public ShortBoardException(int read, int size) {
		super("Read %d vacancies from a board of size %d".formatted(read, size));
		this.read = read;
		this.size = size;
	}

	/** How many distinct vacancies the list named. */
	public int read() {
		return this.read;
	}

	/** How many vacancies the board was expected to hold. */
	public int size() {
		return this.size;
	}

}
