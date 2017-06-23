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

import net.middell.XML;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.JenaException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.transform.TransformerException;

import static net.middell.XML.children;
import static net.middell.XML.elements;
import static org.nines.Util.join;

/**
 * A RDF migration rule set.
 */
public class Migration {

    private static final Logger LOG = Logging.forClass(Migration.class);

    private static final String XML_NS = "http://www.collex.org/migration#";

    public static final String RULES_RESOURCE = "/migration-rules/modnets-refactoring.xml";

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

    /**
     * Parses a migration rule set in a (XML) file.
     *
     * @see Migration#parse(Document)
     */
    public static Migration parse(File file) throws IOException, SAXException {
        return parse(XML.newDocumentBuilder().parse(file));
    }

    /**
     * Parses a migration rule set expressed in a XML document.
     *
     * @param xml the XML document to parse
     * @return the corresponding rule set
     * @throws IllegalArgumentException in case of missing parts/ invalid syntax
     */
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

    /**
     * Applies this rule set to a RDF model.
     *
     * @param model the RDF model
     * @param xml   the model as expressed in its source RDF/XML
     * @return <code>true</code> if the model has been changed by this rule set
     */
    public boolean apply(Model model, RdfXmlDocument xml) {
        boolean modelChanged = false;
        for (final ResIterator it = model.listSubjects(); it.hasNext(); ) {
            final Resource subject = it.next();
            for (Rule rule : rules) {
                final boolean applied = rule.apply(subject, xml);
                if (applied) {
                    LOG.finer(() -> String.format("! %s (%s)", subject, rule));
                    modelChanged = true;
                } else {
                    LOG.finest(() -> String.format(". %s (%s)", subject, rule));
                }
            }
        }
        return modelChanged;
    }

    /**
     * Applies this rule set to a RDF model contained in a RDF/XML file.
     *
     * @see Migration#apply(Model, RdfXmlDocument)
     */
    public boolean apply(File rdf)
        throws IOException, SAXException, TransformerException, JenaException {

        return apply(rdf, new RdfXmlDocument(rdf), RdfXmlDocument.model(rdf));
    }

    public boolean apply(File rdf, RdfXmlDocument xml, Model model) throws TransformerException {
        if (apply(model, xml)) {
            xml.write(rdf);
            return true;
        }
        return false;
    }

    /**
     * Entry point into the migration tool.
     *
     * <p>A migration rule set is read from the classpath and applied to
     * all RDF/XML sources contained in ARC's GitLab, optionally limited to some projects.
     *
     * @see Migration#parse(Document)
     * @see Arc#rdfRepositories()
     * @see Migration#apply(File)
     */
    public static void main(String[] args) throws Exception {
        final long start = System.currentTimeMillis();

        Logging.configure();
        final Logger log = Logging.forClass(Migration.class);

        Document migrationXml;
        try (InputStream rulesStream = Migration.class.getResourceAsStream(RULES_RESOURCE)) {
            migrationXml = XML.newDocumentBuilder().parse(rulesStream);
        }

        final Migration migration = Migration.parse(migrationXml);
        log.fine(() -> String.format("< %s", migration));

        new Workspace(new Arc()).projects()
            .filter(projectFilter())
            .parallel()
            .forEach(rdfProject -> {

                rdfProject.reset();
                rdfProject.withBranch("master", false);

                rdfProject.rdfFiles().parallel().forEach(rdfFile -> {
                    try {
                        RdfXmlDocument.format(rdfFile);
                    } catch (IOException | SAXException | TransformerException e) {
                        log.log(Level.WARNING, e, rdfFile::toString);
                    }

                });
                rdfProject.commitIfChanged(join(" | ", migration.title, "RDF/XML formatting"));

                rdfProject.rdfFiles().parallel().forEach(rdfFile -> {
                    try {
                        log.fine(() -> String.format("? %s", rdfFile.getAbsolutePath()));
                        if (migration.apply(rdfFile)) {
                            log.info(() -> String.format("! %s", rdfFile.getAbsolutePath()));
                        }
                    } catch (IOException | SAXException | TransformerException | JenaException e) {
                        log.log(Level.WARNING, e, rdfFile::toString);
                    }
                });
                
                if (rdfProject.commitIfChanged(join(" | ", migration.title, "RDF migration"))) {
                    //rdfProject.push();
                    log.info(() -> String.format("! %s", rdfProject));
                }
            });

        final long end = System.currentTimeMillis();
        log.info(() -> String.format(". %s", Duration.ofMillis(end - start)));
    }

    private static Predicate<RdfProject> projectFilter() {
        return p -> p.rdfFiles().collect(Collectors.summingLong(File::length)) > ONE_GIGABYTE;
    }

    /*
    private static Predicate<RdfProject> projectFilter() {
        final String arcProjectsEnv = Optional.ofNullable(System.getenv("ARC_PROJECTS"))
            .orElse("");

        final Set<String> projects = Pattern.compile("\\s+").splitAsStream(arcProjectsEnv)
            .map(String::trim).filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());

        return projects.isEmpty()
            ? (p -> true)
            : (p -> projects.contains(p.git.gitLabProject.name));
    }
    */

    private static final int ONE_GIGABYTE = 1_073_741_824;
}
