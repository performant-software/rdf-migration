package org.nines;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class Schema {

    public static final Set<Property> SINGLETON_PROPERTIES = new HashSet<>(Arrays.asList(
            Collex.archive,
            DC.title,
            DC.language,
            Collex.date,
            RDFS.label,
            RDF.value,
            Collex.freeculture,
            Collex.source_xml,
            Collex.source_html,
            Collex.source_sgml,
            Collex.text,
            Collex.image,
            Collex.thumbnail,
            Collex.ocr,
            Collex.fulltext
    ));
}
