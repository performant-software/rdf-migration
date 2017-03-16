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
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class Rule {

    public final SubjectFilter subjectFilter;
    public final PropertyValue[] addedProperties;
    public final PropertyValue[] removedProperties;

    public Rule(SubjectFilter subjectFilter, PropertyValue[] addedProperties, PropertyValue[] removedProperties) {
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

    public static Rule parse(Element element) {
        final List<SubjectFilter> subjectFilters = new LinkedList<>();
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
                subjectFilters.addAll(Arrays.asList(parseSubjectFilter(el)));
            }
        }

        return new Rule(
                new AllOfSubjectFilter(subjectFilters.toArray(new SubjectFilter[subjectFilters.size()])),
                addedProperties.toArray(new PropertyValue[addedProperties.size()]),
                removedProperties.toArray(new PropertyValue[removedProperties.size()])
        );
    }

    public static SubjectFilter[] parseSubjectFilter(Element root) {
        final List<SubjectFilter> subjectFilters = new LinkedList<>();
        for (Element el : elements(children(root))) {
            if (isMigrationElement(el, "addProperties")) {
                continue;
            } else if (isMigrationElement(el, "removeProperties")) {
                continue;
            } else if (isMigrationElement(el, "anyOf")){
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
