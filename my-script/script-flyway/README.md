**1. 说明**
+ xdata flyway进行数据库版本控制，数据库版本为PostgreSQL 8.3.23 (Greenplum Database 5.11.0 build 6f447c2-oss) 

**2. 使用**

在 idea 中设置如下参数运行：
参数：
--dbName=bigdata_odl --schemas=public  --bizCode=bigdata --algVersion=5029

+ 其中，需指定dbName，作为目标数据库；默认schema为public；dirName指定执行目录的sql语句，因为多实例配置时一个目录的语句会对应多个数据库名
+ 要求在工程resources/db目录下新建数据库增量SQL文件，其直接子目录为目标数据库库名。
+ **特别注意：数据库表名结构例子dim\_\${bizCode}\_event\_face\_person\_\${algVersion}默认bizCode=bigdata algVersion=5029可通过参数修改**
+ **特别注意：索引名要带${bizCode}和${algVersion}，避免多实例时冲突**
+ 为支持协同开发，增量SQL文件命名规范为：
```bash
V{yyyyMMdd}.{HH}.{mm}__v{index}_{comment}.sql

```
**3.数据库连接等可配置**

springboot 读取配置有一定优先级：

1. 在jar包的同一目录下建一个config文件夹，然后把配置文件放到这个文件夹下。
2. 直接把配置文件放到jar包的同级目录。
3. 在classpath下建一个config文件夹，然后把配置文件放进去。
4. 在classpath下直接放配置文件。

所以打成 jar 包后，在 jar 包的同级目录下放置 application.properties 即可修改配置，而不用重新打包。

**4.azkaban 中使用**

将 jar 包和配置文件 application.properties 及 .job 文件一起打包即可。