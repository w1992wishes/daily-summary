package me.w1992wishes.azkaban.response;

/**
 * 查询执行Job的日志
 *
 * @author Administrator
 */
public class FetchExecJobLogs extends AzkabanBaseResponse {
    private String data;
    private Integer offset;
    private Integer length;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

}
