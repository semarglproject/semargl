package org.semarglproject.sink;

/**
 * Interface for quad consuming
 */
public interface QuadSink extends TripleSink {

    /**
     * Callback for handling triples with non literal object
     * @param subj subject's IRI or BNode name
     * @param pred predicate's IRI
     * @param obj object's IRI or BNode name
     * @param graph graph's IRI
     */
    void addNonLiteral(String subj, String pred, String obj, String graph);

    /**
     * Callback for handling triples with plain literal objects
     * @param subj subject's IRI or BNode name
     * @param pred predicate's IRI
     * @param content unescaped string representation of content
     * @param lang content's lang, can be null if no language specified
     * @param graph graph's IRI
     */
    void addPlainLiteral(String subj, String pred, String content, String lang, String graph);

    /**
     * Callback for handling triples with typed literal objects
     * @param subj subject's IRI or BNode name
     * @param pred predicate's IRI
     * @param content unescaped string representation of content
     * @param type literal datatype's IRI
     * @param graph graph's IRI
     */
    void addTypedLiteral(String subj, String pred, String content, String type, String graph);

}
