## Synopsis

Interpretes a DSL defining RDF data migration rules and applies those to a set of RDF/XML 
resources under version control.

## Code Example

    
    ARC_RDF_WORKSPACE="..." ARC_PROJECTS="..." GITLAB_PRIVATE_TOKEN="..." mvn exec:exec
        
## Motivation

The Advanced Research Consortium (ARC) runs a catalog of RDF metadata. Changes to those metadata
and its underlying schema shall be supported by this tool in that it automates the migration of
RDF/XML datasets necessitated by adjustments to the schema.

## Installation

### Requirements:

* Java 8
* [Apache Maven](http://www.maven.org/)
* [Git](https://git-scm.com/) Command Line Tool


## API Reference

* [ARC Web Service](http://catalog.ar-c.org/)
* [GitLab API](https://docs.gitlab.com/ce/api/)
