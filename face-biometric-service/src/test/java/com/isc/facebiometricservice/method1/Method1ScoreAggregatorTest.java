package com.isc.facebiometricservice.method1;
import org.junit.jupiter.api.Test;import java.util.List;import static org.junit.jupiter.api.Assertions.*;
class Method1ScoreAggregatorTest{
 private final Method1ScoreAggregator a=new Method1ScoreAggregator();
 @Test void meanAggregatesCandidateScores(){assertEquals(.70,a.aggregate(List.of(.6,.7,.8),"MEAN"),1e-9);}
 @Test void medianAggregatesCandidateScores(){assertEquals(.7,a.aggregate(List.of(.9,.7,.5),"MEDIAN"),1e-9);}
 @Test void maxAggregatesCandidateScores(){assertEquals(.9,a.aggregate(List.of(.9,.7,.5),"MAX"),1e-9);}
 @Test void emptyScoresAreInconclusiveScore(){assertEquals(0,a.aggregate(List.of(),"MEAN"));}
}
