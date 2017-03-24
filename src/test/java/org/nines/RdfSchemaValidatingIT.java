package org.nines;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class RdfSchemaValidatingIT extends RdfValidatingIT {
    @Override
    protected Path csvPath() {
        return Paths.get("arc-schema-errors.csv");
    }

    @Override
    protected String[] csvReportHeader() {
        return new String[] { "File", "Resource", "Property", "Value", "Error", "Link"};
    }

    @Override
    protected Stream<String[]> csvReport(Workspace workspace, RdfProject rdfProject, File rdfFile) {
        final List<String[]> errors = new ArrayList<>();
        RdfXmlDocument.model(rdfFile).listSubjects().filterDrop(RDFNode::isAnon).forEachRemaining(subject -> {
            for (Schema.Error error : Schema.validate(rdfProject, subject)) {
                if (error.value == null || error.property == null || !ERROR_FOCUS.contains(error.property)) {
                    continue;
                }

                errors.add(new String[] {
                    workspacePath(workspace, rdfFile),
                    error.resource.toString(),
                    Optional.ofNullable(error.property).map(Property::toString).orElse(""),
                    Optional.ofNullable(error.value).map(RDFNode::toString).orElse(""),
                    error.message,
                    gitLabUrl(rdfProject, rdfFile, "master", -1)

                });

            }
        });
        return errors.stream();
    }

    @Override
    protected boolean projectFilter(Arc.GitLabProject project) {
        return PROJECT_FOCUS.contains(project.name);
    }

    private static final Set<String> PROJECT_FOCUS = new HashSet<>(Arrays.asList("arc_rdf_Pfaffs"/*, "arc_rdf_pages_ecco"*/));

    private static final Set<Property> ERROR_FOCUS = new HashSet<>(Arrays.asList(
        Collex.genre, Collex.discipline, DC.type
    ));
}
