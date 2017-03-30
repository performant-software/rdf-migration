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
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ARC's RDF schema definition, specifically the set of singleton properties.
 */
public class Schema {

    public static class Error {
        public final Resource resource;
        public final Property property;
        public final RDFNode value;
        public final String message;

        public Error(Statement statement, String message) {
            this(statement.getSubject(), statement.getPredicate(), statement.getObject(), message);

        }
        public Error(Resource resource, Property property, RDFNode value, String message) {
            this.resource = resource;
            this.property = property;
            this.value = value;
            this.message = message;
        }
    }
    public static Error[] validate(RdfProject project, Resource resource) {
        final List<List<Error>> errors;
        if (project.git.gitLabProject.name.contains("pages_")) {
            errors = Arrays.asList(
                validatePagesRequired(resource),
                validateUri(resource)
            );
        } else {
            errors = Arrays.asList(
                validateRequired(resource),
                validate(resource, Collex.genre, VALID_GENRES, "Genre not approved by ARC"),
                validate(resource, Collex.discipline, VALID_DISCIPLINES, "Discipline not approved by ARC"),
                validate(resource, DC.type, VALID_TYPES, "Type not approved by ARC"),
                validateRole(resource),
                validateUri(resource)
            );
        }

        return errors.stream().flatMap(List::stream).toArray(Error[]::new);
    }

    public static List<Error> validateRole(Resource resource) {
        return resource.listProperties()
            .filterKeep(stmt -> LocRelators.uri.equals(stmt.getPredicate().getNameSpace()))
            .filterDrop(stmt -> VALID_ROLES.contains(stmt.getPredicate()))
            .mapWith(stmt -> new Error(stmt, "Invalid role"))
            .toList();
    }

    public static List<Error> validateUri(Resource resource) {
        return resource.listProperties(RDFS.seeAlso)
            .filterDrop(stmt -> stmt.getObject().isResource() && !stmt.getObject().asResource().getURI().startsWith("file:"))
            .mapWith(stmt -> new Error(stmt, "URI not resolved properly"))
            .toList();
    }

    private static Optional<Error> maxOne(Property property, Resource resource) {
        final int cardinality = resource.listProperties(property).toSet().size();

        return (cardinality > 1)
            ? Optional.of(new Error(resource, property, null, "More than one value"))
            : Optional.empty();
    }

    /**
     * Confirms that required fields for PAGES archives are present and non-null
     */
    public static List<Error> validatePagesRequired(Resource resource) {
        ArrayList<Error> errors = new ArrayList<>();

        REQUIRED_PAGE_PROPERTIES.stream()
            .filter(p -> !resource.hasProperty(p))
            .map(p -> new Error(resource, p, null, "Property required"))
            .forEach(errors::add);


        resource.listProperties(Collex.text)
            .filterKeep(stmt -> stmt.getObject().asLiteral().getString().isEmpty())
            .mapWith(stmt -> new Error(stmt, "Warning - blank text"))
            .forEachRemaining(errors::add);

        return errors;
    }

    /**
     * Confirms that required fields are present and non-null
     */
    public static List<Error> validateRequired(Resource resource) {
        final List<Error> errors = new ArrayList<>();

        REQUIRED_PROPERTIES.stream()
            .filter(p -> !resource.hasProperty(p))
            .map(p -> new Error(resource, p, null, "Property required"))
            .forEach(errors::add);

        maxOne(Collex.archive, resource).ifPresent(errors::add);
        maxOne(DC.title, resource).ifPresent(errors::add);
        maxOne(RDFS.seeAlso, resource).ifPresent(errors::add);

        final boolean hasRole = resource.listProperties()
            .mapWith(Statement::getPredicate)
            .mapWith(Property::getNameSpace)
            .filterKeep(LocRelators.uri::equals)
            .hasNext();

        if (!hasRole) {
            errors.add(new Error(resource, null, null, "object must contain at least one role:XXX field"));
        }

        return errors;
    }

