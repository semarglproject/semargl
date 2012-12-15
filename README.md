Welcome to the home of Semargl!
===============================

Semargl is a modular framework for crawling linked data from structured
documents. The main goal of project is to provide lightweight and
performant tool without excess dependencies.

At this moment it offers streaming parsers for:
* RDFa 1.0 and 1.1
* RDF/XML
* NTriples

For more information you can visit [project homepage](http://semarglproject.org),
or try [demo](http://demo.semarglproject.org).

Why use Semargl?
================

Lightweight
-----------

Semargl’s code is small and simple to understand. It will never
[read a mail](http://en.wikipedia.org/wiki/Zawinski's_law_of_software_envelopment).
Internally it operates with raw strings and creates as few objects as possible,
so your Android or GAE projects will be happy.

Standard-conformant
-------------------

Framework conforms corresponding W3C standards for IRI's, RDF, RDFa and so on.
All code base is covered with tests made by W3C, RDF Web Applications Working
Group and Jena ARP project.

Dead Simple
-----------

No jokes!

```java
// just init triple sink you want
MGraph graph = ... // Clerezza calls
TripleSink sink = new ClerezzaTripleSink(graph);
// create processing pipe
DataProcessor<Reader> dp = new CharSource()
        .streamingTo(new NTriplesParser()
                .streamingTo(sink).build();
// and run it!
dp.process(new FileReader(file), docUri);
```

or use Jena wiring

```java
JenaRdfaReader.inject();
Model model = ... // Jena calls
model.read(new FileReader(file), docUri, "RDFA");
```

Semargl works out of the box with Android and GAE applications, frameworks
such as Apache Jena and Apache Clerezza. See examples dir for more info.

What Semargl is not
===================

It's not a validator of any kind.

It's not a triple store (but it provides bridges to other ones).

It's still not a framework with stable API (and won't be until major release).

Supported data formats
======================

RDF/XML
-------

Parsing support. Implementation covered by tests from Jena ARP project.
Atm it outperforms Jena parser even if you load triples to Jena model.
Benchmarks are located in examples folder.

RDFa
----

RDFa parser currently passes all RDFa 1.0 and 1.1
[conformance](http://rdfa.info/test-suite/) tests for all document formats.
Document format detection works out-of-the-box, so you shouldn't worry about
specifying document format. RDFa version detection is also present with
default version 1.1.
Supported RDFa extensions: Vocubulary Expansion, Processor Graph.

NTriples
--------

Parsing support. Implementation covered by tests from Jena ARP project.

Turtle
------

Serialization support. Syntax abbreviations.

Build
=====

To build framework just run mvn install. At some stage RDFa tests will download
large dataset from rdfa.info, be patient.
