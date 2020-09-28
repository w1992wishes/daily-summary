package me.w1992wishes.common.util;

import me.w1992wishes.common.exception.MyException;

import java.lang.reflect.Method;
import java.util.concurrent.*;

/**
 * @author w1992wishes
 */
public class CallMethod {

    /***
     * 方法参数说明
     * @param target 调用方法的当前对象
     * @param methodName 方法名称
     * @param parameterTypes 调用方法的参数类型
     * @param params 参数  可以传递多个参数
     * @param timeout 参数  超时时间
     * @param unit 参数  超时单位
     * */
    public static Object callMethod(final Object target, final String methodName, final Class<?>[] parameterTypes,
                                    final Object[] params, long timeout, TimeUnit unit) throws MyException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        FutureTask<String> future = new FutureTask<>(() -> {
            String value;
            try {
                Method method;
                method = target.getClass().getDeclaredMethod(methodName, parameterTypes);

                Object returnValue = method.invoke(target, params);
                value = returnValue != null ? returnValue.toString() : null;
            } catch (Exception e) {
                throw new MyException("方法调用异常", e);
            }
            return value;
        });

        executorService.execute(future);
        String result;
        try {
            result = future.get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new MyException("方法执行中断");
        } catch (ExecutionException e) {
            future.cancel(true);
            throw new MyException("Excution异常");
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new MyException("invoke timeout", e);
        }
        executorService.shutdownNow();
        return result;
    }

    public Object call(Integer id) {
        try {
            Thread.sleep(11000);
        } catch (Exception e) {
        }
        return id;
    }

    public static void main(String[] args) throws MyException {
        System.out.println(callMethod(new CallMethod(), "call", new Class<?>[]{Integer.class}, new Object[]{1523}, 10, TimeUnit.SECONDS));
    }
}