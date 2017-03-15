package org.nines.filters;

import org.apache.jena.rdf.model.Resource;

import java.util.Arrays;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class NoneOfSubjectFilter implements SubjectFilter {

    public final SubjectFilter[] filters;

    public NoneOfSubjectFilter(SubjectFilter[] filters) {
        this.filters = filters;
    }

    @Override
    public boolean appliesTo(Resource resource) {
        for (SubjectFilter filter : filters) {
            if (filter.appliesTo(resource)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("(noneOf %s)", Arrays.toString(filters));
    }
}
