package uk.co.slysoftware.librerequirements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RequirementList {

    private static Logger log = LoggerFactory.getLogger(RequirementList.class);

    private Map<Short, Requirement> requirements = new HashMap<>();

    private Requirement getRequirement(short id) {
        Requirement result = requirements.get(id);
        if (result == null) {
            result = new Requirement();
            requirements.put(id, result);
        }
        return result;
    }

    public void setName(short id, String name) {
        log.info("Requirement id: " + id + " name: " + name);
        getRequirement(id).setTag(name);
    }

    public void setDescription(short id, String description) {
        log.info("Requirement id: " + id + " description: " + description);
        getRequirement(id).setDescription(description);
    }

    public void setIssue(short id, String issue) {
        log.info("Requirement id: " + id + " issue: " + issue);
        getRequirement(id).setIssue(issue);
    }

    public void setIssueUrl(short id, String issueUrl) {
        log.info("Requirement id: " + id + " issueUrl: " + issueUrl);
        getRequirement(id).setIssueUrl(issueUrl);
    }

    public void setDocTitle(short id, String docTitle) {
        log.info("Requirement id: " + id + " docTitle: " + docTitle);
        getRequirement(id).setDocTitle(docTitle);
    }

    public Collection<Requirement> getRequirements() {
        return requirements.values();
    }
}
