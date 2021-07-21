package uk.co.slysoftware.librerequirements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static uk.co.slysoftware.librerequirements.ErrorHandler.failWithTrace;
import static uk.co.slysoftware.librerequirements.ErrorHandler.fail;

public class JiraConnector {

    private static Logger log = LoggerFactory.getLogger(JiraConnector.class);

    private JiraConfig config;

    public JiraConnector(JiraConfig config) {
        this.config = config;
    }

    private String getAuthHeaderContent() {
        String token = config.getUser() + ":" + config.getApiToken();
        log.info("Unencoded token = " + token);
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private HttpURLConnection getConnection(String request) throws IOException {
        HttpURLConnection.setFollowRedirects(false);

        URL endpoint = null;
        try {
            String endpointString = config.getBaseEndpoint() + request;
            log.info("Endpoint is: " + endpointString);
            endpoint = new URL(endpointString);
        } catch (MalformedURLException e) {
            failWithTrace(e);
        }

        HttpURLConnection con = (HttpURLConnection) endpoint.openConnection();
        String authHeader = getAuthHeaderContent();
        log.info("Auth header: " + authHeader);
        con.setRequestProperty("Authorization", authHeader);
        con.setRequestProperty("Content-Type", "application/json");
        con.setUseCaches(false);

        return con;
    }

    public JiraIssue findIssue(String key) {

        JiraIssue issue = null;

        try {
            HttpURLConnection con = getConnection("/rest/api/2/issue/" + key  +
                    "?fields=id,key," +
                    config.getTagFieldName() +
                    "," + config.getDescriptionFieldName() + ",parent");

            con.setRequestMethod("GET");

            con.connect();

            log.info("Response Code: " + con.getResponseCode());

            if (con.getResponseCode() == 404) {
                fail("Issue not found");
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer buffer = new StringBuffer();

            String line;
            while((line = in.readLine()) != null) {
                log.info(line);
            }

            issue = new JiraIssue(key);

        } catch (IOException e) {
            failWithTrace(e);
        }

        return issue;
    }
}
