package me.w1992wishes.classloader.executor.v1;

import me.w1992wishes.classloader.executor.common.AbstractExecutor;

public class ExecutorV1 extends AbstractExecutor {

    @Override
    public void execute(final String name) {
        this.handle(new Handler() {
            @Override
            public void handle() {
                System.out.println("V1:" + name);
            }
        });
    }

}