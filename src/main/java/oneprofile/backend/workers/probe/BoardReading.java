package oneprofile.backend.workers.probe;

import oneprofile.backend.storage.company.BoardStatusEnum;

/**
 * What a probe read off one board.
 *
 * @param boardStatus whether the board was found, and whether it had openings
 * @param name the readable name the ATS gave, or null if the probe brought none: a probe brings no
 * name for a board with no openings, or where the ATS publishes none
 */
public record BoardReading(BoardStatusEnum boardStatus, String name) {
}
