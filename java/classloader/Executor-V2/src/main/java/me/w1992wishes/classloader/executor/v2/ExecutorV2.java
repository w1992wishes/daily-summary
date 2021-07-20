package me.w1992wishes.classloader.executor.v2;

import me.w1992wishes.classloader.executor.common.AbstractExecutor;

public class ExecutorV2 extends AbstractExecutor {

    @Override
    public void execute(final String name) {
        this.handle(new Handler() {
            @Override
            public void handle() {
                System.out.println("V2:" + name);
            }
        });
    }

}
