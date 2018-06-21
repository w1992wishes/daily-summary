package me.w1992wishes.study.springboot.jpa.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Date;

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