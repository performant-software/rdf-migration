package org.nines;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

public class RdfXmlParsingIT extends RdfValidatingIT {

    private static final SAXParserFactory saxParserFactory;

    static {
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
    }

    @Override
    protected Path csvPath() {
        return Paths.get("xml-errors.csv");
    }

    @Override
    protected String[] csvReportHeader() {
        return new String[] { "File", "Line/Column", "Error", "Link"};
    }

    @Override
    protected Stream<String[]> csvReport(Workspace workspace, RdfProject rdfProject, File rdfFile) {
        try {
            saxParserFactory.newSAXParser().parse(rdfFile, new DefaultHandler2());
            return Stream.empty();
        } catch (SAXParseException e) {
            return Stream.<String[]>of(new String[]{
                workspacePath(workspace, rdfFile),
                String.format("[%s:%s]", e.getLineNumber(), e.getColumnNumber()),
                e.getMessage(),
                gitLabUrl(rdfProject, rdfFile, "master", e.getLineNumber())
            });
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean projectFilter(Arc.GitLabProject project) {
        return PROJECT_FOCUS.contains(project.name);
    }

    private static final Set<String> PROJECT_FOCUS = new HashSet<>(Arrays.asList("arc_rdf_bierce"/*, "arc_rdf_pages_ecco"*/));

}
