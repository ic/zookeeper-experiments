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
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.KeeperException;

import org.apache.zookeeper.recipes.queue.DistributedQueue;

import jp.ac.mcg.SimpleWatcher;

public class Telex {

  final String host = "127.0.0.1";
        int    port = 2181;

  final int    TIMEOUT = 30000;
  final int    RETRY   = 3;
  final String vocabulary = "abcdefghijklmnopqrstuvwxyz";

  ZooKeeper[]   consumers;
  String        directory = "/telex";
  SimpleWatcher watcher;
  DistributedQueue[] handles;

  public static void main(final String[] args) {
    int numConsumers = 2;
    if(args.length > 0 && args[0] != null) {
      numConsumers = new Integer(args[0]);
    }
    new Telex(numConsumers).run();
  }

  Telex(final int numConsumers) {
    try {
      consumers = new ZooKeeper[numConsumers];
      handles   = new DistributedQueue[numConsumers];
      watcher   = new SimpleWatcher();

      for(int i = 0; i < numConsumers; i++) {
	consumers[i] = new ZooKeeper(host + ":" + port, TIMEOUT, watcher);
	handles[i]   = new DistributedQueue(consumers[i], directory, null);
      }
    } catch(IOException e) {
      System.out.println("Initialization error: " + e.getMessage());
      System.exit(1);
    }
  }

  public void run() {
    final int numThreads = consumers.length;
    Thread[] threads = new Thread[numThreads];

    for(int i = 0; i < consumers.length; i++) {
      final int id = i;
      threads[i] = new Thread() {
        public void run() {
	  try {
	    System.out.println("Consumer " + id + " started.");
	    int    attempts = 0;
            byte[] msg;
	    while((msg = handles[id].poll()) != null || attempts < RETRY) {
	      if (msg == null) {
	        attempts++;
	      } else {
	        System.out.println("            " + (new String(msg)) + " > Consumer " + id);
	      }
	    }
          } catch(KeeperException e) {} catch(InterruptedException e) {}
	}
      };
      threads[i].start();
    }

    Thread publisher = new Thread() {
      public void run() {
	try {
	  for(int idx = 0; idx < vocabulary.length(); idx++) {
	    final String msg = vocabulary.substring(idx, idx + 1);
	    handles[0].offer(msg.getBytes());
	    System.out.println("Published > " + msg);
	  }
	} catch(KeeperException e) {} catch(InterruptedException e) {}
      }
    };
    publisher.start();

    try {
      publisher.join();
      for(int i = 0; i < consumers.length; i++) {
        threads[i].join();
      }
    } catch(InterruptedException e) {}

    System.out.println("Completed.");
  }

}

