package uk.co.slysoftware.librerequirements;

import liquibase.exception.LiquibaseException;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import static uk.co.slysoftware.librerequirements.ErrorHandler.failWithTrace;

public class RequirementDb {

    private PreparedStatement requirementsInsert = null;

    private PreparedStatement documentInsert = null;

    private String dbName;

    private Connection con;

    public RequirementDb(String dbName) {

        this.dbName = dbName;
    }

    private void prepareStatements() {

        try {
            requirementsInsert = con.prepareStatement(
                    "INSERT INTO requirement ( doc_title, tag, description, issue, issueUrl ) values (?, ?, ?, ?, ?)");
        } catch (SQLException e) {
            failWithTrace(e);
        }

        try {
            documentInsert = con.prepareStatement(
                    "INSERT INTO document ( file_url, title, subject, status, version, author, issue ) values (?, ?, ?, ?, ?, ?, ?)");
        } catch (SQLException e) {
            failWithTrace(e);
        }
    }

    public  void createDb() {
        File db = new File(dbName);
        DbConstructor constructor = new DbConstructor(db);
        try {
            con = constructor.load(true);
        } catch (SQLException e) {
            failWithTrace(e);
        } catch (LiquibaseException e) {
            failWithTrace(e);
        }
        prepareStatements();
    }

    public void createDoc(String docUrl, DocProperties docProps) {
        try {
            documentInsert.setString(1, docUrl);
            documentInsert.setString(2, docProps.getTitle());
            documentInsert.setString(3, docProps.getSubject());
            documentInsert.setString(4, docProps.getStatus());
            documentInsert.setString(5, docProps.getVersion());
            documentInsert.setString(6, docProps.getAuthor());
            documentInsert.setString(7, docProps.getEpic());
            documentInsert.execute();
        }  catch (SQLException e) {
            failWithTrace(e);
        }
    }

    public void createRequirment(DocProperties docProps, Requirement requirement) {
        try {
            requirementsInsert.setString(1, requirement.getDocTitle());
            requirementsInsert.setString(2, requirement.getTag());
            requirementsInsert.setString(3, requirement.getDescription());
            requirementsInsert.setString(4, requirement.getIssue());
            requirementsInsert.setString(5, requirement.getIssueUrl());
            requirementsInsert.execute();
        } catch (SQLException e) {
            failWithTrace(e
            );
        }
    }

    public List<String> getDocIssues() {
        List<String> docIssues = new LinkedList<>();
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT issue FROM document WHERE issue NOT NULL");
            ResultSet results = stmt.executeQuery();
            while( results.next()) {
                docIssues.add(results.getString(1));
            }
        } catch (SQLException e) {
            failWithTrace(e);
        }
        return docIssues;
    }

    public RequirementCursor getRequirementsNeedingIssues() {
        RequirementCursor cursor = null;
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT doc_title, tag, description, issue, issueUrl FROM requirement WHERE issue IS NULL");
            cursor = new RequirementCursor(stmt.executeQuery());
        } catch (SQLException e) {
            failWithTrace(e);
        }
        return cursor;
    }

    public RequirementCursor getRequirementsWithIssues() {
        RequirementCursor cursor = null;
        try {
            PreparedStatement stmt = con.prepareStatement("SELECT doc_title, tag, description, issue, issueUrl FROM requirement WHERE issue NOT NULL");
            cursor = new RequirementCursor(stmt.executeQuery());
        } catch (SQLException e) {
            failWithTrace(e);
        }
        return cursor;
    }
}
