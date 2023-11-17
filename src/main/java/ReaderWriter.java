import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReaderWriter {

    private static final Lock lock = new ReentrantLock();
    private static boolean isWriting = false;
    private static int numReading = 0;
    private static final Condition notWriting = lock.newCondition();
    private static final Condition notReadingOrWriting = lock.newCondition();

    private static String fakeFile = "";

    private static class Reader implements Runnable {
        public void run() {
            lock.lock();
            try {
                while (isWriting) {
                    notWriting.await();
                }
                int x = ++numReading;
                lock.unlock();
                String contents = fakeFile;
                System.out.println(contents + " " + x);
                lock.lock();
                numReading--;
                if(numReading == 0 && !isWriting) {
                    notReadingOrWriting.signal();
                    notWriting.signalAll();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                lock.unlock();
            }
        }
    }

    private static class Writer implements Runnable {
        private final String contents;

        public Writer(String contentsToWrite) {
            this.contents = contentsToWrite;
        }

        // Assigning isWriting is necessary for the critical sections.
        @SuppressWarnings("UnusedAssignment")
        public void run() {
            lock.lock();
            try {
                while (isWriting || numReading > 0) {
                    notReadingOrWriting.await();
                }
                isWriting = true;
                fakeFile += this.contents;
                isWriting = false;
                notReadingOrWriting.signal();
                notWriting.signalAll();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(10);
        for(int i = 0; i < 26; i++) {
            es.submit(
                    new Writer(""+ (char)('A'+i))
            );
            es.submit(
                    new Reader()
            );
        }
        es.shutdown();
        es.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println(numReading);
    }
}
