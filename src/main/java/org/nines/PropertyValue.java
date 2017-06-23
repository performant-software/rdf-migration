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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.w3c.dom.Element;

import java.util.logging.Logger;

/**
 * A RDF property and a corresponding string literal as its value.
 */
public class PropertyValue {

    private static final Logger LOG = Logging.forClass(PropertyValue.class);

    public final Property property;
    public final String namespaceUri;
    public final String localName;
    public final String value;
    private boolean singleton;

    /**
     * Creates a RDF property/value assignment.
     *
     * @param namespaceUri the namespace of the RDF property
     * @param localName the local name of the RDF property
     * @param value the string literal representing the assigned value
     */
    public PropertyValue(String namespaceUri, String localName, String value) {
        this.property = ResourceFactory.createProperty(namespaceUri, localName);
        this.singleton = Schema.SINGLETON_PROPERTIES.contains(property);
        this.namespaceUri = namespaceUri;
        this.localName = localName;
        this.value = value;
    }

    /**
     * Creates a RDF property/value assignment from an RDF/XML element.
     *
     * @see PropertyValue#PropertyValue(String, String, String)
     */
    public PropertyValue(Element element) {
        this(element.getNamespaceURI(), element.getLocalName(), element.getTextContent().trim());
    }

    @Override
    public String toString() {
        return String.format("<{%s}%s = %s>", namespaceUri, localName, value);
    }

    /**
     * Adds this property/value assignment to the given subject/resource by modifying the
     * source RDF/XML DOM.
     *
     * @param resource the RDF subject to which the property/value will be added
     * @param xml the source RDF/XML DOM to be modified
     * @return <code>true</code> if the assignment has been added, i.e. has not existed before
     */
    public boolean addTo(Resource resource, RdfXmlDocument xml) {
        if (hasProperty(resource)) {
            return false;
        }
        if (singleton) {
            xml.remove(resource, property);
            resource.removeAll(property);
        }
        xml.add(resource, property, value);
        resource.addProperty(property, value);
        LOG.finest(() -> String.format("+ %s %s = %s", resource, property, value));
        return true;
    }

    /**
     * Removes the property/value assignment from a given subject/resource by modifying the source
     * RDF/XML DOM.
     *
     * @param resource the RDF subject from which the property/value will be removed
     * @param xml the source RDF/XML DOM to be modified.
     * @return <code>true</code> if the assignment has been removed, i.e. has existed before
     */
    public boolean removeFrom(Resource resource, RdfXmlDocument xml) {
        if (!hasProperty(resource)) {
            return false;
        }
        xml.remove(resource, property, value);
        for (final StmtIterator it = resource.listProperties(property); it.hasNext(); ) {
            if (value.equals(it.nextStatement().getString())) {
                it.remove();
            }
        }
        LOG.finest(() -> String.format("- %s %s = %s", resource, property, value));
        return true;
    }

    private boolean hasProperty(Resource resource) {
        for (final StmtIterator it = resource.listProperties(property); it.hasNext(); ) {
            if (value.equals(it.next().getObject().asLiteral().getString().trim())) {
                return true;
            }
        }
        return false;
    }
}
