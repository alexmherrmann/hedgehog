import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import com.alexmherrmann.util.hedgehog.queue.Asyncifier;
import com.alexmherrmann.util.hedgehog.queue.QueueImpl;
import kotlinx.coroutines.AwaitKt;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestQueue {
  @Test
  public void testAsync() {
    var queue = new QueueImpl<Integer>();

    var goal = new AtomicInteger(0);
    var async = Asyncifier.asyncifyReceiver(queue);

    var sum = new AtomicInteger(0);
    async.jSubscribe(sum::addAndGet);

    for (int i = 0; i < 50; i++) {
      queue.send(i);
      goal.addAndGet(i);
    }


    Awaitility
      .await()
      .atMost(Duration.ofSeconds(3))
      .until(() -> sum.get() == goal.get());
  }

}
