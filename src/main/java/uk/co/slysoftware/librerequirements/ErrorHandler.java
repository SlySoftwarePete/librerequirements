package uk.co.slysoftware.librerequirements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {

    private static Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    public static void fail(String msg) {
        log.error(msg);
        System.exit(1);
    }

    public static void failWithTrace(java.lang.Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }
}
