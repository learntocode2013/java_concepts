import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.logging.Logger;

public class LockFreeStack<T> {
    private static final Logger log = Logger.getLogger(LockFreeStack.class.getName());

    private record Node<T>(T value, Node<T> next) {
    }

    private final AtomicStampedReference<Node<T>> head = new AtomicStampedReference<>(null, 0);

    public void push(T value) {
        int[] stampHolder = new int[1];
        Node<T> currentHead;
        Node<T> newHead;
        int currentStamp;
        do {
            currentHead = head.get(stampHolder);
            currentStamp = stampHolder[0];
            newHead = new Node<>(value, currentHead);
        }while(!head.compareAndSet(currentHead, newHead, currentStamp, currentStamp + 1));
    }

    public T pop() {
        int[] stampHolder = new int[1];
        Node<T> oldHead;
        Node<T> newHead;
        int currentStamp;
        do {
            oldHead = head.get(stampHolder);
            if(oldHead == null) {
                return null;
            }
            currentStamp = stampHolder[0];
            newHead = oldHead.next;
        } while(!head.compareAndSet(oldHead, newHead, currentStamp, currentStamp + 1));
        return oldHead.value;
    }
}
