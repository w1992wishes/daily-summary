package me.w1992wishes.sync.standard.container;

import me.w1992wishes.common.util.Configuration;
import org.apache.commons.lang3.Validate;

/**
 * @author w1992wishes 2019/3/27 16:49
 */
public abstract class AbstractContainer {

    protected Configuration configuration;

    public AbstractContainer(Configuration configuration) {
        Validate.notNull(configuration, "Configuration can not be null.");

        this.configuration = configuration;
    }

    public abstract void start();

}
