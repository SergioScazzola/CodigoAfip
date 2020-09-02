
package afiputilidades;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.Base64;
import org.apache.axis.encoding.XMLType;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.StringReader;
import java.security.Key;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import javax.swing.JOptionPane;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

/**
 *
 * Sergio Scazzola
 * e-mail scazzolasergio@gmail.com
 * 
 * Modulo de Autenticación y Autorización basado en el código JAVA 
 * publicado por la AFIP.
 * 
 * Los parámetros de la conexión se obtienen del archivo de propiedades : wsaa_client.txt
 * estos parametros deben modificarse de acuerdo a los modos de trabajo (Homologacion/Produccion)
 * y a los nombres de certificados obtenidos.
 * Este modulo trabaja con certificados .p12, los cuáles se generan con "openssl" a partir 
 * del .crt y la clave. Existen muchos tutoriales para consultar sobre como obtener los cerificados.
 *
 * El objetivo de este módulo es autenticar en un servidor de la AFIP (Homo o Produ) a partir 
 * de lo cual se obtienen el token y el sign para luego poder utilizar el resto de los servicios
 * de la AFIP. Antes de llamar a autenticar, se comprueba que el ticket no está vencido, 
 * si no está vencido no llama a autenticar si no que toma el ticket de un archivo (ticket.txt).
 * Si está vencido llama a invoke_wsaa, previamente llama a create_cms para encriptar el mensaje.
 * el método create_LoginTicketRequest genera el requerimiento en formato XML para solicitar 
 * la autorización.
*/
 
public class AfipAuth {
	private String token;
        private String sign;
        private String endpoint;
        
        public AfipAuth(){
            token =    "";
            sign  =    "";
            endpoint = "";
        }

    public String getToken() {
        return token;
    }

    public String getSign() {
        return sign;
    }
        
	private String invoke_wsaa (byte [] LoginTicketRequest_xml_cms) throws Exception {
		
	    String LoginTicketResponse = null;
	    try {
			  
		Service service = new Service();
	        Call call = (Call) service.createCall();
		
		//
		// Prepare the call for the Web service
		//
		call.setTargetEndpointAddress( new java.net.URL(endpoint) );
		call.setOperationName("loginCms");
		call.addParameter( "request", XMLType.XSD_STRING, ParameterMode.IN );
		call.setReturnType( XMLType.XSD_STRING );
						//
		// Make the actual call and assign the answer to a String
		//
		LoginTicketResponse = (String) call.invoke(new Object [] { 
				Base64.encode (LoginTicketRequest_xml_cms) } );

		} catch (Exception e) {
			e.printStackTrace();
		}        
		return (LoginTicketResponse);
    }

