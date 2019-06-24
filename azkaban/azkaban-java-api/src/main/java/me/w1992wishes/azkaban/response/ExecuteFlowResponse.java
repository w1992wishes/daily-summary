package me.w1992wishes.azkaban.response;

/**
 * 执行flow响应
 *
 * @author Administrator
 */
public class ExecuteFlowResponse extends AzkabanBaseResponse {
    private String project;
    private String flow;
    private String execId;

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getExecId() {
        return execId;
    }

    public void setExecId(String execId) {
        this.execId = execId;
    }

}
