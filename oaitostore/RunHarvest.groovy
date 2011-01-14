@Grapes([
    @Grab(group='com.hp.hpl.jena', module='jena', version='2.6.4'),
    @Grab(group='net.sourceforge.nekohtml', module='nekohtml', version='1.9.14'),
    @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1'),
    @Grab(group='xerces', module='xercesImpl', version='2.9.1') ])

// Researchspace.org
import com.hp.hpl.jena.rdf.model.*
import com.hp.hpl.jena.vocabulary.*

String findWikipediaReference() {
  return ""
}

def oai_client = new OAIClient()

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
  addValuesToModel(model,new_res,dc_titles,DC.title)
  addValuesToModel(model,new_res,dc_descriptions,DC.description)
  addValuesToModel(model,new_res,dc_subjects,DC.subject)
  addValuesToModel(model,new_res,partof,DC.isPartOf)
  addValuesToModel(model,new_res,type,DC.type)
  // model.write(System.out, "N-TRIPLE")
  model.write(System.out)
}

void addValuesToModel(model, resource, values, predicate) {
  if ( values.size() == 0 ) {
    // Do nothing
  }
  else if ( values.size() > 1 ) {
    values.each { val ->
      reference(model,resource, val.text(), predicate, null)
    }
  }
  else {
    reference(model,resource, values.text(), predicate, null)
  }
}

void reference(model, resource, value, predicate, sources) {
  resource.addProperty(predicate, value)
}

oai_client.harvest('http://www.culturegrid.org.uk/dpp/oai','CultureGrid_Item', cgItemHandler)

