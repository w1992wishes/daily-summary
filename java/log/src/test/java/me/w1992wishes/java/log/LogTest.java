package me.w1992wishes.java.log;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogTest {

    // 将 pom 中 28 行后得实现先注释，运行无法打印日志
    // 打开 logback-classic，正常打印日志
    // 将所有注释打开，正常打印，但提示引入了多个 slf4j
    @Test
    public void testSlf4j(){
        Logger logger = LoggerFactory.getLogger(getClass().getName());
        logger.info("-------333------");
    }

}
