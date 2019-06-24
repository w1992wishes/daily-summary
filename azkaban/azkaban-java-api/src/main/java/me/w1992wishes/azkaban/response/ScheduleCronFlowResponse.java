package me.w1992wishes.azkaban.response;

/**
 * 定时调度Flow响应
 *
 * @author Administrator
 */
public class ScheduleCronFlowResponse extends AzkabanBaseResponse {
    private String scheduleId;

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }
}
