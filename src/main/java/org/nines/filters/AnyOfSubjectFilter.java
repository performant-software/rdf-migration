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

import java.util.Arrays;

/**
 * A compound subject filter, matching RDF subjects which in turn match any of the child filters.
 */
public class AnyOfSubjectFilter implements SubjectFilter {

    private final SubjectFilter[] filters;

    public AnyOfSubjectFilter(SubjectFilter[] filters) {
        this.filters = filters;
    }

    @Override
    public boolean appliesTo(Resource resource) {
        for (SubjectFilter filter : filters) {
            if (filter.appliesTo(resource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(anyOf %s)", Arrays.toString(filters));
    }

}
