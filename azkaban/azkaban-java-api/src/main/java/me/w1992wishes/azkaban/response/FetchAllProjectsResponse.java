package me.w1992wishes.azkaban.response;

import java.util.List;

/**
 * 查询所有项目的响应
 *
 * @author Administrator
 */
public class FetchAllProjectsResponse extends AzkabanBaseResponse {
    private List<Project> projects;

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

}

