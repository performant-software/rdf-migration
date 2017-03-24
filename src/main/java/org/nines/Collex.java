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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * The Collex RDF schema namespace.
 */
public class Collex {

    public static final String uri = "http://www.collex.org/schema#";

    private static final Property property(String local) {
        return ResourceFactory.createProperty(uri, local);
    }

    public static final Property archive = property("archive");
    public static final Property federation = property("federation");
    public static final Property date = property("date");
    public static final Property genre = property("genre") ;
    public static final Property discipline = property("discipline");
    public static final Property freeculture = property("freeculture");
    public static final Property source_xml = property("source_xml");
    public static final Property source_html = property("source_html");
    public static final Property source_sgml = property("source_sgml");
    public static final Property text = property("text");
    public static final Property image = property("image");
    public static final Property thumbnail = property("thumbnail");
    public static final Property ocr = property("ocr");
    public static final Property fulltext = property("fulltext");
    public static final Property pageof = property("pageof");
    public static final Property pagenum = property("pagenum");


}
