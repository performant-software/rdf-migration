package org.nines;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RdfSchemaValidatingIT extends RdfValidatingIT {
    @Override
    protected Path csvPath() {
        return Paths.get("arc-schema-errors.csv");
    }

    @Override
    protected String[] csvReportHeader() {
        return new String[] { "File", "Resource", "Error", "Link"};
    }

    @Override
    protected Stream<String[]> csvReport(Workspace workspace, RdfProject rdfProject, File rdfFile) {
        final List<String[]> errors = new ArrayList<>();
        RdfXmlDocument.model(rdfFile).listSubjects().forEachRemaining(subject -> {
            Schema.validate(rdfProject, subject).forEach(errorMessage -> {
                if (!ERROR_FOCUS.matcher(errorMessage).find()) {
                    return;
                }

                errors.add(new String[] {
                    workspacePath(workspace, rdfFile),
                    subject.toString(),
                    errorMessage,
                    gitLabUrl(rdfProject, rdfFile, "master", -1)

                });
            });
        });
        return errors.stream();
    }

    @Override
    protected boolean projectFilter(Arc.GitLabProject project) {
        return PROJECT_FOCUS.contains(project.name);
    }

    private static final Set<String> PROJECT_FOCUS = new HashSet<>(Arrays.asList("arc_rdf_Pfaffs"/*, "arc_rdf_pages_ecco"*/));

    private static final Pattern ERROR_FOCUS = Pattern.compile("((genre)|(discipline)|(type)) not approved", Pattern.CASE_INSENSITIVE);
}
