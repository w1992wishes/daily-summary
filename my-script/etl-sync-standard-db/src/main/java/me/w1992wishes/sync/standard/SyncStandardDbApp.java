package me.w1992wishes.sync.standard;

import me.w1992wishes.common.util.Configuration;
import me.w1992wishes.sync.standard.container.AbstractContainer;
import me.w1992wishes.sync.standard.container.TaskGroupContainer;

import java.io.File;

/**
 * @author w1992wishes 2019/3/26 17:48
 */
public class SyncStandardDbApp {

    public static void main(String[] args) {
        Configuration configuration = Configuration.from(new File("./config/mapper.json"));
        AbstractContainer container = new TaskGroupContainer(configuration);
        container.start();
    }
}
