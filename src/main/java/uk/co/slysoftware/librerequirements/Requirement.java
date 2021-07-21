package uk.co.slysoftware.librerequirements;

public class Requirement {

    private String docTitle;

    private String tag;

    private String description;

    private String issue;

    private String issueUrl;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getIssueUrl() {
        return issueUrl;
    }

    public void setIssueUrl(String issueUrl) {
        this.issueUrl = issueUrl;
    }

    public boolean isValid() {
        if (
                tag == null
                || tag.isBlank()
                || description == null
                || description.isBlank()
                || (issue != null && issueUrl == null)
                || (issueUrl != null && issue == null)

        ) {
            return false;
        }
        return true;
    }

    public boolean hasIssue() {
        return issue != null;
    }
}
