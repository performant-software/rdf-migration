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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.nines.Util.join;

/**
 * Access to a git repository via the command line tool.
 */
public class Git {

    private static final Logger LOG = Logging.forClass(Git.class);

    public final File repository;

    public Git(File repository) {
        this.repository = repository;
    }

    public static Git clone(File workspace, Arc.GitLabProject gitLabProject) throws IOException {
        execute(workspace, "git", "clone", gitLabProject.gitUrl());
        return new Git(new File(workspace, gitLabProject.name));
    }

    public String checkoutNewBranch(String name) throws IOException {
        return execute("git", "checkout", "-t", "-b", name);
    }
    
    public String status() throws IOException {
        return execute("git", "status", "--porcelain");
    }

    public String commit(String commitMessage) throws IOException {
        return execute("git", "commit", "-a", "-m", commitMessage);
    }

    public String push() throws IOException {
        return execute("git", "push", "-u", "origin");
    }

    public String execute(String... command) throws IOException {
        return execute(repository, command);
    }

    private static String execute(File directory, String... command) throws IOException {
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
    }
}
