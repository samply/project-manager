package de.samply.utils;

import de.samply.app.ProjectManagerConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class ProjectVersion {

    private final static String POM_FILE = "pom.xml";

    public static String getProjectVersion() {
        try {
            return getProjectVersion_WithoutManagementException();
        } catch (IOException | XmlPullParserException e) {
            log.error("Error getting project version: " + ExceptionUtils.getStackTrace(e));
            return ProjectManagerConst.APP_NAME;
        }
    }

    private static String getProjectVersion_WithoutManagementException()
            throws IOException, XmlPullParserException {

        if (Files.exists(Paths.get(POM_FILE))) {
            return getProjectVersionFromPomFile();
        } else {
            return getProjectVersionFromManifest();
        }

    }

    private static String getProjectVersionFromPomFile() throws IOException, XmlPullParserException {

        MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
        Model model = mavenXpp3Reader.read(new FileReader(POM_FILE));

        return fetchVersion(model);

    }

    private static String getProjectVersionFromManifest() {

        String implementationVersion = ProjectVersion.class.getPackage().getImplementationVersion();
        String implementationTitle = ProjectVersion.class.getPackage().getImplementationTitle();

        return createVersionToDisplay(implementationTitle, implementationVersion);

    }

    private static String fetchVersion(Model model) {
        return createVersionToDisplay(model.getArtifactId(), model.getVersion());
    }

    public static String createVersionToDisplay(String artifactId, String version) {
        return artifactId + ':' + version;
    }

}
