package oneprofile.backend.workers.stoplist;

/**
 * Jev's answer on whether one word could name a technology.
 *
 * @param choice {@code yes} or {@code no}
 * @param no the probability that it could name none
 * @param yes the probability that it could name one
 * @param model the model that answered
 * @param inputTokens what the question cost, as Jev reported it
 */
public record JevAnswer(String choice, double no, double yes, String model, long inputTokens) {
}
