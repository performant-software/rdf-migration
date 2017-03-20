package org.nines;

import net.middell.XML;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.JenaException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.transform.TransformerException;

import static net.middell.XML.children;
import static net.middell.XML.elements;
import static org.nines.Util.join;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class Migration {

    private static final String XML_NS = "http://www.collex.org/migration#";

    private static final Logger LOG = Logging.forClass(Migration.class);

    private final String title;
    private final Rule[] rules;

    public Migration(String title, Rule[] rules) {
        this.title = title;
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

        String title = "";
        final List<Rule> rules = new LinkedList<>();
        for (Element el : elements(children(rootElement))) {
            if (isMigrationElement(el, "title")) {
                title = el.getTextContent().trim();
            }
            if (isMigrationElement(el, "rule")) {
                rules.add(Rule.parse(el));
            }
        }
        if (title.isEmpty()) {
            throw new IllegalArgumentException(XML.toString(xml));
        }

        return new Migration(title, rules.toArray(new Rule[rules.size()]));
    }

    public static boolean isMigrationElement(Element element, String localName) {
        return XML_NS.equals(element.getNamespaceURI()) && localName.equals(element.getLocalName());
    }

    public boolean apply(Model model, RdfXmlDocument xml) {
        boolean modelChanged = false;
        for (final ResIterator it = model.listSubjects(); it.hasNext(); ) {
            final Resource subject = it.next();
            for (Rule rule : rules) {
                final boolean applied = rule.apply(subject, xml);
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

    public boolean apply(File rdf) throws IOException, SAXException, TransformerException, JenaException {
        final RdfXmlDocument xml = new RdfXmlDocument(rdf);
        final Model model = ModelFactory.createDefaultModel().read(rdf.toURI().toURL().toString());
        if (apply(model, xml)) {
            xml.write(rdf);
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        Logging.configure();
        final Logger log = Logging.forClass(Migration.class);

        final long start = System.currentTimeMillis();

        final File migrationFile = Stream.of(args).map(File::new)
                .filter(File::isFile)
                .findFirst().orElseThrow(IllegalArgumentException::new);

        final File workspace = Files.createTempDirectory(Migration.class.getName()).toFile();

        final Migration migration = Migration.parse(migrationFile);
        log.fine(() -> String.format("%s:\n%s", migrationFile, migration));

        final String featureBranchName = String.format(
                "feature/rdf-migration-%s",
                DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(LocalDateTime.now(ZoneId.of("EST", ZoneId.SHORT_IDS)))
        );

        int skip = 1;
        int limit = 1;
        for (Arc.GitLabProject gitLabProject : new Arc().rdfRepositories()) {
            if (skip-- > 0) {
                continue;
            }
            if (limit-- == 0) {
                break;
            }
            try (RdfProject rdfProject = RdfProject.checkout(workspace, gitLabProject).withWorkingBranch(featureBranchName)) {
                final File[] rdfFiles = rdfProject.rdfFiles();
                for (File rdfFile : rdfFiles) {
                    RdfXmlDocument.format(rdfFile);
                }

                rdfProject.commitIfChanged(join(" | ", migration.title, "RDF/XML formatting"));
                
                for (File rdfFile : rdfFiles) {
                    try {
                        if (migration.apply(rdfFile)) {
                            log.fine(() -> String.format("! %s", rdfFile.getAbsolutePath()));
                        }
                    } catch (JenaException e) {
                        log.log(Level.WARNING, e, rdfFile::toString);
                    }
                }

                if (rdfProject.commitIfChanged(join(" | ", migration.title, "RDF migration"))) {
                    rdfProject.push();
                    log.info(() -> String.format("! %s", gitLabProject));
                }
            }
        }

        workspace.delete();

        final long end = System.currentTimeMillis();

        log.info(() -> String.format(". %s", Duration.ofMillis(end - start)));
    }
}
