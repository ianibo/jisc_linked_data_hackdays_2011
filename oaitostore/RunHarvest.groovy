@Grapes([
    @Grab(group='com.hp.hpl.jena', module='jena', version='2.6.4'),
    @Grab(group='net.sourceforge.nekohtml', module='nekohtml', version='1.9.14'),
    @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1'),
    @Grab(group='xerces', module='xercesImpl', version='2.9.1') ])

// Researchspace.org

import com.hp.hpl.jena.rdf.model.*
import com.hp.hpl.jena.vocabulary.*
import groovyx.net.http.*
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovyx.net.http.*
import org.apache.http.entity.mime.*
import org.apache.http.entity.mime.content.*
import java.nio.charset.Charset


String findWikipediaReference() {
  return ""
}

def oai_client = new OAIClient()


// This closure takes a value passed in a subject and tries to resolve the literal into one or more resource
def subjectResolver = { model, resource, value ->
  println "subjectResolver: ${value}"
  println "Attempt to resolve urn:cg:subject:${value}"
  def res = null; // getResource("urn:cg:subject:${value}")
  getResource("urn:cg:subject:${value}")
  if ( res == null ) {
    // We need to create a new resource to represent this subject.
  }

  value
}

// Define some schema specific closures
def oaidc = { record ->
  def identifier = record.header.identifier;
  def metadata = record.metadata
  def dc_identifier = metadata.dc.identifier
  def dc_creator = metadata.dc.creator
  def dc_title = metadata.dc.title
  def dc_description = metadata.dc.description

  println "Processing dc record ${dc_identifier} - ${dc_title} - ${dc_description} - ${dc_creator}"
}

cgItemHandler = { record ->
  def identifier = record.header.identifier;
  def metadata = record.metadata
  def dc_identifier = metadata.description.identifier
  def dc_creators = metadata.description.creator
  def dc_titles = metadata.description.title
  def dc_descriptions = metadata.description.description
  def dc_subjects = metadata.description.subject
  def rights = metadata.description.rights
  def rightsHolder = metadata.description.rightsHolder
  def coverage = metadata.description.coverage
  def license = metadata.description.license
  def partOf = metadata.description.isPartOf
  def type = metadata.description.type

  // println "Processing cg record ${dc_identifier} - ti:${dc_titles} - desc:${dc_descriptions} - creators:${dc_creators} - subjects:${dc_subjects} rights:${rights} rightsHolder:${rightsHolder} coverage:${coverage} license:${license}"

  // Create a new empty graph for this resource
  def Model model = ModelFactory.createDefaultModel();

  // create the resource
  //   and add the properties cascading style
  def Resource new_res = model.createResource(dc_identifier.text())
  addValuesToModel(model,new_res,dc_titles,DCTerms.title)
  addValuesToModel(model,new_res,dc_descriptions,DCTerms.description)
  addValuesToModel(model,new_res,dc_subjects,DCTerms.subject, subjectResolver)
  addValuesToModel(model,new_res,partOf,DCTerms.isPartOf)
  addValuesToModel(model,new_res,type,DCTerms.type)
  // model.write(System.out, "N-TRIPLE")
  // model.write(System.out)
  def sw = new java.io.StringWriter()
  model.write(sw)
  println "Attempting to publish ${dc_identifier.text()}"
  publish(identifier.text(), sw.toString())
}

// Helpers

void addValuesToModel(model, resource, values, predicate) {
  addValuesToModel(model, resource, values, predicate, null)
}

void addValuesToModel(model, resource, values, predicate, resolver) {
  if ( values.size() == 0 ) {
    // Do nothing
  }
  else if ( values.size() > 1 ) {
    values.each { val ->
      reference(model,resource, val.text(), predicate, resolver)
    }
  }
  else {
    reference(model,resource, values.text(), predicate, resolver)
  }
}

void getResource(uri) {
  def result = null
  try {
    def endpoint = new groovyx.net.http.RESTClient( 'http://localhost:9000/sparql/' )
    def response = endpoint.post(
      body: [
        contentType : "application/json",
        // "content-type": "application/json",    // 4Store doesn't seem to want to return json.
        query: """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                  SELECT * WHERE {
                   <${uri}> ?p ?o
                  } LIMIT 10 """
      ]
    ) { resp, xml ->
      def xml_response = new XmlSlurper().parseText(xml.text)
      if ( xml_response.results.children().size() > 0 ) {
        println "There are results."
      }
      else {
        println "There are no results"
      }
    }
  }
  catch ( Exception e ) {
    println "Problem ${e}"
    e.printStackTrace()
  }
  finally {
  }
  result
}

void publish(graph_uri, rdfxml) {
  try {
    // def endpoint = new HTTPBuilder( 'http://localhost:9000/data/' )
    println "Deleteing and recreating graph at ${graph_uri} (${java.net.URLEncoder.encode(graph_uri)})"
    def endpoint = new groovyx.net.http.RESTClient( 'http://localhost:9000/data/' )

    // Firstly, delete any previous graph with this graph_uri
    endpoint.delete(path:java.net.URLEncoder.encode(graph_uri))

    def response = endpoint.post(
    body: [
      requestContentType: URLENC,
      "mime-type": "application/rdf",  // application/x-turtle, text/rdf+n3, text/rdf+nt, application/x-trig
      graph: java.net.URLEncoder.encode(graph_uri),
      data: rdfxml
    ]) {  resp ->
      println "${resp}"
    }
  }
  catch ( Exception e ) {
    println "Problem ${e}"
    e.printStackTrace()
  }
  finally {
  }
}

void reference(model, resource, value, predicate, resolver) {
  // Don't create triples like "Unknown Subject" its silly.
  if ( ! value.contains("unknown") ) {
    if ( resolver != null ) {
      resolver(model, resource, value)
    }
    else {
      resource.addProperty(predicate, value)
    }
  }
}

oai_client.harvest('http://www.culturegrid.org.uk/dpp/oai','CultureGrid_Item', cgItemHandler)

