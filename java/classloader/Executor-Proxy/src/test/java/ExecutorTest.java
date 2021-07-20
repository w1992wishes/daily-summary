import me.w1992wishes.classloader.executor.common.Executor;
import me.w1992wishes.classloader.executor.proxy.ExecutorProxy;
import org.junit.jupiter.api.Test;

public class ExecutorTest {
    
    @Test
    public void testExecuteV1() {
        Executor executor = new ExecutorProxy("v1");
        executor.execute("TOM");
    }
    
    @Test
    public void testExecuteV2() {
        Executor executor = new ExecutorProxy("v2");
        executor.execute("TOM");
    }
    
}