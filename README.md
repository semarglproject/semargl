Welcome to the home of Semargl!
===============================

Semargl is a modular framework for crawling linked data from structured documents.
The main goal of project is to provide lightweight and performant tool without excess dependencies.

At this moment it provides streaming parsers for:
* RDFa 1.0 and 1.1
* RDF/XML
* NTriples

Why use Semargl?
================

Lightweight
-----------

Semargl’s code is small (each parser is under 1k LOC) and simple to understand.
Hope it will never [read a mail](http://en.wikipedia.org/wiki/Zawinski's_law_of_software_envelopment).

Standard-conformant
-------------------

Framework conforms corresponding W3C standards for IRI's, RDF, RDFa and so on. All code base is covered
with tests made by W3C, RDF Web Applications Working Group and Jena ARP project.

Dead Simple
-----------

No jokes!

// just init triple sink you want
Model model = ModelFactory.createDefaultModel();
TripleSink sink = new JenaTripleSink(model);
// create processing pipe
DataProcessor<Reader> dp = new SaxSource(XMLReaderFactory.createXMLReader())
        .streamingTo(new RdfXmlParser()
                .streamingTo(sink).build();
// and run it!
dp.process(new FileReader(file), docUri);

Semargl works out of the box with Android applications and frameworks such as Apache Jena and Apache Clerezza.
See examples dir for more info (JavaDocs coming soon).

What Semargl is not
===================

It's not a validator of any kind.
It's not a triple store (but it provides bridges to other ones).
It's not a framework with stable API (and will be until 1.0 release).

Supported data formats
======================

RDF/XML
-------

Implementation covered by tests from Jena ARQ project. Btw it outperforms Jena parser even if you load triples
to Jena model. Benchmark located in examples folder.

RDFa
----

RDFa parser currently passes all RDFa 1.0 and 1.1 [conformance](http://rdfa.info/test-suite/) tests for
all document formats. Also, you shouldn't worry about specifying document format because auto-detection works
out-of-the-box.

NTriples
--------

Implementation covered by tests from Jena ARQ project.
