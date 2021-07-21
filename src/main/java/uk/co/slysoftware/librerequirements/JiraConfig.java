package uk.co.slysoftware.librerequirements;

public class JiraConfig {

    private String apiToken;

    private String user;

    private String tagFieldName = "summary";

    private String descriptionFieldName = "description";

    private String baseEndpoint;

    public JiraConfig(String url, String user, String token) {
        this.baseEndpoint = url;
        this.user = user;
        this.apiToken = token;
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getUser() {
        return user;
    }

    public String getTagFieldName() {
        return tagFieldName;
    }

    public String getDescriptionFieldName() {
        return descriptionFieldName;
    }

    public String getBaseEndpoint() {
        return baseEndpoint;
    }
}
