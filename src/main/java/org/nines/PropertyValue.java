package org.nines;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.w3c.dom.Element;

import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class PropertyValue {

    private static final Logger LOG = Logging.forClass(PropertyValue.class);

    public final Property property;
    public final String namespaceUri;
    public final String localName;
    public final String value;
    private boolean singleton;

    public PropertyValue(String namespaceUri, String localName, String value) {
        this.property = ResourceFactory.createProperty(namespaceUri, localName);
        this.singleton = Schema.SINGLETON_PROPERTIES.contains(property);
        this.namespaceUri = namespaceUri;
        this.localName = localName;
        this.value = value;
    }

    public PropertyValue(Element element) {
        this(element.getNamespaceURI(), element.getLocalName(), element.getTextContent().trim());
    }

    @Override
    public String toString() {
        return String.format("<{%s}%s = %s>", namespaceUri, localName, value);
    }

    public boolean addTo(Resource resource, RdfXmlDocument xml) {
        if (hasProperty(resource)) {
            return false;
        }
        if (singleton) {
            xml.remove(resource, property);
        }
        xml.add(resource, property, value);
        LOG.finest(() -> String.format("+ %s %s = %s", resource, property, value));
        return true;
    }

    public boolean removeFrom(Resource resource, RdfXmlDocument xml) {
        if (!hasProperty(resource)) {
            return false;
        }
        xml.remove(resource, property, value);
        LOG.finest(() -> String.format("- %s %s = %s", resource, property, value));
        return true;
    }

    private boolean hasProperty(Resource resource) {
        for (final StmtIterator it = resource.listProperties(property); it.hasNext(); ) {
            if (value.equals(it.next().getObject().asLiteral().getString())) {
                return true;
            }
        }
        return false;
    }
}
