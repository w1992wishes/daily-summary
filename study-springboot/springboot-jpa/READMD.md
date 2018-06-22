# springboot-jpa

## 引入依赖

```
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
    
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-annotations</artifactId>
        <version>2.9.0</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

## 配置数据源

```
spring:
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://localhost:3306/test?charset=utf8mb4&serverTimezone=UTC
    username: root
    password: introcks
    tomcat:
      max-active: 20
      test-while-idle: true
      validation-query: select 1
      default-auto-commit: false
      min-idle: 15
      initial-size: 15
  jpa:
    hibernate:
      ddl-auto: update
      show-sql: true
  jackson:
    serialization:
      indent-output: true
```

## entity

```java
@Entity
@Table(name = "comment")
public class Comment {

    public Comment() {
    }

    public Comment(User user, Weibo weibo, String commentText, Date commentDate) {
        this.user = user;
        this.weibo = weibo;
        this.commentText = commentText;
        this.commentDate = commentDate;
    }

    private long commentId;
    private User user;
    @JsonIgnore
    private Weibo weibo;
    private String commentText;
    private Date commentDate;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
    }

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weibo_id")
    public Weibo getWeibo() {
        return weibo;
    }

    public void setWeibo(Weibo weibo) {
        this.weibo = weibo;
    }

    @Column(name = "comment_text")
    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    @Column(name = "comment_date")
    public Date getCommentDate() {
        return commentDate;
    }

    public void setCommentDate(Date commentDate) {
        this.commentDate = commentDate;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "commentId=" + commentId +
                ", user=" + user +
                ", weibo=" + weibo +
                ", commentText='" + commentText + '\'' +
                ", commentDate=" + commentDate +
                '}';
    }
}
```

## repository

JpaRepository 提供了很多常规的CURD操作，很强大。

```java
@NoRepositoryBean
public interface JpaRepository<T, ID> extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T> {
    List<T> findAll();

    List<T> findAll(Sort var1);

    List<T> findAllById(Iterable<ID> var1);

    <S extends T> List<S> saveAll(Iterable<S> var1);

    void flush();

    <S extends T> S saveAndFlush(S var1);

    void deleteInBatch(Iterable<T> var1);

    void deleteAllInBatch();

    T getOne(ID var1);

    <S extends T> List<S> findAll(Example<S> var1);

    <S extends T> List<S> findAll(Example<S> var1, Sort var2);
}
```

这些操作完全不用去实现，通过继承JpaRepository接口，就可以获得基础的CURD操作。

还可以通过Spring规定的接口命名方法自动创建复杂的CRUD操作。

| Keyword | Sample | JPQL snippet |
| :----- | :----- | :---- |
| And | findByLastnameAndFirstname | … where x.lastname = ?1 and x.firstname = ?2 |
| Or | findByLastnameOrFirstname | … where x.lastname = ?1 or x.firstname = ?2 |
| Is,Equals | findByFirstname,findByFirstnameIs,findByFirstnameEquals | … where x.firstname = ?1 |
| Between | findByStartDateBetween | … where x.startDate between ?1 and ?2 |
| LessThan | findByAgeLessThan | … where x.age < ?1 |
| Containing | findByFirstnameContaining | … where x.firstname like ?1 (parameter bound wrapped in %) |

以UserRepository为例：

```java
public interface UserRepository extends JpaRepository<User, Long> {

    //查询用户名称包含username字符串的用户对象
    List<User> findByUsernameContaining(String username);

    //获得单个用户对象，根据username和pwd的字段匹配
    User getByUsernameIsAndUserpwdIs(String username,String pwd);

    //精确匹配username的用户对象
    User getByUsernameIs(String username);

}
```

什么annotation都不用打，也不需要写实现，就可以在被直接使用。

还可以在接口方法中通过定义@Query annotation自定义接口方法的JPQL语句。

```java
public interface WeiboRepository extends JpaRepository<Weibo, Long> {

    @Query("select w from Weibo w where w.user.username = :username")
    List<Weibo> searchUserWeibo(@Param("username") String username);

    @Query("select w from Weibo w where w.user.username = :username")
    List<Weibo> searchUserWeibo(@Param("username") String username, Sort sort);

    @Modifying
    @Transactional(readOnly = false)
    @Query("update Weibo w set w.weiboText = :text where w.user = :user")
    int setUserWeiboContent(@Param("text")String weiboText,@Param("user")User user);

    Page<Weibo> findByUserIsAndWeiboTextContaining(User user, String weiboText, Pageable pageable);

    @Transactional(readOnly = false)
    int deleteByUser(User user);

}
```

Sort提供字段排序的功能，而Pageable则提供分页的功能。

## 测试

```java
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = {"classpath:application.yml"})
@SpringBootTest
public abstract class AbstractServiceTest {

    @Autowired
    protected UserService userService;

    protected User generateUser(String username, String userpwd){
        User user = new User();
        user.setUserId(1);
        user.setUsername(username);
        user.setUserpwd(userpwd);
        return user;
    }
}

public class UserServiceImplTest extends AbstractServiceTest {

    @Test
    public void saveUser(){
        User user = generateUser("alibaba", "alibaba");
        User newUser = userService.save(user);
        Assert.assertEquals(user.getUsername(), newUser.getUsername());
    }

}
```





















