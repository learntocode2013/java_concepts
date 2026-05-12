import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleNonReEntrantMutex {
    private static final Logger log = Logger.getLogger(SimpleNonReEntrantMutex.class.getName());
    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int acquires) {
            Thread current =  Thread.currentThread();
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                log.log(Level.INFO, "Mutex acquired by {0}",  current);
                return true;
            }
            log.log(Level.WARNING, "{0} failed to acquire mutex bcoz it was owned by another thread: {1}",
                    new Object[]{current, getExclusiveOwnerThread()});
            return false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            Thread current =  Thread.currentThread();
            if (!isHeldExclusively()) {
                log.log(Level.WARNING, "{0} failed to release mutex bcoz it was owned by another thread",
                        current);
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            log.log(Level.INFO, "Mutex released by {0}",  current);
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
    }
    private final Sync sync = new Sync();

    public boolean lock() {
        return sync.tryAcquire(1);
    }

    public boolean unlock() {
        return sync.tryRelease(1);
    }
}
