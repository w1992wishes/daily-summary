package me.w1992wishes.script.flyway.config;

import me.w1992wishes.common.util.DateUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * flyway配置类
 */
@Component
public class FlywayConfig {

    @Resource
    private ApplicationArguments arguments;
    @Resource
    private DataSource dataSource;
    @Value("${bizCode}")
    private String bizCode;
    @Value("${algVersion}")
    private String algVersion;
    @Value("${flyway.table}")
    private String flywayTable;
    @Value("${flyway.baselineOnMigrate}")
    private Boolean flywayBaselineOnMigrate;
    @Value("${flyway.baselineVersion}")
    private String flywayBaselineVersion;

    public void migrate() {
        List<String> dbNames = arguments.getOptionValues("dirName");
        if(CollectionUtils.isEmpty(dbNames)) {
            throw new RuntimeException("启动脚本需带有dbName参数");
        }
        String dbName = dbNames.get(0);

        List<String> schemas = arguments.getOptionValues("schemas");
        String schema = null;
        if(!CollectionUtils.isEmpty(schemas)) {
            schema = schemas.get(0);
        }
        schema = StringUtils.isEmpty(schema) ? "public" : schema;

        //初始化flyway类
        Flyway flyway = new Flyway();
        //设置加载数据库的相关配置信息
        flyway.setDataSource(dataSource);
        //设置flyway扫描sql升级脚本、java升级脚本的目录路径或包路径，默认"db/migration"，可不写
        flyway.setLocations("db/"+dbName);
        flyway.setSchemas(schema);
        
        String yesterday = DateUtils.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtils.DF_YMD);
        String today = DateUtils.dateTimeToStr(LocalDateTime.now(), DateUtils.DF_YMD);
        String tomorrow = DateUtils.dateTimeToStr(LocalDateTime.now().plusDays(1), DateUtils.DF_YMD);
        String partitionYesterday = "p".concat(DateUtils.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtils.DF_YMD_NO_LINE));
        String partitionToday = "p".concat(DateUtils.dateTimeToStr(LocalDateTime.now(), DateUtils.DF_YMD_NO_LINE));
        //String dirName = "classpath:db";

        Map<String, String> placeHolders = new HashMap<>();
        
        List<String> buzVersions = arguments.getOptionValues("bizCode");
        if(!CollectionUtils.isEmpty(buzVersions)) {
        	placeHolders.put("bizCode", buzVersions.get(0));
        }else if(bizCode != null) {
        	placeHolders.put("bizCode", bizCode);
        }else {
        	placeHolders.put("bizCode", "bigdata");
        }
        
        List<String> algVersions = arguments.getOptionValues("algVersion");
        if(!CollectionUtils.isEmpty(algVersions)) {
        	placeHolders.put("algVersion", algVersions.get(0));
        }else if(algVersion != null) {
        	placeHolders.put("algVersion", algVersion);
        }else {
        	placeHolders.put("algVersion", "5029");
        }
        
        placeHolders.put("partitionYesterday", partitionYesterday);
        placeHolders.put("partitionToday", partitionToday);
        placeHolders.put("yesterday", yesterday);
        placeHolders.put("today", today);
        placeHolders.put("tomorrow", tomorrow);
        
        flyway.setPlaceholders(placeHolders);
        
        flyway.setBaselineOnMigrate(flywayBaselineOnMigrate);
        flyway.setBaselineVersion(MigrationVersion.fromVersion(flywayBaselineVersion));
        flyway.setTable(flywayTable);
        
        flyway.migrate();
    }

}
