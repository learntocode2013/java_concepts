import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomBoundedBuffer {
    private static final Logger LOG = Logger.getLogger(CustomBoundedBuffer.class.getName());
    private final Object[] buffer;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private int putPtr;
    private int takePtr;
    private int count;
    private final int capacity;

    public CustomBoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    public void put(Object e) throws InterruptedException {
        lock.lock();
        Thread current = Thread.currentThread();
        try {
            while (isFull()) {
                LOG.log(Level.INFO, "{0} is waiting for empty space in buffer",
                        current.getName());
                notFull.await();
            }
            buffer[putPtr] = e;
            if (++putPtr == capacity) {
                putPtr = 0;
            }
            ++count;
            LOG.log(Level.INFO, "{0} Put {1}", new Object[]{current.getName(), e});
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public Object take() throws InterruptedException {
        lock.lock();
        Thread current = Thread.currentThread();
        try {
            while (isEmpty()) {
                LOG.log(Level.INFO, "{0} is waiting for data in buffer", current.getName());
                notEmpty.await();
            }
            Object e = buffer[takePtr];
            if (++takePtr == capacity) {
                takePtr = 0;
            }
            --count;
            notFull.signal();
            return e;
        } finally {
            lock.unlock();
        }
    }

    public int  size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isFull() {
        return count == capacity;
    }
}
