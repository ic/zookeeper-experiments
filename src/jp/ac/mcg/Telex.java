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
  final int TIMEOUT = 30000;

  ZooKeeper[] consumers;
  String      directory  = "/telex";
  String      vocabulary = "abcdefghijklmnopqrstuvwxyz";
  SimpleWatcher     watcher;
  DistributedQueue[] handles;
  int       nextPort = 31457;

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
      watcher   = new SimpleWatcher(new CountDownLatch(numConsumers));

      for(int i = 0; i < numConsumers; i++) {
	consumers[i] = new ZooKeeper(host + ":" + assignPort(), TIMEOUT, watcher);
	if (!watcher.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS)) {
          System.out.println("Timeout: Unable to connect to server");
	  System.exit(1);
        }
	handles[i] = new DistributedQueue(consumers[i], directory, null);
      }
    } catch(IOException e) {
      System.out.println("Initialization error: " + e.getMessage());
      System.exit(1);
    } catch(InterruptedException e) {
     // e.printStackTrace();
     // System.exit(3);
    }
  }

  public void run() {
    int numThreads = consumers.length;
    Thread[] threads = new Thread[numThreads];

    for(int i = 0; i < consumers.length; i++) {
      final int id = i;
      threads[i] = new Thread() {
        public void run() {
	  try {
            final String msg = new String(handles[id].take());
	    System.out.println("             " + msg + " > Consumer " + id);
          } catch(KeeperException e) {
	   // e.printStackTrace();
	   // System.exit(2);
	  } catch(InterruptedException e) {
	   // e.printStackTrace();
	   // System.exit(3);
	  }
	}
      };
      threads[i].start();
    }

    try {
      for(int idx = 0; idx < vocabulary.length() - 1; idx++) {
	final String msg = vocabulary.substring(idx, idx + 1);
	System.out.println("Publishing > " + msg);
	handles[idx % numThreads].offer(msg.getBytes());
      }
    } catch(KeeperException e){
    //  e.printStackTrace();
    //  System.exit(4);
    } catch(InterruptedException e){
    //  e.printStackTrace();
    //  System.exit(5);
    }

    for(int i = 0; i < consumers.length; i++) {
      try {
        threads[i].join();
      } catch(InterruptedException e){
//	System.out.println("Interrupted: " + e.getMessage());
//	System.exit(6);
      }
    }

    System.out.println("Completed.");
  }


  private int assignPort() {
    return (nextPort += 1);
  }

}