    public static List<Error> validate(Resource resource, Property predicate, Set<String> validStringLiterals, String message) {
        return resource.listProperties(predicate)
            .filterDrop(stmt -> validStringLiterals.contains(stmt.getObject().asLiteral().getString()))
            .mapWith(stmt -> new Error(stmt, message))
            .toList();
    }

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

    public static final Set<String> VALID_TYPES = new HashSet<>(Arrays.asList(
        "Codex", "Collection", "Dataset", "Drawing", "Illustration", "Interactive Resource",
        "Manuscript", "Map", "Moving Image", "Notated Music", "Page Proofs", "Pamphlet",
        "Periodical", "Physical Object", "Roll", "Sheet", "Sound", "Still Image", "Typescript"
    ));

    public static final Set<String> VALID_GENRES = new HashSet<>(Arrays.asList(
        "Advertisement", "Animation", "Bibliography", "Catalog", "Chronology", "Citation",
        "Collection", "Correspondence", "Criticism", "Drama", "Ephemera", "Essay", "Fiction",
        "Film, Documentary", "Film, Experimental", "Film, Narrative", "Film, Other",
        "Historiography", "Interview", "Life Writing", "Liturgy", "Musical Analysis",
        "Music, Other", "Musical Work", "Musical Score", "Nonfiction", "Paratext",
        "Performance", "Philosophy", "Photograph", "Political Statement", "Poetry", "Religion",
        "Reference Works", "Review", "Scripture", "Sermon", "Speech", "Translation",
        "Travel Writing", "Unspecified", "Visual Art"
    ));

    public static final Set<String> VALID_DISCIPLINES = new HashSet<>(Arrays.asList(
        "Anthropology", "Archaeology", "Architecture", "Art History", "Art Studies", "Book History",
        "Classics and Ancient History", "Dance Studies", "Economics", "Education", "Ethnic Studies",
        "Film Studies", "Gender Studies", "Geography", "History", "Labor Studies", "Law",
        "Literature", "Manuscript Studies", "Math", "Music Studies", "Philosophy",
        "Political Science", "Religious Studies", "Science", "Sociology", "Sound Studies",
        "Theater Studies"
    ));

