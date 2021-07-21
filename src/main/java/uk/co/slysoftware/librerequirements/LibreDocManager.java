package uk.co.slysoftware.librerequirements;

import com.sun.star.beans.*;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.io.IOException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.table.CellContentType;
import com.sun.star.table.XCell;
import com.sun.star.text.*;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.co.slysoftware.librerequirements.ErrorHandler.fail;
import static uk.co.slysoftware.librerequirements.ErrorHandler.failWithTrace;

public class LibreDocManager {

    private  static Logger log = LoggerFactory.getLogger(LibreDocManager.class);

    private XDesktop xDesktop = null;
    private XComponentLoader xComponentLoader = null;
    private XComponent currentComp = null;

    private static String[] colHeaders = {
            "Name", "Description"
    };

    private static String[] cellColNames = {
            "A", "B"
    };

    private static Pattern issuePattern = null;

    public  LibreDocManager(String jiraProject) {
        this.xDesktop = startLibreOffice();
        this.xComponentLoader = UnoRuntime.queryInterface(com.sun.star.frame.XComponentLoader.class, xDesktop);
        this.issuePattern = Pattern.compile(jiraProject + "-\\d+");
    }

    private XDesktop startLibreOffice() {
        // Initialise
        XDesktop xDesktop = null;
        XMultiComponentFactory MCF = null;

        try {
            XComponentContext xContext = null;
            xContext = com.sun.star.comp.helper.Bootstrap.bootstrap();
            MCF = xContext.getServiceManager();
            if (MCF != null) {
                log.debug("Connected to running office...");

                Object oDesktop = MCF.createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
                xDesktop = UnoRuntime.queryInterface(com.sun.star.frame.XDesktop.class, oDesktop);
            } else {
                fail("Failed to create a desktop no connection, no remote office servicemanager");
            }
        } catch (Exception e) {
            failWithTrace(e);
        } catch (BootstrapException e) {
            failWithTrace(e);
        }
        return xDesktop;
    }

