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
    private final CountDownLatch connected;

    public SimpleWatcher(final int clients) {
      this.connected = new CountDownLatch(clients);
    }

    synchronized public void process(WatchedEvent event) {
      System.out.println("Event received: " + event);
      if (event.getState() == KeeperState.SyncConnected ||
	  event.getState() == KeeperState.ConnectedReadOnly) {
        connected.countDown();
      }
      notifyAll();
    }
}

