package oneprofile.backend.workers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ThrottleTest {

	@Test
	void halvesBothLimitsOnEachRefusalDownToOne() {
		Throttle throttle = new Throttle("The test", 20, 16);

		throttle.refused();
		assertThat(throttle.requestsAtOnce()).isEqualTo(10);
		assertThat(throttle.startsASecond()).isEqualTo(8);

		for (int i = 0; i < 5; i++) {
			throttle.refused();
		}
		assertThat(throttle.requestsAtOnce()).isEqualTo(1);
		assertThat(throttle.startsASecond()).isEqualTo(1);
	}

	@Test
	void halvesOnceForABurstOfRefusalsAndHoldsLongerWhileItStillRefuses() throws InterruptedException {
		Throttle throttle = new Throttle("The test", 20, 100);
		Duration hold = Duration.ofMillis(100);
		long first = throttle.awaitStart();
		long second = throttle.awaitStart();

		assertThat(throttle.refused(first, hold, Duration.ofMillis(150))).isTrue();
		assertThat(throttle.refused(second, hold, Duration.ofMillis(150))).isFalse();
		assertThat(throttle.requestsAtOnce()).isEqualTo(10);

		throttle.finished();
		throttle.finished();
		long held = System.nanoTime();
		long afterTheHold = throttle.awaitStart();
		assertThat(Duration.ofNanos(afterTheHold - held)).isGreaterThan(Duration.ofMillis(80));
		assertThat(throttle.refused(afterTheHold, hold, Duration.ofMillis(150))).isFalse();
		assertThat(throttle.requestsAtOnce()).isEqualTo(10);

		throttle.finished();
		held = System.nanoTime();
		long afterTheLongerHold = throttle.awaitStart();
		assertThat(Duration.ofNanos(afterTheLongerHold - held)).isGreaterThan(Duration.ofMillis(130));
		throttle.answered(afterTheLongerHold);
		throttle.finished();
		assertThat(throttle.refused(throttle.awaitStart(), hold, Duration.ofMillis(150))).isTrue();
		assertThat(throttle.requestsAtOnce()).isEqualTo(5);
	}

	@Test
	void keepsItsHalvingWhileARequestIsOpenAndTakesItsFullPaceBackOnceIdle() throws InterruptedException {
		Throttle throttle = new Throttle("The test", 20, 16, Duration.ofMillis(100));

		throttle.awaitStart();
		throttle.refused();
		Thread.sleep(150);
		assertThat(throttle.requestsAtOnce()).isEqualTo(10);

		throttle.finished();
		assertThat(throttle.requestsAtOnce()).isEqualTo(10);
		Thread.sleep(150);
		assertThat(throttle.requestsAtOnce()).isEqualTo(20);
		assertThat(throttle.startsASecond()).isEqualTo(16);
	}

	@Test
	void keepsItsHalvingForAsLongAsItIsHeldUnlessToldToRestore() throws InterruptedException {
		Throttle throttle = new Throttle("The test", 20, 16);

		throttle.refused();
		Thread.sleep(50);

		assertThat(throttle.requestsAtOnce()).isEqualTo(10);
	}

}
