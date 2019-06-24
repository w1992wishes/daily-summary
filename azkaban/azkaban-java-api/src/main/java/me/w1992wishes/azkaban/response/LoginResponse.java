package me.w1992wishes.azkaban.response;

/**
 * 登录返回信息
 *
 * @author Administrator
 */
public class LoginResponse extends AzkabanBaseResponse {
    private String sessionId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
