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
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * ARC's RDF schema definition, specifically the set of singleton properties.
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
