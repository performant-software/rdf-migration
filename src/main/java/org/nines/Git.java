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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static org.nines.Util.join;

/**
 * Access to a git repository via the command line tool.
 */
public class Git {

    private static final Logger LOG = Logging.forClass(Git.class);

    public final Arc.GitLabProject gitLabProject;
    public final File repository;

    public Git(Arc.GitLabProject gitLabProject, File repository) {
        this.gitLabProject = gitLabProject;
        this.repository = repository;
    }

    public Path relativize(Path path) {
        return repository.toPath().relativize(path);
    }

    /**
     * Clones a GitLab project to the given workspace directory, if the target directory does not
     * exist yet.
     *
     * @param workspace the workspace directory
     * @param gitLabProject the GitLab project to clone
     * @return an accessor for the cloned repository
     */
    public static Git clone(File workspace, Arc.GitLabProject gitLabProject) {
        final File directory = new File(workspace, gitLabProject.name);
        if (!directory.isDirectory()) {
            execute(workspace, "git", "clone", gitLabProject.gitUrl());
        }
        return new Git(gitLabProject, directory);
    }

    /**
     * Checks out a (remote) branch, optionally creating it.
     *
     * @param branch the branch to check out
     * @param create <code>true</code> if the branch should be created locally
     */
    public void checkoutBranch(String branch, boolean create) {
        final List<String> command = new LinkedList<>(Arrays.asList("git", "checkout"));
        if (create) {
            command.addAll(Arrays.asList("-t", "-b"));
        }
        command.add(branch);
        execute(command);
    }

    public String status() {
        return execute("git", "status", "--porcelain");
    }

    public String commit(String commitMessage) {
        return execute("git", "commit", "-a", "-m", commitMessage);
    }

    public String push() {
        return execute("git", "push", "-u", "origin");
    }

    @Override
    public String toString() {
        return gitLabProject.toString();
    }

    public String execute(List<String> command) {
        return execute(repository, command);
    }

    public String execute(String... command) {
        return execute(repository, command);
    }

    private static String execute(File directory, String... command) {
        return execute(directory, Arrays.asList(command));
    }

    private static String execute(File directory, List<String> command) {
        try {
            final Path outputLog = Files.createTempFile(Git.class.getName(), "-output.log");
            final Path errorLog = Files.createTempFile(Git.class.getName(), "-error.log");
            try {
                final int exitValue = new ProcessBuilder(command)
                        .directory(directory)
                        .redirectError(errorLog.toFile())
                        .redirectOutput(outputLog.toFile())
                        .start()
                        .waitFor();

                if (exitValue != 0) {
                    throw new RuntimeException(join("\n\n",
                            join(": ", directory.toString(), String.join(" ", command)),
                            String.format("Exited with value %d", exitValue),
                            join("\n", Files.readAllLines(errorLog, Charset.defaultCharset()))
                    ));
                }

                final String output = join("\n", Files.readAllLines(
                    outputLog,
                    Charset.defaultCharset()
                ));
                LOG.fine(() -> join("\n\n",
                        join(": ", directory.toString(), String.join(" ", command)),
                        output
                ));

                return output;
            } catch (InterruptedException e) {
                throw new IOException(e);
            } finally {
                Files.delete(errorLog);
                Files.delete(outputLog);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
