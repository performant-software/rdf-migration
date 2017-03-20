package org.nines;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class Collex {

    public static final String uri = "http://www.collex.org/schema#";

    protected static final Property property(String local) {
        return ResourceFactory.createProperty(uri, local);
    }

    public static final Property archive = property("archive");
    public static final Property date = property("date");
    public static final Property freeculture = property("freeculture");
    public static final Property source_xml = property("source_xml");
    public static final Property source_html = property("source_html");
    public static final Property source_sgml = property("source_sgml");
    public static final Property text = property("text");
    public static final Property image = property("image");
    public static final Property thumbnail = property("thumbnail");
    public static final Property ocr = property("ocr");
    public static final Property fulltext = property("fulltext");

}
