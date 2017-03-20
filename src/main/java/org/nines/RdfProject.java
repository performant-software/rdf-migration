package org.nines;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class RdfProject implements Closeable {

    private final Git git;
    private final Path dotGit;

    public RdfProject(Git git) {
        this.git = git;
        this.dotGit = git.repository.toPath().resolve(".git");
    }

    public static RdfProject checkout(File workspace, Arc.GitLabProject gitLabProject) throws IOException {
        return new RdfProject(Git.shallowClone(workspace, gitLabProject));
    }

    public RdfProject withWorkingBranch(String name) throws IOException {
        git.checkoutNewBranch(name);
        return this;
    }
    public File[] rdfFiles() throws IOException {
        return Files.walk(git.repository.toPath())
                .filter(p -> !p.startsWith(dotGit))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(f -> f.getName().toLowerCase().endsWith(".rdf"))
                .toArray(File[]::new);
    }

    public boolean commitIfChanged(String message) throws IOException {
        if (git.status().isEmpty()) {
            return false;
        }
        git.commit(message);
        return true;
    }

    public String push() throws IOException {
        return git.push();
    }

    @Override
    public String toString() {
        return git.toString();
    }

    @Override
    public void close() throws IOException {
        //Util.deleteRecursively(repository);
    }
}
