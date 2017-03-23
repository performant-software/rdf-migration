package org.nines;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Filesystem directory serving as a workspace with Git repositories.
 */
public class Workspace {

    public final File directory;
    private final Arc arc;

    public Workspace(Arc arc, File directory) {
        this.directory = Util.existingDirectory(directory);
        this.arc = arc;
    }

    public Workspace(Arc arc) throws IOException {
        this(arc, findWorkspace());
    }

    public Stream<RdfProject> projects() throws IOException {
        return Stream.of(arc.rdfRepositories())
            .map(gitLabProject -> RdfProject.checkout(directory, gitLabProject));
    }

    public Path relativize(Path path) {
        return directory.toPath().relativize(path);
    }

    private static File findWorkspace() {
        final Stream<String> configSources = Stream.of(
            System.getenv("ARC_RDF_WORKSPACE"),
            System.getProperty("arc.rdf.workspace")
        );

        return configSources
            .filter(s -> s != null)
            .map(File::new).filter(File::isDirectory)
            .findFirst().orElseThrow(() -> new IllegalArgumentException(
                "$ARC_RDF_WORKSPACE/ -Darc.rdf.workspace"
            ));
    }

}
