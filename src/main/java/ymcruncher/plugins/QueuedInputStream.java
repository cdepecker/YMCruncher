package ymcruncher.plugins;

import com.google.auto.service.AutoService;
import ymcruncher.core.InputPlugin;

import java.io.InputStream;

/**
 * An input stream that implements a circular queue.
 * Reading & writing block each other.
 */
public class QueuedInputStream extends InputStream {
    public boolean blockUntilFull;
    private byte buf[];     // circular list of bytes
    private int bufsize;    // size of the list
    private int head;       // where next read comes from
    private int capacity;   // how many bytes in the buf
    // reading will block until at full capacity

    QueuedInputStream(int buflen) {
        bufsize = buflen;
        buf = new byte[buflen];
    }

    public final synchronized void waitUntilEmpty() {
        try {
            while (capacity > 0)
                wait();
        } catch (InterruptedException e) {
        }
    }

    public final int available() {
        return capacity;
    }

    public final synchronized int read() {
        try {
            while (capacity == 0 || blockUntilFull)
                wait();
            int a = buf[head] & 0xff;
            if (++head == bufsize)
                head = 0;
            capacity--;
            notify();
//            System.out.println("read 1 cap " + capacity);
            return a;
        } catch (InterruptedException e) {
            return -1;
        }
    }

    public final synchronized int read(byte b[], int off, int len) {
        if (len == 0)
            return 0;
        try {
            while (capacity == 0 || blockUntilFull) {
//                System.out.println("wait read");
                wait();
            }
            int numread = 0;
            while (capacity > 0 && numread < len) {
                b[off++] = buf[head];
                if (++head == bufsize)
                    head = 0;
                capacity--;
                numread++;
            }
            notify();
//            System.out.println("read " + numread + " cap " + capacity);
            return numread;
        } catch (InterruptedException e) {
            return -1;
        }
    }

    public final synchronized void write(byte b) {
        try {
            while (capacity == bufsize) {
                if (blockUntilFull) {
                    blockUntilFull = false;
                    notify();
                }
                wait();
            }
            int pos = (head + capacity) % bufsize;
            buf[pos] = b;
            capacity++;
            notify();
        } catch (InterruptedException e) {
        }
    }

    public final synchronized void write(byte b[], int off, int len) {
        /*
        while (len-- > 0)
            write(b[off++]);
        */
        try {
//            System.out.println("write " + len);
            int pos = (head + capacity) % bufsize;
            while (len > 0) {
                if (capacity == bufsize) {
                    blockUntilFull = false;
                    notify();
                    while (capacity == bufsize) {
//                        System.out.println("wait write");
                        wait();
                    }
                }
                buf[pos++] = b[off++];
                if (pos == bufsize)
                    pos = 0;
                capacity++;
                len--;
            }
            notify();
        } catch (InterruptedException e) {
        }
    }

}
