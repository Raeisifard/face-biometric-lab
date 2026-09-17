package com.isc.facebiometricservice.fullclip;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FullClipScoreAggregatorTest {
    private final FullClipScoreAggregator aggregator = new FullClipScoreAggregator();

    @Test void meanAggregatesCandidateScores() { assertEquals(.70, aggregator.aggregate(List.of(.6, .7, .8), "MEAN"), 1e-9); }
    @Test void medianAggregatesCandidateScores() { assertEquals(.7, aggregator.aggregate(List.of(.9, .7, .5), "MEDIAN"), 1e-9); }
    @Test void maxAggregatesCandidateScores() { assertEquals(.9, aggregator.aggregate(List.of(.9, .7, .5), "MAX"), 1e-9); }
    @Test void emptyScoresAreInconclusiveScore() { assertEquals(0, aggregator.aggregate(List.of(), "MEAN")); }
}