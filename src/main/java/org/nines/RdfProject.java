/*
 * Copyright © 2017 The Advanced Research Consortium - ARC (http://idhmcmain.tamu.edu/arcgrant/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nines;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A project containing ARC RDF/XML source material as represented by a checked-out Git repository.
 */
public class RdfProject {

    public final Git git;
    public final Path dotGit;

    public RdfProject(Git git) {
        this.git = git;
        this.dotGit = git.repository.toPath().resolve(".git");
    }

    public static RdfProject checkout(File workspace, Arc.GitLabProject gitLabProject) {
        return new RdfProject(Git.clone(workspace, gitLabProject));
    }

    /**
     * Delegates to {@link Git#checkoutBranch(String, boolean) the git accessor method}.
     */
    public RdfProject withBranch(String branch, boolean create) {
        git.checkoutBranch(branch, create);
        return this;

    }

    /**
     * Generates all RDF/XML files contained in the Git repository of this project.
     *
     * @return all RDF/XML files (those having the extension <code>.rdf</code> or <code>.xml</code>
     */
    public Stream<File> rdfFiles() {
        try {
            return Files.walk(git.repository.toPath())
                    .filter(p -> !p.startsWith(dotGit))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> RDF_FILE_EXTENSIONS.matcher(f.getName().toLowerCase()).find());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new Git commit if there have been changes to the working tree.
     *
     * @param message the commit message
     * @return <code>true</code> if a commit has been created
     */
    public boolean commitIfChanged(String message) {
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

    private static final Pattern RDF_FILE_EXTENSIONS = Pattern.compile("\\.(rdf)|(xml)$");
}
