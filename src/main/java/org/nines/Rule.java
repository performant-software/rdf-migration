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

import org.apache.jena.rdf.model.Resource;
import org.nines.filters.AllOfSubjectFilter;
import org.nines.filters.AnyOfSubjectFilter;
import org.nines.filters.NoneOfSubjectFilter;
import org.nines.filters.PropertyValueSubjectFilter;
import org.nines.filters.SubjectFilter;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static net.middell.XML.children;
import static net.middell.XML.elements;
import static org.nines.Migration.isMigrationElement;

/**
 * A RDF migration rule.
 *
 * <p>Each rule is comprised of a filter matching subjects to which the rule applies, a set of
 * property/value assignments to be added to as well a set of property/value assignments to be
 * removed from matching subjects.</p>
 */
public class Rule {

    public final SubjectFilter subjectFilter;
    public final PropertyValue[] addedProperties;
    public final PropertyValue[] removedProperties;

    /**
     * Creates a migration rule.
     * 
     * @param subjectFilter the filter matching targetted RDF subjects
     * @param addedProperties a set of properties to add
     * @param removedProperties a set of properties to remove
     */
    public Rule(SubjectFilter subjectFilter, PropertyValue[] addedProperties,
                PropertyValue[] removedProperties) {
        this.subjectFilter = subjectFilter;
        this.addedProperties = addedProperties;
        this.removedProperties = removedProperties;
    }

    @Override
    public String toString() {
        return String.format("[ %s | %s | %s]",
                String.format("? %s", subjectFilter),
                String.format("+ %s", Arrays.toString(addedProperties)),
                String.format("- %s", Arrays.toString(removedProperties))
        );
    }

    /**
     * Parses a migration rule from its XML representation.
     *
     * @param element the XML element expressing a migration rule
     * @return the corresponding rule
     */
    public static Rule parse(Element element) {
        final List<SubjectFilter> filters = new LinkedList<>();
        final List<PropertyValue> addedProperties = new LinkedList<>();
        final List<PropertyValue> removedProperties = new LinkedList<>();

        for (Element el : elements(children(element))) {
            if (isMigrationElement(el, "addProperties")) {
                for (Element propertyToAdd : elements(children(el))) {
                    addedProperties.add(new PropertyValue(propertyToAdd));
                }
            } else if (isMigrationElement(el, "removeProperties")) {
                for (Element propertyToRemove : elements(children(el))) {
                    removedProperties.add(new PropertyValue(propertyToRemove));
                }
            } else if (isMigrationElement(el, "subjects")) {
                filters.addAll(Arrays.asList(parseSubjectFilter(el)));
            }
        }

        return new Rule(
                new AllOfSubjectFilter(filters.toArray(new SubjectFilter[filters.size()])),
                addedProperties.toArray(new PropertyValue[addedProperties.size()]),
                removedProperties.toArray(new PropertyValue[removedProperties.size()])
        );
    }

    /**
     * Parses a subject filter as expressed in XML.
     *
     * @param root the root of the XML hierarchy expressing a subject filter
     * @return the corresponding filter
     */
    public static SubjectFilter[] parseSubjectFilter(Element root) {
        final List<SubjectFilter> subjectFilters = new LinkedList<>();
        for (Element el : elements(children(root))) {
            if (isMigrationElement(el, "addProperties")) {
                continue;
            } else if (isMigrationElement(el, "removeProperties")) {
                continue;
            } else if (isMigrationElement(el, "anyOf")) {
                subjectFilters.add(new AnyOfSubjectFilter(parseSubjectFilter(el)));
            } else if (isMigrationElement(el, "allOf")) {
                subjectFilters.add(new AllOfSubjectFilter(parseSubjectFilter(el)));
            } else if (isMigrationElement(el, "noneOf")) {
                subjectFilters.add(new NoneOfSubjectFilter(parseSubjectFilter(el)));
            } else {
                subjectFilters.add(new PropertyValueSubjectFilter(new PropertyValue(el)));
            }
        }
        return subjectFilters.toArray(new SubjectFilter[subjectFilters.size()]);
    }

    /**
     * Attempts to apply this rule to a given RDF subject.
     *
     * @param resource the RDF subject to test
     * @param xml      the RDF/XML DOM to be modified in case this rule applies
     * @return <code>true</code> if this rule's filter matched the given subject and properties have
     *     been added and/or removed
     */
    public boolean apply(Resource resource, RdfXmlDocument xml) {
        boolean applied = false;

        if (appliesTo(resource)) {
            for (PropertyValue propertyValue : addedProperties) {
                applied = propertyValue.addTo(resource, xml) || applied;
            }
            for (PropertyValue propertyValue : removedProperties) {
                applied = propertyValue.removeFrom(resource, xml) || applied;
            }
        }

        return applied;
    }

    public boolean appliesTo(Resource resource) {
        return subjectFilter.appliesTo(resource);
    }
}
