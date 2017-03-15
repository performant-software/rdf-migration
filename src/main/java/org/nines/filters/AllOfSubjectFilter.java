package org.nines.filters;

import org.apache.jena.rdf.model.Resource;

import java.util.Arrays;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class AllOfSubjectFilter implements SubjectFilter {

    private final SubjectFilter[] filters;

    public AllOfSubjectFilter(SubjectFilter[] filters) {
        this.filters = filters;
    }

    @Override
    public boolean appliesTo(Resource resource) {
        for (SubjectFilter filter : filters) {
            if (!filter.appliesTo(resource)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("(allOf %s)", Arrays.toString(filters));
    }

}
