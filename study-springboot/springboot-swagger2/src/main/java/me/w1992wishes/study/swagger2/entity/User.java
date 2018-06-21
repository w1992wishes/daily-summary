package me.w1992wishes.study.swagger2.entity;

import java.util.Date;

/**
 * Description: study-records
 * Created by w1992wishes on 2018/6/7 16:31
 */
public class User {

    private int id;
    private String username;
    private int age;
    private Date ctm;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Date getCtm() {
        return ctm;
    }

    public void setCtm(Date ctm) {
        this.ctm = ctm;
    }
}
