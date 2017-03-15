package org.nines;

import net.middell.XML;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.middell.XML.children;
import static net.middell.XML.elements;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class Migration {

    private static final String XML_NS = "http://www.collex.org/migration#";

    private static final Logger LOG = Logging.forClass(Migration.class);

    private final Rule[] rules;

    public Migration(Rule[] rules) {
        this.rules = rules;
    }

    @Override
    public String toString() {
        return Stream.of(rules)
                .map(Rule::toString)
                .collect(Collectors.joining("\n"));
    }

    public static Migration parse(File file) throws IOException, SAXException {
        return parse(XML.newDocumentBuilder().parse(file));
    }

    public static Migration parse(Document xml) {
        final Element rootElement = xml.getDocumentElement();
        if (!isMigrationElement(rootElement, "migration")) {
            throw new IllegalArgumentException(XML.toString(xml));
        }

        final List<Rule> rules = new LinkedList<>();
        for (Element el : elements(children(rootElement))) {
            if (isMigrationElement(el, "rule")) {
                rules.add(Rule.parse(el));
            }
        }
        return new Migration(rules.toArray(new Rule[rules.size()]));
    }

    public static boolean isMigrationElement(Element element, String localName) {
        return XML_NS.equals(element.getNamespaceURI()) && localName.equals(element.getLocalName());
    }

    public boolean apply(Model model) {
        boolean modelChanged = false;
        for (final ResIterator it = model.listSubjects(); it.hasNext(); ) {
            final Resource subject = it.next();
            for (Rule rule : rules) {
                final boolean applied = rule.apply(subject);
                if (applied) {
                    LOG.fine(() -> String.format("! %s (%s)", subject, rule));
                    modelChanged = true;
                } else {
                    LOG.finer(() -> String.format(". %s (%s)", subject, rule));
                }
            }
        }
        return modelChanged;
    }

    public boolean apply(File rdf) throws IOException {
        final Model model = ModelFactory.createDefaultModel().read(rdf.toURI().toURL().toString());
        if (apply(model)) {
            try (OutputStream stream = Files.newOutputStream(rdf.toPath())) {
                RDFDataMgr.write(stream, model, RDFFormat.RDFXML_PLAIN);
            }
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        Logging.configure();
        final Logger log = Logging.forClass(Migration.class);


        final File migrationFile = Stream.of(args).map(File::new)
                .filter(File::isFile)
                .findFirst().orElseThrow(IllegalArgumentException::new);

        final File[] repositories = Stream.of(args)
                .map(File::new)
                .filter(File::isDirectory)
                .map(dir -> dir.listFiles(f -> f.isDirectory() && f.getName().startsWith("arc_rdf")))
                .findFirst().orElseThrow(IllegalArgumentException::new);

        final long start = System.currentTimeMillis();

        final Migration migration = Migration.parse(migrationFile);
        log.fine(() -> String.format("%s:\n%s", migrationFile, migration));

        for (File repository : repositories) {
            final Path dotGit = repository.toPath().resolve(".git");

            final File[] rdfFiles = Files.walk(repository.toPath())
                    .filter(p -> !p.startsWith(dotGit))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> f.getName().toLowerCase().endsWith(".rdf"))
                    .toArray(File[]::new);

            for (File rdfFile : rdfFiles) {
                if (migration.apply(rdfFile)) {
                    log.info(() -> String.format("! %s", rdfFile.getAbsolutePath()));
                }
            }
        }

        final long end = System.currentTimeMillis();

        log.info(() -> String.format(". %s", Duration.ofMillis(end - start)));
    }
}
