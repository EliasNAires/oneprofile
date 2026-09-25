package oneprofile.backend.workers.probe;

import oneprofile.backend.storage.company.BoardStatusEnum;

/**
 * What a probe read off one board.
 *
 * @param boardStatus whether the board was found, and whether it had openings
 * @param name the readable name the board gave, or null if it gave none — a board with no openings
 * names nobody
 */
public record BoardReading(BoardStatusEnum boardStatus, String name) {
}
