Welcome to the home of Semargl!
===============================

Semargl is a modular framework for crawling [linked data](http://en.wikipedia.org/wiki/Linked_data)
from structured documents. The main goal of project is to provide lightweight
and performant tool without excess dependencies.

At this moment it offers high-performant streaming parsers for RDF/XML,
[RDFa](http://en.wikipedia.org/wiki/Rdfa), N-Triples,
streaming serializer for Turtle and integration with Jena, Clerezza and Sesame.

Small memory footprint, and CPU requirements allow framework to be embedded in any system.
It runs seamlessly on Android and GAE. You can try [RDFa parser demo](http://demo.semarglproject.org)
or visit [project page](http://semarglproject.org) for more information.

Why use Semargl?
================

Lightweight
-----------

Semargl’s code is small and simple to understand. It will never
[read a mail](http://en.wikipedia.org/wiki/Zawinski's_law_of_software_envelopment).
Internally it operates with raw strings and creates as few objects as possible,
so your Android or GAE applications will be happy.

Standard conformant
-------------------

All implementations fully support corresponding W3C specifications and test suites.
See more at [project page](http://semarglproject.org).

Dead Simple
-----------

No jokes!

```java
// just init triple store you want
MGraph graph = ... // Clerezza calls
// create processing pipe
StreamProcessor<Reader> sp = CharSource.streamingTo(NTriplesParser.streamingTo(new ClerezzaSink(graph));
// and run it!
sp.process(new FileReader(file), docUri);
```

or use Jena wiring

```java
JenaRdfaReader.inject();
Model model = ... // Jena calls
model.read(new FileReader(file), docUri, "RDFA");
```

similar for Sesame

```java
RDFParser rdfParser = Rio.createParser(RDFaFormat.RDFA);
rdfParser.setRDFHandler(model);
rdfParser.parse(input, inputUri);
```

If you don't want to use external frameworks, you can always use internal
serializers or implement own TripleSink to process triple streams.
Feel free to use examples provided with project.

What Semargl is not
===================

It's not a validator of any kind.

It's not a triple store (but it provides bridges to other ones).

It's still not a framework with stable API (and won't be until major release).

Supported data formats
======================

Streaming parsers
-----------------

* RDF/XML
* RDFa
* NTriples

Stream serializers
------------------

* Turtle
* Jena model
* Clerezza graph
* Sesame RDFHandler

Build
=====

To build framework just run `mvn install`.