    public static final Set<Property> VALID_ROLES = Stream.of(
        "role_ABR", "role_ACP", "role_ACT", "role_ADI", "role_ADP", "role_AFT", "role_ANL",
        "role_ANM", "role_ANN", "role_ANT", "role_APE", "role_APL", "role_APP", "role_AQT",
        "role_ARC", "role_ARD", "role_ARR", "role_ART", "role_ASG", "role_ASN", "role_ATO",
        "role_ATT", "role_AUC", "role_AUD", "role_AUI", "role_AUS", "role_AUT", "role_BDD",
        "role_BJD", "role_BKD", "role_BKP", "role_BLW", "role_BND", "role_BPD", "role_BRD",
        "role_BRL", "role_BSL", "role_CAS", "role_CCP", "role_CHR", "role_CLI", "role_CLL",
        "role_CLR", "role_CLT", "role_CMM", "role_CMP", "role_CMT", "role_CND", "role_CNG",
        "role_CNS", "role_COE", "role_COL", "role_COM", "role_CON", "role_COR", "role_COS",
        "role_COT", "role_COU", "role_COV", "role_CPC", "role_CPE", "role_CPH", "role_CPL",
        "role_CPT", "role_CRE", "role_CRP", "role_CRR", "role_CRT", "role_CSL", "role_CSP",
        "role_CST", "role_CTB", "role_CTE", "role_CTG", "role_CTR", "role_CTS", "role_CTT",
        "role_CUR", "role_CWT", "role_DBP", "role_DFD", "role_DFE", "role_DFT", "role_DGG",
        "role_DGS", "role_DIS", "role_DLN", "role_DNC", "role_DNR", "role_DPC", "role_DPT",
        "role_DRM", "role_DRT", "role_DSR", "role_DST", "role_DTC", "role_DTE", "role_DTM",
        "role_DTO", "role_DUB", "role_EDC", "role_EDM", "role_EDT", "role_EGR", "role_ELG",
        "role_ELT", "role_ENG", "role_ENJ", "role_ETR", "role_EVP", "role_EXP", "role_FAC",
        "role_FDS", "role_FLD", "role_FLM", "role_FMD", "role_FMK", "role_FMO", "role_FMP",
        "role_FND", "role_FPY", "role_FRG", "role_GIS", "role_HIS", "role_HNR", "role_HST",
        "role_ILL", "role_ILU", "role_INS", "role_INV", "role_ISB", "role_ITR", "role_IVE",
        "role_IVR", "role_JUD", "role_JUG", "role_LBR", "role_LBT", "role_LDR", "role_LED",
        "role_LEE", "role_LEL", "role_LEN", "role_LET", "role_LGD", "role_LIE", "role_LIL",
        "role_LIT", "role_LSA", "role_LSE", "role_LSO", "role_LTG", "role_LYR", "role_MCP",
        "role_MDC", "role_MED", "role_MFP", "role_MFR", "role_MOD", "role_MON", "role_MRB",
        "role_MRK", "role_MSD", "role_MTE", "role_MTK", "role_MUS", "role_NRT", "role_OPN",
        "role_ORG", "role_ORM", "role_OSP", "role_OTH", "role_OWN", "role_PAN", "role_PAT",
        "role_PBD", "role_PBL", "role_PDR", "role_PFR", "role_PHT", "role_PLT", "role_PMA",
        "role_PMN", "role_POP", "role_PPM", "role_PPT", "role_PRA", "role_PRC", "role_PRD",
        "role_PRE", "role_PRF", "role_PRG", "role_PRM", "role_PRN", "role_PRO", "role_PRP",
        "role_PRS", "role_PRT", "role_PRV", "role_PTA", "role_PTE", "role_PTF", "role_PTH",
        "role_PTT", "role_PUP", "role_RBR", "role_RCD", "role_RCE", "role_RCP", "role_RDD",
        "role_RED", "role_REN", "role_RES", "role_REV", "role_RPC", "role_RPS", "role_RPT",
        "role_RPY", "role_RSE", "role_RSG", "role_RSP", "role_RSR", "role_RST", "role_RTH",
        "role_RTM", "role_SAD", "role_SCE", "role_SCL", "role_SCR", "role_SDS", "role_SEC",
        "role_SGD", "role_SGN", "role_SHT", "role_SLL", "role_SNG", "role_SPK", "role_SPN",
        "role_SPY", "role_SRV", "role_STD", "role_STG", "role_STL", "role_STM", "role_STN",
        "role_STR", "role_TCD", "role_TCH", "role_THS", "role_TLD", "role_TLP", "role_TRC",
        "role_TRL", "role_TYD", "role_TYG", "role_UVP", "role_VAC", "role_VDG", "role_WAC",
        "role_WAL", "role_WAM", "role_WAT", "role_WDC", "role_WDE", "role_WIN", "role_WIT",
        "role_WPR", "role_WST")
        .map(role -> role.substring("role_".length()))
        .map(LocRelators::property)
        .collect(Collectors.toSet());

    public static final Set<Property> REQUIRED_PROPERTIES = new HashSet<>(Arrays.asList(
        Collex.archive, DC.title, DC.date, DC.type, RDFS.seeAlso,
        Collex.genre, Collex.discipline, Collex.freeculture, Collex.fulltext, Collex.ocr,
        Collex.federation
    ));

    public static final Set<Property> REQUIRED_PAGE_PROPERTIES = new HashSet<>(Arrays.asList(
        Collex.text, Collex.pageof, Collex.pagenum
    ));
}
