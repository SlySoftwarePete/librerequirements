package uk.co.slysoftware.librerequirements;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import static uk.co.slysoftware.librerequirements.ErrorHandler.failWithTrace;

public class RequirementCursor {

    private ResultSet results;

    public RequirementCursor(ResultSet results) {
        this.results = results;
    }

    public boolean next() {
        boolean hasNext = false;
        try {
            hasNext = results.next();
        } catch (SQLException e) {
            failWithTrace(e);
        }
        return hasNext;
    }

    public Requirement getRequirement() {
        Requirement req = new Requirement();

        try {
            req.setDocTitle(results.getString("doc_title"));
            req.setIssue(results.getString("issue"));
            req.setDescription(results.getString("description"));
            req.setTag(results.getString("tag"));
            req.setIssueUrl(results.getString("issueUrl"));
        } catch (SQLException e) {
            failWithTrace(e);
        }
        return req;
    }

    public void close() {
        try {
            results.close();
        } catch (SQLException e) {
            // Who cares
        }
    }
}
