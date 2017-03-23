package org.nines;

import au.com.bytecode.opencsv.CSVWriter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public abstract class RdfValidatingIT extends LoggingIT {

    private final AtomicLong fileCounter = new AtomicLong();

    @Test
    public void report() throws IOException {
        final Workspace workspace = new Workspace(new Arc());

        final Stream<String[]> parseErrors = workspace.projects()
            .filter(rdfProject -> projectFilter(rdfProject.git.gitLabProject))
            .parallel()
            .flatMap(rdfProject -> rdfProject.rdfFiles().parallel().flatMap(rdfFile -> {
                progress(rdfFile);
                return csvReport(workspace, rdfProject, rdfFile);
            }))
            .limit(csvErrorLimit());

        try (CSVWriter csv = new CSVWriter(Files.newBufferedWriter(csvPath(), StandardCharsets.UTF_8))) {
            Stream.concat(Stream.<String[]>of(csvReportHeader()), parseErrors).sequential().forEach(csv::writeNext);
        }
    }

    @Before
    public void resetProgress() {
        fileCounter.set(0);
    }
    private void progress(File file) {
        final long count = fileCounter.incrementAndGet();
        if (count % 1000L == 0L) {
            logger.fine(() -> String.format("[%08d] %s", count, file));
        }
    }


    protected boolean projectFilter(Arc.GitLabProject project) {
        return true;
    }

    protected abstract Path csvPath();

    protected long csvErrorLimit() {
        return Long.MAX_VALUE;
    }

    protected abstract String[] csvReportHeader();

    protected abstract Stream<String[]> csvReport(Workspace workspace, RdfProject rdfProject, File rdfFile);

    protected static String workspacePath(Workspace workspace, File rdfFile) {
        return workspace.relativize(rdfFile.toPath()).toString();
    }

    protected static String gitLabUrl(RdfProject rdfProject, File rdfFile, String branch, int lineNumber) {
        final String path = rdfProject.git.relativize(rdfFile.toPath()).toString();
        return lineNumber == -1
            ? rdfProject.git.gitLabProject.url(branch, path).toString()
            : rdfProject.git.gitLabProject.url(branch, path, lineNumber).toString();
    }
}
