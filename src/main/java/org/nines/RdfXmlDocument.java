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
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpression;

/**
 * The DOM of an RDF/XML document.
 */
public class RdfXmlDocument {

    public final Document document;
    public final Map<String, List<Element>> resourceIndex;

    public static Model model(File file) {
        try {
            return ModelFactory.createDefaultModel().read(file.toURI().toURL().toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static File format(File file) throws IOException, SAXException, TransformerException {
        new RdfXmlDocument(file).write(file);
        return file;
    }

    public RdfXmlDocument(File file) throws IOException, SAXException {
        this(XML.newDocumentBuilder().parse(file));
    }

    public RdfXmlDocument(Document document) {
        this.document = document;
        this.resourceIndex = resourceIndex(document);
    }

    /**
     * Serializes the DOM to a given file, removing empty text nodes and indenting the source
     * in the process.
     *
     * @param rdf the destination file
     */
    public void write(File rdf) throws TransformerException {
        XML.nodes(EMPTY_TEXT_NODES, document).forEach(n -> n.getParentNode().removeChild(n));
        XML.indentingTransformer(XML.newTransformer())
                .transform(new DOMSource(document), new StreamResult(rdf));
    }

    /**
     * Adds a property/value assignment to a RDF subject.
     *
     * @param resource the RDF subject
     * @param property the RDF property
     * @param value the string literal expressing the value to add
     */
    public void add(Resource resource, Property property, String value) {
        final List<Element> elements = resourceIndex.getOrDefault(
            resource.getURI(), Collections.emptyList()
        );

        final Element element = elements.stream()
            .findFirst().orElseThrow(IllegalArgumentException::new);

        element.appendChild(document.createElementNS(
                property.getNameSpace(),
                qualifiedName(element, property)
        )).setTextContent(value);

    }

    /**
     * Removes all assignments of a given RDF property from a RDF subject.
     *
     * @param resource the RDF subject
     * @param property the RDF property whose values are removed
     */
    public void remove(Resource resource, Property property) {
        remove(resource, property, null);
    }

    /**
     * Removes assignments of a given RDF property from a RDF subject.
     *
     * @param resource the RDF subject
     * @param property the RDF property
     * @param value a specific value to be removed or <code>null</code> if all value assignments
     *              shall be removed
     */
    public void remove(Resource resource, Property property, String value) {
        final String ns = property.getNameSpace();
        final String ln = property.getLocalName();
        final List<Element> elements = resourceIndex.getOrDefault(
            resource.getURI(), Collections.emptyList()
        );
        for (Element element : elements) {
            final List<Element> removed = new ArrayList<>();
            for (Element propertyEl : XML.elements(XML.children(element))) {
                if (!ns.equals(propertyEl.getNamespaceURI())) {
                    continue;
                }
                if (!ln.equals(propertyEl.getLocalName())) {
                    continue;
                }
                if (value != null && !value.equals(propertyEl.getTextContent().trim())) {
                    continue;
                }
                removed.add(propertyEl);
            }
            removed.forEach(element::removeChild);
        }
    }

    private static String qualifiedName(Element parent, Property property) {
        return Optional.ofNullable(parent.lookupPrefix(property.getNameSpace()))
                .map(prefix -> prefix + ":").orElse("") + property.getLocalName();
    }

    private static Map<String, List<Element>> resourceIndex(Document document) {
        final Map<String, List<Element>> resourceIndex = new HashMap<>();
        final NodeIterator it = ((DocumentTraversal) document).createNodeIterator(
                document,
                NodeFilter.SHOW_ELEMENT,
                null,
                false
        );
        while (true) {
            final Element element = (Element) it.nextNode();
            if (element == null) {
                break;
            }
            final String rdfAbout = element.getAttributeNS(RDF.uri, "about");
            if (rdfAbout.isEmpty()) {
                continue;
            }
            resourceIndex.computeIfAbsent(rdfAbout, k -> new ArrayList<>()).add(element);
        }
        return resourceIndex;
    }

    private static final XPathExpression EMPTY_TEXT_NODES = XML.xpath(
        "//text()[normalize-space(.) = '']"
    );

}