		//
		// Create the CMS Message
		//
    public byte [] create_cms (String p12file, String p12pass, String signer, String dstDN, String service, Long TicketTime) {

	//PrivateKey pKey = null;
        PrivateKey pKey = null;
	X509Certificate pCertificate = null;
	byte [] asn1_cms = null;
	CertStore cstore = null;
	String LoginTicketRequest_xml;
	String SignerDN = null;
	//
	// Manage Keys & Certificates
	//
        try {
	    // Create a keystore using keys from the pkcs#12 p12file
	    KeyStore ks = KeyStore.getInstance("pkcs12");        
	    FileInputStream p12stream = new FileInputStream ( p12file ) ;
	    ks.load(p12stream, p12pass.toCharArray());            
	    p12stream.close();

	    // Get Certificate & Private key from KeyStore
	    //pKey = (PrivateKey) ks.getKey(signer, p12pass.toCharArray());           
            pKey = (PrivateKey) ks.getKey(signer, p12pass.toCharArray());           
	    pCertificate = (X509Certificate)ks.getCertificate(signer);
	    SignerDN = pCertificate.getSubjectDN().toString();

	    // Create a list of Certificates to include in the final CMS
	    ArrayList<X509Certificate> certList = new ArrayList<X509Certificate>();
	    certList.add(pCertificate);

	    if (Security.getProvider("BC") == null) {
		Security.addProvider(new BouncyCastleProvider());
	    }

	    cstore = CertStore.getInstance("Collection", new CollectionCertStoreParameters (certList), "BC");
	    } catch (Exception e) {
		e.printStackTrace();
	    } 
            //
	    // Create XML Message
	    // 
	    LoginTicketRequest_xml = create_LoginTicketRequest(SignerDN, dstDN, service, TicketTime);
		
	    //
	    // Create CMS Message
	    //
	    try {
		// Create a new empty CMS Message
		CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

		// Add a Signer to the Message
		gen.addSigner(pKey, pCertificate, CMSSignedDataGenerator.DIGEST_SHA1);

		// Add the Certificate to the Message
	      	gen.addCertificatesAndCRLs(cstore);

		// Add the data (XML) to the Message
		CMSProcessable data = new CMSProcessableByteArray(LoginTicketRequest_xml.getBytes());

		// Add a Sign of the Data to the Message
		CMSSignedData signed = gen.generate(data, true, "BC");	

		// 
		asn1_cms = signed.getEncoded();
	    } 
	    catch (Exception e) {
		e.printStackTrace();
	    } 
		
	    return (asn1_cms);
}

//
// Create XML Message for AFIP wsaa
// 	
public String create_LoginTicketRequest (String SignerDN, String dstDN, String service, Long TicketTime) {
	String LoginTicketRequest_xml;

	Date GenTime = new Date();
	GregorianCalendar gentime = new GregorianCalendar();
	GregorianCalendar exptime = new GregorianCalendar();
	String UniqueId = new Long(GenTime.getTime() / 1000).toString();			
	exptime.setTime(new Date(GenTime.getTime()+TicketTime));		
	XMLGregorianCalendarImpl XMLGenTime = new XMLGregorianCalendarImpl(gentime);
	XMLGregorianCalendarImpl XMLExpTime = new XMLGregorianCalendarImpl(exptime);	
	LoginTicketRequest_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
				+"<loginTicketRequest version=\"1.0\">"
				+"<header>"
				+"<source>" + SignerDN + "</source>"
				+"<destination>" + dstDN + "</destination>"
				+"<uniqueId>" + UniqueId + "</uniqueId>"
				+"<generationTime>" + XMLGenTime + "</generationTime>"
				+"<expirationTime>" + XMLExpTime + "</expirationTime>"
				+"</header>"
				+"<service>" + service + "</service>"
				+"</loginTicketRequest>";
			
	//System.out.println("TRA: " + LoginTicketRequest_xml);
		
	return (LoginTicketRequest_xml);
}
 public boolean conectar_wsaa() throws FileNotFoundException, ParseException {	

	String LoginTicketResponse = null;        
	System.setProperty("http.proxyHost", "");
	System.setProperty("http.proxyPort", "80");
        boolean seconecto = false;
				
	// Leer archivo de propiedades "wsaa_client.properties" en el objeto config
	Properties config = new Properties();
		
	try {
	    config.load(new FileInputStream("C:/CERTIF/wsaa_client.properties"));
        } catch (Exception e) {
	    e.printStackTrace();
        } 
		
	endpoint = config.getProperty("endpoint","http://wsaahomo.afip.gov.ar/ws/services/LoginCms");          
	String service  = config.getProperty("service","test");
	String dstDN    = config.getProperty("dstdn","cn=wsaahomo,o=afip,c=ar,serialNumber=CUIT 33693450239");
	
        
	String p12file  = config.getProperty("keystore","test-keystore.p12");
	String signer   = config.getProperty("keystore-signer","scazzola");
	String p12pass  = config.getProperty("keystore-password","123456");
		
	// Set proxy system vars
	//System.setProperty("http.proxyHost", config.getProperty("http_proxy",""));
	//System.setProperty("http.proxyPort", config.getProperty("http_proxy_port",""));
	//System.setProperty("http.proxyUser", config.getProperty("http_proxy_user",""));
	//System.setProperty("http.proxyPassword", config.getProperty("http_proxy_password",""));
		
	// Set the keystore used by SSL
	System.setProperty("javax.net.ssl.trustStore", config.getProperty("trustStore",""));
	System.setProperty("javax.net.ssl.trustStorePassword",config.getProperty("trustStore_password","")); 
		
	Long TicketTime = new Long(config.getProperty("TicketTime","36000"));
	if (!verificarTicketAcceso()){ // si expiró o no existe el ticket de acceso : pedirlo a la afip y guardarlo                    
	   // Create LoginTicketRequest_xml_cms
	   byte [] LoginTicketRequest_xml_cms = create_cms(p12file, p12pass, 
				signer, dstDN, service, TicketTime);
			
	   // Invoke AFIP wsaa and get LoginTicketResponse
	   try {
	      LoginTicketResponse = invoke_wsaa ( LoginTicketRequest_xml_cms );
	   } catch (Exception e) {
              JOptionPane.showMessageDialog(null, "Error al intentar obtener el ticket de acceso : "+e.getMessage());
	      // e.printStackTrace();
	   }		
	   // Get token & sign from LoginTicketResponse
	   try {           
	      Reader tokenReader = new StringReader(LoginTicketResponse);         
	      Document tokenDoc  = new SAXReader(false).read(tokenReader);           			
	      token  = tokenDoc.valueOf("/loginTicketResponse/credentials/token");
	      sign   = tokenDoc.valueOf("/loginTicketResponse/credentials/sign");	      
              grabarTicketAcceso(tokenDoc);// guardar el TA recibido al archivo ticket
           } catch (Exception e) {
              JOptionPane.showMessageDialog(null, "Error al obtener TOKEN+SIGN :"+e.getMessage());	 	
           }  
        }  // si verificarTicketAcceso = True, token y sign tienen valores no nulos
        if (token != null && sign !=null){	                  
           JOptionPane.showMessageDialog(null,"Conexión a la AFIP realizada con éxito","ATENCION!",JOptionPane.INFORMATION_MESSAGE);                      
           seconecto = true;
        } else {
            JOptionPane.showMessageDialog(null,"No se ha podido obtener el Ticket de Acceso","ATENCION!",JOptionPane.INFORMATION_MESSAGE);                      
            seconecto = false;
        }
	return seconecto;
  }
  
