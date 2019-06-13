package me.w1992wishes.jvm;

/**
 * VM args:-Xss2M
 */
public class JavaVMStatckOOM {
    public void dontStop() {
        while (true) {
        }
    }

    public void stackLeakByThread() {
        while (true) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    dontStop();
                }
            });
            t.start();
        }
    }

    public static void main(String[] args) {
        JavaVMStatckOOM sofm = new JavaVMStatckOOM();
        sofm.stackLeakByThread();
    }
}