    private XDocumentProperties getDocumentProperties(XComponent xComp) {
        XDocumentPropertiesSupplier xDocumentPropertiesSupplier = UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, xComp);
        return xDocumentPropertiesSupplier.getDocumentProperties();
    }

    private DocProperties getDocumentProperties(XDocumentProperties xDocumentProperties) throws UnknownPropertyException, WrappedTargetException {
        DocProperties doc = new  DocProperties();

        doc.setSubject(xDocumentProperties.getSubject());
        doc.setTitle(xDocumentProperties.getTitle());

        XPropertyContainer xPropertyContainer = xDocumentProperties.getUserDefinedProperties();
        XPropertySet xPropertySet =  UnoRuntime.queryInterface(XPropertySet.class, xPropertyContainer);
        XPropertySetInfo xPropertySetInfo = xPropertySet.getPropertySetInfo();
        for (Property property : xPropertySetInfo.getProperties() ) {
            log.info(String.format("Found property %s with value %s", property.Name, xPropertySet.getPropertyValue(property.Name)).toString());
            switch ((property.Name)) {
                case "Version":
                    doc.setVersion(xPropertySet.getPropertyValue(property.Name).toString());
                    break;
                case "Author":
                    doc.setAuthor(xPropertySet.getPropertyValue(property.Name).toString());
                    break;
                case "Status":
                    doc.setStatus(xPropertySet.getPropertyValue(property.Name).toString());
                    break;
                case "Epic":
                    doc.setEpic(xPropertySet.getPropertyValue(property.Name).toString());
            }
        }
        return doc;
    }

    public DocProperties openDoc(String docUrl) {

        DocProperties docProps = null;

        try {
            log.debug("Opening " + docUrl);
            currentComp = xComponentLoader.loadComponentFromURL(docUrl, "_blank", 0, null);
            XDocumentProperties xDocumentProperties = getDocumentProperties(currentComp);
            docProps = getDocumentProperties(xDocumentProperties);

        }catch (IOException e) {
            failWithTrace(e);
        } catch (WrappedTargetException e) {
            failWithTrace(e);
        } catch (UnknownPropertyException e) {
            failWithTrace(e);
        } catch (NullPointerException e) {
            failWithTrace(e);
        }
        return docProps;
    }

    private XNameAccess getDocumentTables(XTextDocument xTextDocument) {
        XTextTablesSupplier xTextTablesSupplier = UnoRuntime.queryInterface(XTextTablesSupplier.class, xTextDocument);
        return xTextTablesSupplier.getTextTables();
    }

    public String[] getTables() {
        XTextDocument xTextDocument = UnoRuntime.queryInterface(XTextDocument.class, currentComp);
        XNameAccess xNamedTables = getDocumentTables(xTextDocument);
        return xNamedTables.getElementNames();
    }

    public void closeDoc() {
        if (currentComp == null) fail("Trying to close doc when none open");

        currentComp.dispose();
    }

    public void close() {
        xDesktop.terminate();
    }



    private void parseName(RequirementList list, short id, XCell xCell) throws
            UnknownPropertyException, WrappedTargetException{

        XText xCellText = UnoRuntime.queryInterface(XText.class, xCell);

        String txt = xCellText.getString();

        String url = null;
        String name = txt;

        Matcher matcher = issuePattern.matcher(txt);
        while(matcher.find()) {
            short startJiraText = (short) matcher.start();
            short lenJiraText = (short) (matcher.end() - matcher.start());
            XTextCursor xTextCursor = xCellText.createTextCursor();
            xTextCursor.goRight(startJiraText, false);
            xTextCursor.goRight(lenJiraText, true);
            XPropertySet xCursorProps = UnoRuntime.queryInterface(XPropertySet.class, xTextCursor);
            url = (String) xCursorProps.getPropertyValue("HyperLinkURL");
            if (url != null) {
                name = txt.substring(0, startJiraText-1);
                list.setIssue(id, xTextCursor.getString());
                list.setIssueUrl(id, url);
                break;
            }
        }

        list.setName(id, name);
    }

    private void parseDescription(RequirementList list, short id, XCell xCell) {
        XText xCellText = UnoRuntime.queryInterface(XText.class, xCell);
        String txt = xCellText.getString();

        list.setDescription(id, txt);
    }

    private void validateHeaders(XTextTable xTextTable) {

        for (int i = 0; i < colHeaders.length; i++) {
            XCell xCell = xTextTable.getCellByName(cellColNames[i] + "1");
            if (xCell == null || xCell.getType() != CellContentType.TEXT) {
                fail(String.format("Column %s is null or has a none text type", cellColNames[i]));
            }
            XText xCellText = UnoRuntime.queryInterface(XText.class, xCell);
            String cellText = xCellText.getString();
            if (cellText.compareTo(colHeaders[i]) != 0) {
                fail(String.format("Reference table col %s had %s rather than expected value %s",
                        cellColNames[i], cellText, colHeaders[i]));
            }
        }
    }

    public RequirementList processRequirementsTable(DocProperties doc, String name ) {

        RequirementList list = new RequirementList();

        try {

            XTextDocument xTextDocument = UnoRuntime.queryInterface(XTextDocument.class, currentComp);
            XNameAccess xNamedTables = getDocumentTables(xTextDocument);
            Object table = xNamedTables.getByName(name);
            XTextTable xTextTable = UnoRuntime.queryInterface(XTextTable.class, table);

            validateHeaders(xTextTable);

            String[] cellNames = xTextTable.getCellNames();


            Set<String> headerCellNames = new HashSet<>();
            for (String prefix : cellColNames) {
                headerCellNames.add(prefix + "1");
            }

            for (String cellName : cellNames) {

                if (headerCellNames.contains(cellName)) {
                    continue;
                }

                XCell xCell = xTextTable.getCellByName(cellName);
                if (xCell.getType() != CellContentType.TEXT) {
                    fail(String.format("Cell %s is not a text type", cellName));
                }

                String row = cellName.substring(1);
                if (row == null || row.isEmpty()) {
                    fail(String.format("Failed to parse cell name ", cellName));
                }
                short id = Short.valueOf(row);
                list.setDocTitle(id, doc.getTitle());

                String col = cellName.substring(0, 1);
                switch (col) {
                    case "A":
                        parseName(list, id, xCell);
                        break;
                    case "B":
                        parseDescription(list, id, xCell);
                        break;
                    default:
                        fail(String.format("Failed to decode cell name %s", cellName));
                }
            }

        } catch (WrappedTargetException e) {
            failWithTrace(e);
        } catch (NoSuchElementException e) {
            failWithTrace(e);
        } catch (UnknownPropertyException e) {
            failWithTrace(e);
        }
        return list;
    }
}
