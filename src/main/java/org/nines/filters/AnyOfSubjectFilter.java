package org.nines.filters;

import org.apache.jena.rdf.model.Resource;

import java.util.Arrays;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
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
