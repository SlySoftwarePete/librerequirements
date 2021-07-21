package uk.co.slysoftware.librerequirements;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;


import java.io.File;
import java.util.List;
import static uk.co.slysoftware.librerequirements.ErrorHandler.fail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static Logger log = LoggerFactory.getLogger("main");

    @Parameter(names = {"-dir"}, description = "Where to find the libreoffice requirements files", required = true)
    private String requirementsDirectory = null;

    @Parameter(names = {"-db_name"}, description = "Name of the sqlite db to create")
    private String dbName = "requirements.db";

    @Parameter(names = {"-project"}, description = "Jira project prefix", required = true)
    private String jiraProject;

    @Parameter(names = {"-jira_token"}, description = "Access token for Jira", required = true)
    private String jiraToken;

    @Parameter(names = {"-jira_url"}, description = "Jira URL", required = true)
    private String jiraUrl;

    @Parameter(names = { "-jira_user"}, description = "Jira user id (normally email address)", required = true)
    private String jiraUser;

    public static void main(String... argv) {

        Main main = new Main();

        // Config can be set by environment variables (for use with docker) or by command line for testing
        // build scripts and stuff. If environment variables are detected don't do command line parsing

        if (!main.probeEnvVariables()) {
            JCommander.newBuilder()
                    .addObject(main)
                    .build()
                    .parse(argv);
        }

        main.run();
        System.exit(0);
    }

    private boolean probeEnvVariables() {
        jiraUser = System.getenv("REQ_JIRA_USER");
        jiraToken = System.getenv("REQ_JIRA_TOKEN");
        jiraUrl = System.getenv("REQ_JIRA_URL");
        jiraProject = System.getenv("REQ_JIRA_PROJECT");

        if (jiraUser == null && jiraToken == null && jiraUrl == null && jiraProject == null) {
            // No configuration has been set via env variables.
            return false;
        }

        // If some env variables have been set than ALL must be set
        if (jiraUser == null)  fail("REQ_JIRA_USER must be set, if any environment variable is used");
        if (jiraToken == null) fail("REQ_JIRA_TOKEN must be set if any environment variable is used");
        if (jiraUrl == null) fail("REQ_JIRA_URL must be set if any environment variable is used");
        if (jiraProject == null) fail("REQ_JIRA_PROJECT must be set if any environment variable is used");

        requirementsDirectory = "/app/data";
        return true;
    }

    private void extractRequirementsFromDoc(LibreDocManager docManager, RequirementDb db, File doc) {

        String docUrl = "file:///" + doc.getAbsolutePath();

        DocProperties docProps = docManager.openDoc(docUrl);
        for (String elementName : docManager.getTables()) {

            RequirementList list;
            switch (elementName) {
                case "Requirements":
                    db.createDoc(docUrl, docProps);
                    list = docManager.processRequirementsTable(docProps, elementName);
                    for (Requirement requirement : list.getRequirements()) {
                        db.createRequirment(docProps, requirement);
                    }
            }

        }
        docManager.closeDoc();
    }

    private RequirementDb extractRequirementsFromTree() {
        RequirementTree tree = new RequirementTree(requirementsDirectory);
        LibreDocManager docManager = new LibreDocManager(jiraProject);
        RequirementDb db = new RequirementDb(dbName);

        // Initialises new requirements db - deletes old one, creates new one, loads schema
        db.createDb();

        List<File> requirements = tree.getRequirements();

        for (File doc : requirements) {
            extractRequirementsFromDoc(docManager, db, doc);
        }

        docManager.close();
        return db;
    }

    public void run() {

        RequirementDb db = extractRequirementsFromTree();

        JiraConnector jira = new JiraConnector(new JiraConfig(jiraUrl, jiraUser, jiraToken));
        for (String key : db.getDocIssues()) {
            JiraIssue issue = jira.findIssue(key);
            if (issue == null) {
                fail("Unable to find issue " + key + " in jira");
            }
            log.info("Found issue: " + issue.getKey());
        }

        RequirementCursor reqs = db.getRequirementsWithIssues();
        while(reqs.next()) {
            Requirement req = reqs.getRequirement();
            log.info("Found requirement with issue " + req.getIssue());
        }
        reqs.close();

        reqs = db.getRequirementsNeedingIssues();
        log.info("Following requirements need issues");
        while(reqs.next()) {
            Requirement req = reqs.getRequirement();
            log.info("Requirement doc:tag: " + req.getDocTitle() + ":" + req.getTag());
        }

        // Need to exit as we potentially have threads running a connection to open office
        System.exit(0);
    }
}
