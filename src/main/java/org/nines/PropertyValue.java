package org.nines;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.w3c.dom.Element;

import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class PropertyValue {

    private static final Logger LOG = Logging.forClass(PropertyValue.class);

    private static final Model MODEL = ModelFactory.createDefaultModel();

    public final Property property;
    public final String namespaceUri;
    public final String localName;
    public final String value;

    public PropertyValue(String namespaceUri, String localName, String value) {
        this.property = MODEL.createProperty(namespaceUri, localName);
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

    public boolean addTo(Resource resource) {
        resource.addProperty(property, value);
        LOG.finest(() -> String.format("+ %s %s = %s", resource, property, value));
        return true;
    }

    public boolean removeFrom(Resource resource) {
        boolean applied = false;
        for (final StmtIterator it = resource.listProperties(property); it.hasNext(); ) {
            final Statement statement = it.next();
            if (value.equals(statement.getObject().asLiteral().getString())) {
                it.remove();
                applied = true;
                LOG.finest(() -> String.format("- %s %s = %s", resource, property, value));
            }
        }
        return applied;
    }
}
