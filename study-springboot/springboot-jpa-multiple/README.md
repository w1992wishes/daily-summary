# springboot-jap-multiple

在项目开发中,常常需要在一个项目中使用多个数据源,因此需要配置Spring data jpa对多数据源的使用.

一般分为以下三步:

* 配置多数据源
* 不同源的repsitory放入不同的包路径
* 声明不同包路径下使用不同的数据源,事物支持

## 一、引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid</artifactId>
        <version>1.1.6</version>
    </dependency>
</dependencies>
```

## 二、配置多数据源

```yaml
#primary
spring:
  primary:
    datasource:
      jdbc-url: jdbc:mysql://localhost:3306/test1
      username: root
      password: introcks1234
      driver-class-name: com.mysql.jdbc.Driver

#secondary
  secondary:
    datasource:
      jdbc-url: jdbc:mysql://localhost:3306/test2
      username: root
      password: introcks1234
      driver-class-name: com.mysql.jdbc.Driver

  jpa:
    database: mysql
    show-sql: true
    hibernate:
      ddl-auto: update
      dialect: org.hibernate.dialect.MySQL5InnoDBDialect
```

这两个DataSource都使用mysql，但是数据库是不同的。此外，在使用多数据源的时候，所有必要配置都不能省略。

## 三、读取两个数据源

创建两个DataSource的Bean，其中一个标记为primaryDataSource，另一个命名为secondaryDataSource

```java
@Configuration
public class DataSourceConfig {

    @Bean(name = "primaryDataSource")
    @Primary
    @Qualifier("primaryDataSource")
    @ConfigurationProperties(prefix = "spring.primary.datasource")
    public DataSource primaryDatasource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "secondaryDataSource")
    @Qualifier("secondaryDataSource")
    @ConfigurationProperties(prefix = "spring.secondary.datasource")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }

}
```

通过@ConfigurationProperties(prefix = "xxx")指定配置项的前缀。

## 四、创建两个JdbcTemplate的Bean，其中一个标记为Primary，另一个命名为secondJdbcTemplate，分别使用对应的DataSource

```java
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "entityManagerFactoryPrimary", // 配置连接工厂 entityManagerFactory
        transactionManagerRef = "transactionManagerPrimary", //配置 事物管理器  transactionManager
        basePackages = {"me.w1992wishes.study.springboot.jpa.multiple.test1.repository"}) //设置Repository所在位置
public class PrimaryConfig {
    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;

    @Autowired
    private JpaProperties jpaProperties;

    @Primary
    @Bean(name = "entityManagerPrimary")
    public EntityManager entityManager(EntityManagerFactoryBuilder builder) {
        return entityManagerFactoryPrimary(builder).getObject().createEntityManager();
    }

    /**
     * 设置实体类所在位置
     *
     * @param builder
     * @return
     */
    @Bean(name = "entityManagerFactoryPrimary")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryPrimary(EntityManagerFactoryBuilder builder) {
        return builder
                //设置数据源
                .dataSource(primaryDataSource)
                //设置数据源属性
                .properties(getVendorProperties())
                //设置实体类所在位置.扫描所有带有 @Entity 注解的类
                .packages("me.w1992wishes.study.springboot.jpa.multiple.test1.entity")
                // Spring会将EntityManagerFactory注入到Repository之中.有了 EntityManagerFactory之后,
                // Repository就能用它来创建 EntityManager 了,然后 EntityManager 就可以针对数据库执行操作
                .persistenceUnit("primaryPersistenceUnit")
                .build();
    }

    /**
     * 配置事物管理器
     *
     * @param builder
     * @return
     */
    @Bean(name = "transactionManagerPrimary")
    @Primary
    PlatformTransactionManager transactionManagerPrimary(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(entityManagerFactoryPrimary(builder).getObject());
    }

    @Primary
    @Bean(name="jdbcTemplatePrimary")
    public JdbcTemplate jdbcTemplatePrimary() {
        return new JdbcTemplate(primaryDataSource);
    }
    
    private Map<String, Object> getVendorProperties() {
        return jpaProperties.getHibernateProperties(new HibernateSettings());
    }
}
```

```java
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "entityManagerFactorySecondary", // 配置连接工厂 entityManagerFactory
        transactionManagerRef = "transactionManagerSecondary", //配置 事物管理器  transactionManager
        basePackages = {"me.w1992wishes.study.springboot.jpa.multiple.test2.repository"}) //设置Repository所在位置
public class SecondaryConfig {
    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;

    @Autowired
    private JpaProperties jpaProperties;

    @Bean(name = "entityManagerSecondary")
    public EntityManager entityManager(EntityManagerFactoryBuilder builder) {
        return entityManagerFactorySecondary(builder).getObject().createEntityManager();
    }

    /**
     * 设置实体类所在位置
     *
     * @param builder
     * @return
     */
    @Bean(name = "entityManagerFactorySecondary")
    public LocalContainerEntityManagerFactoryBean entityManagerFactorySecondary(EntityManagerFactoryBuilder builder) {
        return builder
                //设置数据源
                .dataSource(secondaryDataSource)
                //设置数据源属性
                .properties(getVendorProperties())
                //设置实体类所在位置.扫描所有带有 @Entity 注解的类
                .packages("me.w1992wishes.study.springboot.jpa.multiple.test2.entity")
                // Spring会将EntityManagerFactory注入到Repository之中.有了 EntityManagerFactory之后,
                // Repository就能用它来创建 EntityManager 了,然后 EntityManager 就可以针对数据库执行操作
                .persistenceUnit("SecondaryPersistenceUnit")
                .build();
    }

    /**
     * 配置事物管理器
     *
     * @param builder
     * @return
     */
    @Bean(name = "transactionManagerSecondary")
    PlatformTransactionManager transactionManagerSecondary(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(entityManagerFactorySecondary(builder).getObject());
    }

    @Bean(name="jdbcTemplateSecondary")
    public JdbcTemplate jdbcTemplateSecondary() {
        return new JdbcTemplate(secondaryDataSource);
    }

    private Map<String, Object> getVendorProperties() {
        return jpaProperties.getHibernateProperties(new HibernateSettings());
    }
}
```

## 五、创建 entity 和 repository

User 对应数据源 primaryDataSource， Message 对应数据源 secondaryDataSource：

```java
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer age;

    public User(){}

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
    // 省略getter、setter
}
```

```java
@Entity
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String content;

    public Message(){}

    public Message(String name, String content) {
        this.name = name;
        this.content = content;
    }
    // 省略getter、setter
}
```

## 六、测试两个针对不同数据源的配置进行数据操作

```java
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = {"classpath:application.yml"})
@SpringBootTest
public class JpaMultipleApplicationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MessageRepository messageRepository;

    @Test
    public void test() throws Exception {

        userRepository.save(new User("aaa", 10));
        userRepository.save(new User("bbb", 20));
        userRepository.save(new User("ccc", 30));
        userRepository.save(new User("ddd", 40));
        userRepository.save(new User("eee", 50));

        Assert.assertEquals(5, userRepository.findAll().size());

        messageRepository.save(new Message("o1", "aaaaaaaaaa"));
        messageRepository.save(new Message("o2", "bbbbbbbbbb"));
        messageRepository.save(new Message("o3", "cccccccccc"));

        Assert.assertEquals(3, messageRepository.findAll().size());
    }
}
```