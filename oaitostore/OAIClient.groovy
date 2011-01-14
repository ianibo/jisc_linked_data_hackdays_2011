@Grapes([
    @Grab(group='net.sourceforge.nekohtml', module='nekohtml', version='1.9.14'),
    @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1'),
    @Grab(group='xerces', module='xercesImpl', version='2.9.1') ])

import groovyx.net.http.*
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovyx.net.http.*
import org.apache.http.entity.mime.*
import org.apache.http.entity.mime.content.*
import java.nio.charset.Charset


// A simple OAI CLient
class OAIClient {

  def harvest(address,prefix,record_handler) {
    def oai_client = new HTTPBuilder( address )
    try {
      def more_records = true;
      def resumption_token = null;
      def counter = 0;

      while ( more_records ) {
        def oai_response = null;
        if ( resumption_token != null ) {
          oai_response = oai_client.get( query : [verb:'ListRecords', resumptionToken:resumption_token] )
        }
        else {
          oai_response = oai_client.get( query : [verb:'ListRecords', metadataPrefix:prefix] )
        }

        oai_response.ListRecords.record.each { record ->
          // Header = record.header
          // Metadata = record.metadata
          record_handler(record)
        }
  
        // Extract a resumption token if one exists. The null below is just until I can test how the end case behaves
        resumption_token = null;
        resumption_token = oai_response.ListRecords.resumptionToken
        println "Resumption token ${resumption_token}"

        // Now handle the resumption token
        if ( ( resumption_token != null ) && ( counter++ < 2 ) )
          more_records = true;
        else
          more_records = false;
      }
    }
    catch ( Exception e ) {
        println "Problem ${e}"
        e.printStackTrace()
    }
    finally {
    }
  }

}
