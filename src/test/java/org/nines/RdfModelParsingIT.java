package org.nines;

import org.apache.jena.rdfxml.xmlinput.ARP;
import org.apache.jena.rdfxml.xmlinput.ParseException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RdfModelParsingIT extends RdfValidatingIT {

    @Override
    protected Path csvPath() {
        return Paths.get("rdf-errors.csv");
    }


    @Override
    protected String[] csvReportHeader() {
        return new String[] { "File", "Line/Column", "Error", "Link"};
    }

    @Override
    protected Stream<String[]> csvReport(Workspace workspace, RdfProject rdfProject, File rdfFile) {
        final ARP arp = new ARP();
        final List<String[]> report = new ArrayList<>();
        arp.getHandlers().setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                handle(exception);
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                handle(exception);
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                handle(exception);
            }

            public void handle(SAXParseException e) {
                if (!(e instanceof ParseException)) {
                    return;
                }
                final ParseException pe = (ParseException) e;
                switch (pe.getErrorNumber()) {
                    case 107: // invalid URI
                    case 131: // UTF encoding
                        break;
                    default:
                        report.add(new String[]{
                            workspacePath(workspace, rdfFile),
                            String.format("[%s:%s]", pe.getLineNumber(), pe.getColumnNumber()),
                            format(e.getMessage()),
                            gitLabUrl(rdfProject, rdfFile, "master", e.getLineNumber())
                        });
                }
            }

            private String format(String exceptionMessage) {
                exceptionMessage = WHITESPACE_RUNS.matcher(exceptionMessage).replaceAll(" ");
                return exceptionMessage.length() > 200
                    ? String.format("%s...", exceptionMessage.substring(0, 197))
                    : exceptionMessage;
            }

        });

        try (InputStream rdfStream = Files.newInputStream(rdfFile.toPath())) {
            arp.load(rdfStream, rdfFile.toURI().toString());
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }

        return report.stream();
    }

    @Override
    protected boolean projectFilter(Arc.GitLabProject project) {
        return LARGE_PROJECTS.contains(project.name);
    }

    private static final Set<String> LARGE_PROJECTS = new HashSet<>(Arrays.asList(
        "arc_rdf_amdecj",
        "arc_rdf_amdeveryday",
        "arc_rdf_ECCO",
        "arc_rdf_eebo",
        "arc_rdf_pages_ecco",
        "arc_rdf_pages_eebo_prq"
    ));

    private static Pattern WHITESPACE_RUNS = Pattern.compile("\\s+");

}
