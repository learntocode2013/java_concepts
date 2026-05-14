import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class LockFreeStack<T> {
    private static final Logger log = Logger.getLogger(LockFreeStack.class.getName());

    private record Node<T>(T value, Node<T> next) {
    }

    private final AtomicReference<Node<T>> head = new AtomicReference<>();

    public void push(T value) {
        Node<T> currentHead;
        Node<T> newHead;
        do {
            currentHead = head.get();
            newHead = new Node<>(value, currentHead);
        }while(!head.compareAndSet(currentHead, newHead));
    }

    public T pop() {
        Node<T> oldHead;
        Node<T> newHead;
        do {
            oldHead = head.get();
            if(oldHead == null) {
                return null;
            }
            newHead = oldHead.next;
        } while(!head.compareAndSet(oldHead, newHead));
        return oldHead.value;
    }
}
