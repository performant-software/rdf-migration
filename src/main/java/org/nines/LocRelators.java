package org.nines;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class LocRelators {

    public static final String uri = "http://www.loc.gov/loc.terms/relators/";

    public static final Property property(String local) {
        return ResourceFactory.createProperty(uri, local);
    }

}
