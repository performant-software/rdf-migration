package org.nines.filters;

import org.apache.jena.rdf.model.Resource;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public interface SubjectFilter {

    boolean appliesTo(Resource resource);
}
