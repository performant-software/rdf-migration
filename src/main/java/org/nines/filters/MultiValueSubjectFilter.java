package org.nines.filters;

import org.apache.jena.rdf.model.Resource;

import java.util.Arrays;

public class MultiValueSubjectFilter implements SubjectFilter {

    private final SubjectFilter[] filters;

    public MultiValueSubjectFilter(SubjectFilter[] filters) {
        this.filters = filters;
    }


    @Override
    public boolean appliesTo(Resource resource) {
        for (SubjectFilter filter : filters) {
            if (filter instanceof PropertyValueSubjectFilter) {
                PropertyValueSubjectFilter pvFilter = (PropertyValueSubjectFilter) filter;
                if (!pvFilter.appliesTo(resource)) {
                    return false;
                }
                return resource.listProperties(pvFilter.propertyValue.property).toSet().size() > 1;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(mult %s)", Arrays.toString(filters));
    }

}
