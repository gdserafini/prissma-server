package br.pucpr.prissma_server.schedule;

public class ScheduleResponsibilityRequest {

    private String userResponsibility;

    public ScheduleResponsibilityRequest() {}

    public ScheduleResponsibilityRequest(String userResponsibility) {
        this.userResponsibility = userResponsibility;
    }

    public String getUserResponsibility() { return userResponsibility; }
    public void setUserResponsibility(String userResponsibility) { this.userResponsibility = userResponsibility; }
}
