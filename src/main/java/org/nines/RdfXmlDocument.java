package org.nines;

import net.middell.XML;
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
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class RdfXmlDocument {

    public final Document document;
    public final Map<String, List<Element>> resourceIndex;

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

    public void write(File rdf) throws TransformerException {
        XML.nodes(EMPTY_TEXT_NODES, document).forEach(n -> n.getParentNode().removeChild(n));
        XML.indentingTransformer(XML.newTransformer())
                .transform(new DOMSource(document), new StreamResult(rdf));
    }

    public void add(Resource resource, Property property, String value) {
        final List<Element> elements = resourceIndex.getOrDefault(resource.getURI(), Collections.emptyList());
        final Element element = elements.stream().findFirst().orElseThrow(IllegalArgumentException::new);

        element.appendChild(document.createElementNS(
                property.getNameSpace(),
                qualifiedName(element, property)
        )).setTextContent(value);

    }

    public void remove(Resource resource, Property property) {
        remove(resource, property, null);
    }

    public void remove(Resource resource, Property property, String value) {
        final String ns = property.getNameSpace();
        final String ln = property.getLocalName();
        final List<Element> elements = resourceIndex.getOrDefault(resource.getURI(), Collections.emptyList());
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

    private static final XPathExpression EMPTY_TEXT_NODES = XML.xpath("//text()[normalize-space(.) = '']");

}
