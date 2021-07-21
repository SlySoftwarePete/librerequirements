package uk.co.slysoftware.librerequirements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;
import static uk.co.slysoftware.librerequirements.ErrorHandler.fail;
import static uk.co.slysoftware.librerequirements.ErrorHandler.failWithTrace;

public class RequirementTree {

    private static Logger log = LoggerFactory.getLogger(RequirementTree.class);

    private String requirementsDirectory;

    public RequirementTree(String requirementsDirectory) {
        this.requirementsDirectory = requirementsDirectory;
    }

    private File checkRequirementsDirectory() {
        log.info("Processing requirements directory: " + requirementsDirectory);
        File root = new File(requirementsDirectory);
        if (!root.isDirectory()) {
            fail(String.format("The location %s is not a directory", requirementsDirectory));
        }
        return root;
    }

    private void findFiles(File root, List<File> requirements) {

        for (File doc : root.listFiles(new RequirementTree.FodtFileFilter()) ) {
            if (doc.isDirectory()) {
                findFiles(doc, requirements);
                continue;
            }
            requirements.add(doc);
        }
    }

    private class FodtFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            if (!pathname.canRead()) {
                fail(String.format("Cannot read file %s, aborting", pathname.getPath()));
            }
            if (pathname.isDirectory()) {
                return true;
            }
            if (!pathname.getPath().endsWith(".fodt")) {
                return false;
            }
            return true;
        }
    };

    public List<File> getRequirements() {
        List<File> requirements = new LinkedList<>();
        findFiles(checkRequirementsDirectory(), requirements);
        return requirements;
    }
}
