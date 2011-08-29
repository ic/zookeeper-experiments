/*
 * Telex.
 *
 * This experiment is a simplistic message passing program
 * based on a distributed queue. This first version just
 * creates a talking publisher and two listening consumers.
 * The publisher sends the alphabet, and consumers display
 * the letters they receive on the standard output.
 *
 */
package jp.ac.mcg;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.KeeperException;

import org.apache.zookeeper.recipes.queue.DistributedQueue;

import jp.ac.mcg.SimpleWatcher;

public class Telex {

  final String host = "127.0.0.1";
  final int TIMEOUT = 30000;

  ZooKeeper[] publishers, consumers;
  String      directory  = "/telex";
  String      vocabulary = "abcdefghijklmnopqrstuvwxyz";
  Watcher     watcher;
  DistributedQueue[] handles;
  int       nextPort = 31457;

  public static void main(final String[] args) {
    int numConsumers    = 2;
    int numPublishers = 1;
    if(args.length > 0 && args[0] != null) {
      numConsumers = new Integer(args[0]);
    }
    if(args.length > 1 && args[1] != null) {
      numPublishers = new Integer(args[1]);
    }
    new Telex(numConsumers, numPublishers).run();
  }

  Telex(final int numConsumers, final int numPublishers) {
    try {
      publishers = new ZooKeeper[numPublishers];
      consumers  = new ZooKeeper[numConsumers];
      handles    = new DistributedQueue[numConsumers + numPublishers];
      watcher    = new SimpleWatcher(new CountDownLatch(numConsumers));
      for(int i = 0; i < numPublishers; i++) {
	publishers[i] = new ZooKeeper(host + ":" + assignPort(), TIMEOUT, watcher);
	handles[i] = new DistributedQueue(publishers[i], directory, null);
      }
      for(int i = 0; i < numConsumers; i++) {
	consumers[i] = new ZooKeeper(host + ":" + assignPort(), TIMEOUT, watcher);
	handles[i + numPublishers] = new DistributedQueue(consumers[i], directory, null);
      }
    } catch(IOException e) {
      System.out.println("Initialization error: " + e.getMessage());
      System.exit(1);
    }
  }

  public void run() {
    int numThreads = consumers.length + publishers.length;
    Thread[] threads = new Thread[numThreads];

    for(int i = 0; i < consumers.length; i++) {
      final int id = i;
      threads[i] = new Thread() {
        public void run() {
	  try {
            final String msg = new String(handles[id].take());
	    System.out.println("              " + msg + " > " + "Consumer " + id);
          } catch(KeeperException e) {
	    System.out.println("Keeper error: " + e.getMessage());
	    System.exit(2);
	  } catch(InterruptedException e) {
	    System.out.println("Interrupted: " + e.getMessage());
	    System.exit(3);
	  }
	}
      };
    }

    for(int i = 0; i < publishers.length; i++) {
      final int id = i;
      threads[i + consumers.length] = new Thread() {
        public void run() {
	  try {
	    for(int idx = 0; idx < vocabulary.length() - 1; idx++) {
              final String msg = vocabulary.substring(idx, idx + 1);
	      handles[id].offer(msg.getBytes());
	      System.out.println("Publisher " + id + " > " + msg);
	    }
          } catch(KeeperException e){
	    System.out.println("Keeper error: " + e.getMessage());
	    System.exit(4);
	  } catch(InterruptedException e){
	    System.out.println("Interrupted: " + e.getMessage());
	    System.exit(5);
	  }
	}
      };
    }

    for(int i = 0; i < (consumers.length + publishers.length); i++) {
      try {
        threads[i].start();
        threads[i].join();
      } catch(InterruptedException e){
	System.out.println("Interrupted: " + e.getMessage());
	System.exit(6);
      }
    }

    System.out.println("Completed.");
  }


  private int assignPort() {
    return (nextPort += 1);
  }

}