  private boolean verificarTicketAcceso() throws FileNotFoundException, ParseException{
      // Verifica que el ticket de acceso "ticket" en c:\certif no este vencido
      boolean retorno = false; //true -> ticket valido; false -> ticket invalido
      String  fecexp; // fecha de expiracion del ticket
      File archivo = null;
      FileReader fr = null;
      BufferedReader br = null;
      Document ticketxml = null;
      SimpleDateFormat  ffecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date hoy      = new Date();
      Date fechaexp = new Date();      
      try {         
         archivo = new File ("C:/CERTIF/ticket.txt");
         fr = new FileReader (archivo);
         br = new BufferedReader(fr);
         ticketxml = new SAXReader(false).read(br); //lee el archivo en el Doc.XML
      } catch(Exception e){
          JOptionPane.showMessageDialog(null, "Error al abrir Archivo de Ticket "+e.getMessage());         
          retorno = false;
      }finally{      
         try{                    
            if( null != fr ){   
               fr.close();     
            }                  
         }catch (Exception e2){ 
            e2.printStackTrace();
            retorno = false;
         }
      }
      // Extraigo token, sign y fecha de expiración
      token  = ticketxml.valueOf("/loginTicketResponse/credentials/token");
      sign   = ticketxml.valueOf("/loginTicketResponse/credentials/sign");
      fecexp = ticketxml.valueOf("/loginTicketResponse/header/expirationTime");         
      fechaexp = ffecha.parse(fecexp.substring(0, 19).replace("T", " "));
      if (fechaexp.after(hoy)) { // fechaexp > fecha de hoy -> ticket no vencido
          retorno = true;
      } else {
          retorno = false;
      }
      
      return retorno;
  }
  
  private void grabarTicketAcceso(Document tokend){
      // Graba el documento xml "tokend" al archivo ticket.txt
      try {
            String ruta = "C:/CERTIF/ticket.txt";
            String contenido = "Contenido de ejemplo";
            File file = new File(ruta);
            // Si el archivo no existe es creado
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            tokend.write(bw);  // graba el XML al archivo ticket.txt            
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
  }
	
}
