package me.w1992wishes.azkaban.response;

/**
 * 上传项目zip包响应
 *
 * @author Administrator
 */
public class ProjectZipResponse extends AzkabanBaseResponse {
    private String projectId;
    private String version;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
