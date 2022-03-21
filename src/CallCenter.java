import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;

public class CallCenter {
    /*
       N is the total number of customers that each agent will serve in
       this simulation.
       (Note that an agent can only serve one customer at a time.)
     */
    private static final int NUMBER_OF_THREADS = 8;
    private static final int CUSTOMERS_PER_AGENT = 5;
    private static final int NUMBER_OF_AGENTS = 3;
    private static final int NUMBER_OF_CUSTOMERS = NUMBER_OF_AGENTS * CUSTOMERS_PER_AGENT;
    /*
      NUMBER_OF_THREADS specifies the number of threads to use for this simulation.
      (The number of threads should be greater than the number of agents and greeter combined
      to allow simulating multiple concurrent customers.)
     */
    private static final Queue<Customer> waitQueue = new LinkedList<>();
    private static final Queue<Customer> dispatchQueue = new LinkedList<>();
    private static final ReentrantLock waitLock = new ReentrantLock();
    private static final ReentrantLock dispatchLock = new ReentrantLock();
    private static final Condition dispatchEmpty = dispatchLock.newCondition();
    private static final Condition waitEmpty = waitLock.newCondition();

    /*
        Create the greeter and agents threads first, and then create the customer threads.
     */
    public static void main(String[] args) throws InterruptedException {

        // Insert a random sleep between 0 and 150 ms after submitting every customer task,
        // to simulate a random interval between customer arrivals.
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        executorService.submit(new Greeter());

        for (int i = 0; i < NUMBER_OF_AGENTS; i++) {
            executorService.submit(new Agent(i + 1));
        }

        for (int i = 0; i < NUMBER_OF_CUSTOMERS; i++) {
            executorService.submit(new Customer(i + 1));
            sleep(ThreadLocalRandom.current().nextInt(0, 150));
        }

        executorService.shutdown();
    }

    public static class Agent implements Runnable {

        private final int ID;
        private int served = 0;

        //Feel free to modify the constructor
        public Agent(int i) {
            ID = i;
        }

        /*
        Your Agent implementation must call the method below
        to serve each customer.
        Do not modify this method.
         */
        public void serve(int customerID) {
            System.out.println("Agent " + ID + " is serving customer " + customerID);
            try {
                /*
                   Simulate busy serving a customer by sleeping for a random amount of time.
                */
                sleep(ThreadLocalRandom.current().nextInt(10, 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (served < CUSTOMERS_PER_AGENT) {
                int cID = 0;
                dispatchLock.lock();
                try {
                    while (dispatchQueue.isEmpty()) {
                        dispatchEmpty.await();
                    }
                    cID = dispatchQueue.remove().ID;
                    served += 1;
                } catch (InterruptedException ignored) {
                } finally {
                    dispatchLock.unlock();
                    serve(cID);
                }
            }
        }
    }

    public static class Greeter implements Runnable {

        private int greeted = 0;

        @Override
        public void run() {
            while (greeted < NUMBER_OF_CUSTOMERS) {
                waitLock.lock();
                try {
                    while (waitQueue.isEmpty()) {
                        waitEmpty.await();
                    }

                    Customer customerToGreet = waitQueue.remove();
                    waitLock.unlock();

                    dispatchLock.lock();
                    dispatchQueue.add(customerToGreet);
                    int cID = customerToGreet.ID;
                    int cPlacement = dispatchQueue.size();
                    System.out.printf("Greeting customer %d: your place in queue is %d\n", cID, cPlacement);
                    greeted += 1;
                    dispatchEmpty.signal();
                } catch (InterruptedException ignored) {
                } finally {
                    dispatchLock.unlock();
                }
            }
        }
    }

    public static class Customer implements Runnable {

        private final int ID;

        //Feel free to modify the constructor
        public Customer(int i) {
            ID = i;
        }

        @Override
        public void run() {
            waitLock.lock();
            try {
                waitQueue.add(this);
                waitEmpty.signal();
            } finally {
                waitLock.unlock();
            }
        }
    }
}
