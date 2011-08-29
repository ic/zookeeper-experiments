/*
 * For the moment stolen and stripped down from org.apache.zookeeper.test.WatcherFuncTest
 */
package jp.ac.mcg;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.Watcher.Event.EventType;

class SimpleWatcher implements Watcher {
    private LinkedBlockingQueue<WatchedEvent> events = new LinkedBlockingQueue<WatchedEvent>();
    private CountDownLatch latch;

    public SimpleWatcher(CountDownLatch latch) {
      this.latch = latch;
    }

    public CountDownLatch getLatch() {
      return this.latch;
    }

    public void process(WatchedEvent event) {
      if (event.getState() == KeeperState.SyncConnected) {
        if (latch != null) {
          latch.countDown();
        }
      }

      if (event.getType() == EventType.None) {
        return;
      }
      try {
        events.put(event);
      } catch (InterruptedException e) {
        // @todo
      }
    }

    public void verify(List<EventType> expected) throws InterruptedException{
      WatchedEvent event;
      int count = 0;
      while (count < expected.size() && (event = events.poll(30, TimeUnit.SECONDS)) != null)
      {
        count++;
      }
      events.clear();
    }
}

