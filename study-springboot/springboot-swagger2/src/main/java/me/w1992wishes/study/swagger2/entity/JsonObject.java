package me.w1992wishes.study.swagger2.entity;

import java.io.Serializable;

/**
 * Description: study-records
 * Created by w1992wishes on 2018/6/7 16:32
 */
public class JsonObject implements Serializable {

    private static final long serialVersionUID = 1918561161454874419L;

    public JsonObject(Object data) {
        this.data = data;
    }

    public JsonObject() {
    }

    public JsonObject(Object data, Object resultCode) {
        this.data = data;
        this.resultCode = resultCode;
    }

    public JsonObject(Object data, Object resultCode, Object maxPage) {
        this.data = data;
        this.resultCode = resultCode;
        this.maxPage = maxPage;
    }

    public JsonObject(Object data, Object resultCode, Object maxPage, int total) {
        this.data = data;
        this.resultCode = resultCode;
        this.maxPage = maxPage;
        this.total = total;
    }

    // Setter,getters
    private Object data = "";
    private Object resultCode = 0;
    private Object maxPage = 0;
    private int total = 0;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getResultCode() {
        return resultCode;
    }

    public void setResultCode(Object resultCode) {
        this.resultCode = resultCode;
    }

    public Object getMaxPage() {
        return maxPage;
    }

    public void setMaxPage(Object maxPage) {
        this.maxPage = maxPage;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}

