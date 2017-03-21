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
package org.nines.filters;

import org.apache.jena.rdf.model.Resource;
import org.nines.PropertyValue;

/**
 * A subject filter, matching RDF subjects with a given property and textual value.
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
