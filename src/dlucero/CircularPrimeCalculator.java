package dlucero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

 
public class CircularPrimeCalculator {

  private static final int CALC_LIMIT = 1000000;
  private static final int THREAD_COUNT = 4;
  private final Object lock = new Object();
  private final Thread[] ths;
  private final List<Integer> circularPrimes;
  private final BlockingQueue<Integer> queue;
  private final CyclicBarrier barrier;


  public CircularPrimeCalculator() {
    ths = new Thread[THREAD_COUNT];
    circularPrimes = Collections.synchronizedList(new ArrayList());
    queue = new ArrayBlockingQueue(THREAD_COUNT, true);
    barrier = new CyclicBarrier(THREAD_COUNT);
  }

  public void execute() throws InterruptedException {

    final boolean[] nums = new boolean[CALC_LIMIT + 1];

    for (int tid = 0; tid < THREAD_COUNT; tid++) {
      final Thread t = new Thread(new Worker(tid, nums));
      t.setName("Worker-Thread-" + tid);
      ths[tid] = t;
      t.start();
    }
    int root = (int) Math.ceil(Math.sqrt((double) CALC_LIMIT));
    for (int i = 3; i <= root; i += 2) {
      if (!nums[i]) {
        if (i - 3 < THREAD_COUNT) {
          synchronized (lock) {
            queue.add(i);
            lock.wait();
          }
        } else {
          queue.put(i);
        }
      }
    }

    for (int tid = 0; tid < THREAD_COUNT; tid++) {
      queue.put(-1);
    }

    if (CALC_LIMIT > 2) {
      circularPrimes.add(2);
    }

    for (int tid = 0; tid < THREAD_COUNT; tid++) {
      ths[tid].join();
    }
  }
  public Set<Integer> getResult() {
    Set<Integer> result = new TreeSet(circularPrimes);
    return result;
  }
  public void report() {
    System.out.printf("Cantidad de primos circulares: %d\n", circularPrimes.size());
    System.out.printf("Lista: \n");
    Iterator<Integer> it = getResult().iterator();
    while (it.hasNext()) {
      System.out.println(it.next());
    }
  }

  public static void main(String[] args) throws InterruptedException {
    CircularPrimeCalculator pu = new CircularPrimeCalculator();
    pu.execute();
    pu.report();
  }

  private class Worker implements Runnable {

    private final int TID;
    private final boolean[] nums;

    public Worker(int TID, boolean[] nums) {
      this.TID = TID;
      this.nums = nums;
    }

    @Override
    public void run() {
      while (true) {
        if (processNextStep()) {
          break;
        }
      }
      waitSStep();
      searchForCircularNumbers();
    }

    private void waitSStep() {
      try {
        barrier.await();
      } catch (BrokenBarrierException | InterruptedException ex) {
        Logger.getLogger(CircularPrimeCalculator.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    private void searchForCircularNumbers() {
      for (int i = 3 + (2 * (TID)); i < CALC_LIMIT; i += (2 * THREAD_COUNT)) {
        if (!nums[i] && filter(i)) {
          int orbit = i;
          boolean prime = true;
          do {
            orbit = orbit(orbit);
            if (orbit == i) {
              break;
            }
            prime = !nums[orbit];
          } while (prime);

          if (prime) {
            circularPrimes.add(i);
          }
        }
      }
    }

    private boolean processNextStep() {
      try {
        int step = queue.take();
        if (step == -1) {
          return true;
        }
        for (int j = step; j <= CALC_LIMIT / step; j++) {
          nums[step * j] = true;
          if (step - 3 <= THREAD_COUNT && j == step + 1) {
            synchronized (lock) {
              lock.notify();
            }
          }
        }
      } catch (InterruptedException ex) {
        Logger.getLogger(CircularPrimeCalculator.class.getName()).log(Level.SEVERE, null, ex);
      }
      return false;
    }

    private boolean filter(int n) {
      if (n < 10) {
        return true;
      }
      int o = n;
      do {
        int d = o % 10;
        if (d == 1 || d == 3 || d == 7 || d == 9) {
        } else {
          return false;
        }
        o /= 10;
      } while (o > 0);
      return true;
    }

    private int orbit(int n) {
      int n_10 = n / 10;
      int orbit = n % 10;
      n = n_10;
      while (n != 0) {
        orbit *= 10;
        n /= 10;
      }
      orbit += n_10;
      return orbit;
    }

  }

}
