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
  final String vocabulary = "abcdefghijklmnopqrstuvwxyz";

  ZooKeeper[]   consumers;
  String        directory  = "/telex";
  SimpleWatcher watcher;
  DistributedQueue[] handles;
  int           lastPort = 3000;

  public static void main(final String[] args) {
    int numConsumers = 1;
    if(args.length > 0 && args[0] != null) {
      numConsumers = new Integer(args[0]);
    }
    new Telex(numConsumers).run();
  }

  Telex(final int numConsumers) {
    try {
      consumers = new ZooKeeper[numConsumers];
      handles   = new DistributedQueue[numConsumers];
      watcher   = new SimpleWatcher(numConsumers);

      for(int i = 0; i < numConsumers; i++) {
	consumers[i] = new ZooKeeper(host + ":" + assignPort(), TIMEOUT, watcher);
	handles[i] = new DistributedQueue(consumers[i], directory, null);
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

    try{
      System.out.println("Waiting for setup?");
      Thread.sleep(1000);
      System.out.println(handles[0].toString());
    } catch(InterruptedException e) {}

    Thread publisher = new Thread() {
      public void run() {
	try {
//	  for(int idx = 0; idx < vocabulary.length() - 1; idx++) {
//	    final String msg = vocabulary.substring(idx, idx + 1);
	    final String msg = vocabulary.substring(3, 4);
	    boolean offered = handles[0].offer(msg.getBytes());
	    System.out.println(offered);
	    System.out.println("Published > " + msg);
//	  }
	} catch(KeeperException e){
	//  e.printStackTrace();
	//  System.exit(4);
	} catch(InterruptedException e){
	//  e.printStackTrace();
	//  System.exit(5);
	}
      }
    };
    publisher.start();
    try {
      publisher.join();
    } catch(InterruptedException e){
    }

    for(int i = 0; i < consumers.length; i++) {
      try {
        threads[i].join();
      } catch(InterruptedException e){
//	System.out.println("Interrupted: " + e.getMessage());
//	System.exit(6);
      }
    }

    //try {
    //  watcher.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);
    //} catch(InterruptedException e) {
     // e.printStackTrace();
     // System.exit(3);
    //}

    System.out.println("Completed.");
  }


  private int assignPort() {
    return (lastPort += 1);
  }

}

