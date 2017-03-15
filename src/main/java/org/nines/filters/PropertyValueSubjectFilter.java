package org.nines.filters;

import org.apache.jena.rdf.model.Resource;
import org.nines.PropertyValue;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class PropertyValueSubjectFilter implements SubjectFilter {

    private final PropertyValue propertyValue;

    public PropertyValueSubjectFilter(PropertyValue propertyValue) {
        this.propertyValue = propertyValue;
    }

    @Override
    public String toString() {
        return String.format("(= %s)", propertyValue);
    }

    @Override
    public boolean appliesTo(Resource resource) {
        return resource.listProperties(propertyValue.property)
                .mapWith(stmt -> stmt.getObject().asLiteral().getString())
                .filterKeep(propertyValue.value::equals)
                .hasNext();
    }
}
