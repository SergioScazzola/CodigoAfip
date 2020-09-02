package afiputilidades;


import clases.dto.CaeDTO; // clases propias del sistema : no se incluyen
import clases.dto.VentaDTO;//clases propias del sistema : no se incluyen
import clases.jdbc.CaeDaoJDBC;//clases propias del sistema : no se incluyen
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import fev1.dif.afip.gov.ar.FECAEAGetResponse;
import fev1.dif.afip.gov.ar.FECAEResponse;
import fev1.dif.afip.gov.ar.CbteTipoResponse;
import fev1.dif.afip.gov.ar.ConceptoTipoResponse;
import fev1.dif.afip.gov.ar.DocTipoResponse;
import fev1.dif.afip.gov.ar.DummyResponse;
import fev1.dif.afip.gov.ar.FEAuthRequest;
import fev1.dif.afip.gov.ar.FECAECabRequest;
import fev1.dif.afip.gov.ar.ArrayOfFECAEDetRequest;
import fev1.dif.afip.gov.ar.ArrayOfAlicIva;
import fev1.dif.afip.gov.ar.AlicIva;
import fev1.dif.afip.gov.ar.ArrayOfErr;
import fev1.dif.afip.gov.ar.ArrayOfFECAEDetResponse;
import fev1.dif.afip.gov.ar.ArrayOfObs;
import fev1.dif.afip.gov.ar.Err;
import fev1.dif.afip.gov.ar.FECAEDetRequest;
import fev1.dif.afip.gov.ar.FECAEAGetResponse;
import fev1.dif.afip.gov.ar.FECAEARequest;
import fev1.dif.afip.gov.ar.FECAEAResponse;
import fev1.dif.afip.gov.ar.FECAEASinMovConsResponse;
import fev1.dif.afip.gov.ar.FECAEASinMovResponse;
import fev1.dif.afip.gov.ar.FECAEDetResponse;
import fev1.dif.afip.gov.ar.FECAERequest;
import fev1.dif.afip.gov.ar.FECAEResponse;
import fev1.dif.afip.gov.ar.FECAESolicitar;
import fev1.dif.afip.gov.ar.FECompConsultaReq;
import fev1.dif.afip.gov.ar.FECompConsultaResponse;
import fev1.dif.afip.gov.ar.FECompUltimoAutorizado;
import fev1.dif.afip.gov.ar.FECompUltimoAutorizadoResponse;
import fev1.dif.afip.gov.ar.FECotizacionResponse;
import fev1.dif.afip.gov.ar.FEPaisResponse;
import fev1.dif.afip.gov.ar.FEPtoVentaResponse;
import fev1.dif.afip.gov.ar.FERecuperaLastCbteResponse;
import fev1.dif.afip.gov.ar.FERegXReqResponse;
import fev1.dif.afip.gov.ar.FETributoResponse;
import fev1.dif.afip.gov.ar.IvaTipoResponse;
import fev1.dif.afip.gov.ar.MonedaResponse;
import fev1.dif.afip.gov.ar.OpcionalTipoResponse;
import fev1.dif.afip.gov.ar.Service;
import java.sql.SQLException;




import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import javax.xml.soap.SOAPEnvelope;
import org.apache.axis.client.Call;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

/* @Autor Sergio Scazzola
*  e-mail : scazzolasergio@gmail.com
*
*  Este módulo implementa el acceso a los servicios Web de la AFIP
*  fecaeSolicitar y feCompUltimoAutorizado. Para lo cual se deben setear 
*  previamente las variables token y sign resultado del proceso de Autorizacion
*  que implementa el modulo AfipAuth.

*  Como se puede ver este módulo hace referencia a servicios Web y clases 
*  que deben generarse previamente en su proyecto  de la siguiente manera (NetBeans) : 
*  boton derecho en su proyecto -> New -> Web Service Client -> Elegir WSDL URL y tipear
*  la siguiente url : https://wswhomo.afip.gov.ar/wsfev1/service.asmx?WSDL. Le dan aceptar
*  y ésto deberia generarles todos los servicios y clases que se encuentran especificados 
*  en este archivo WSDL y que se utilizan (algunos) en este módulo.
*  Para utilizar un servicio como por ejemplo : fecaeSolicitar, Botón derecho en archivo .java +
*  insertar código + Call Web Service Operation y se elije el servicio a utilizar.
*  Para utilizar los servicios hay que instanciar objetos de las clases especificadas en los
*  servicios como se muestra en los métodos correspondientes. Esto nos permite no tener que 
*  generar el XML de requerimiento como en el módulo AfipAuth. Consulten los métodos, creo que 
*  está bastante claro para no requerir mas explicación. Cualquier cosa me escriben un e-mail.
*  
*  Estos dos Modulos (AfipAuth y AfipServicios) funcionan perfecto en modo Homologacion, todavia
*  no los he probado en producción pero si no funcionan será por cuestiones de cerificados u otros
*  otras yerbas. Para probar los servicios fuera de una aplicación, se pueden instalar una 
*  herramienta como SOAPUI o ReadyApi que les va a permitir probar los servicios llenando 
*  el XML a mano. Yo no lo probé con la autenticación, los probé copiando y pegando el token
*  y el sign y llamando a FECAESolicitar (me sirvió muchisimo). 
*  Creo que tambien se puede probar con la autenticación.

*  Se incluye el archivo "wsaa_client.properties" (parametros de conexion)
*  Ejemplo de archivo "ticket.txt" con los datos de una conexion
*/

public class AfipServicios {
  
  private String      token;
  private String      sign;  
  private String      cae;
  private Date        fvtocae;
  private String      ptoventa;
  private CaeDTO      caedto;
  private String      cuitemisor; // cuit de la Empresa que emite comprobantes
  
  
  public AfipServicios(){
      token      = ""; // Se genera en AfipAuth y se guarda aqui para solicitar CAE y demás
      sign       = ""; // idem anterior     
      cae        = ""; // Valor del CAE obtenido
      fvtocae    = null; // Fecha del vencimiento de CAE
      cuitemisor = "20200351097"; // cuit emisor
      ptoventa   = "1";      // punto de venta
  }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }


// Servicio Web de solicitud de CAE, generado automaticamente por :
// Botón derecho + insertar código + Call Web Service Operation
private static FECAEResponse fecaeSolicitar(fev1.dif.afip.gov.ar.FEAuthRequest auth, fev1.dif.afip.gov.ar.FECAERequest feCAEReq) {
        fev1.dif.afip.gov.ar.Service service = new fev1.dif.afip.gov.ar.Service();
        fev1.dif.afip.gov.ar.ServiceSoap port = service.getServiceSoap12();
        return port.fecaeSolicitar(auth, feCAEReq);
}
// Servicio Web de Ultimo comprobante autorizado, generado automaticamente por :
// Botón derecho + insertar código + Call Web Service Operation
private static FERecuperaLastCbteResponse feCompUltimoAutorizado(fev1.dif.afip.gov.ar.FEAuthRequest auth, int ptoVta, int cbteTipo) {
        fev1.dif.afip.gov.ar.Service service = new fev1.dif.afip.gov.ar.Service();
        fev1.dif.afip.gov.ar.ServiceSoap port = service.getServiceSoap12();
        return port.feCompUltimoAutorizado(auth, ptoVta, cbteTipo);
}

private double redondearDecimales(double valorInicial, int numeroDecimales) {
        double parteEntera, resultado;
        resultado = valorInicial;
        parteEntera = Math.floor(resultado);
        resultado=(resultado-parteEntera)*Math.pow(10, numeroDecimales);
        resultado=Math.round(resultado);
        resultado=(resultado/Math.pow(10, numeroDecimales))+parteEntera;
        return resultado;
    }

private int determinarTipoComp(String letr, String tcomp){
    
int compro = 0;
if (letr.equals("A") && tcomp.equals("FAC")){
        compro = 1; 
     }
if (letr.equals("A") && tcomp.equals("NDC")){
        compro = 3; 
}
if (letr.equals("B") && tcomp.equals("FAC")){
        compro = 6; 
}
if (letr.equals("B") && tcomp.equals("NDC")){
        compro = 8; 
}
if (letr.equals("A") && tcomp.equals("NDB")){
        compro = 2; 
}
if (letr.equals("B") && tcomp.equals("NDB")){
        compro = 7; 
}
return compro;
}
public int FECAEUltimoAutorizado(String letra, String tipocomp){
    // Obtiene el ultimo comprobante autorizado de por ej. FAC A, NDC B, etc
    int retorno = 0;
    FECompUltimoAutorizado fecompua = new FECompUltimoAutorizado();
    FEAuthRequest feauth = new FEAuthRequest();
    feauth.setToken(token);
    feauth.setSign(sign);
    long cuitemi = Long.parseLong(cuitemisor);
    feauth.setCuit(cuitemi);
    
    FERecuperaLastCbteResponse fuaresp = new FERecuperaLastCbteResponse();    
    // LLama al Servicio Web  
    fuaresp = feCompUltimoAutorizado(feauth,Integer.parseInt(ptoventa),determinarTipoComp(letra,tipocomp));        
    ArrayOfErr errores = fuaresp.getErrors();
    if (errores != null){ //hubo error
        Err error = (Err) errores.getErr().get(0);        
        JOptionPane.showMessageDialog(null,"Error "+error.getCode()+" - "+error.getMsg());
        retorno = 0;   
    } else { // no hubo error      
      String[] mensa = new String[3];
      mensa[0] = "Ultimo Comprobante Autorizado : "+fuaresp.getCbteNro();
      mensa[1] = "Pto.Venta : "+fuaresp.getPtoVta();
      mensa[2] = "Tipo Comp.: "+fuaresp.getCbteTipo();
      JOptionPane.showMessageDialog(null,mensa);
      retorno = fuaresp.getCbteNro();
    }
    return retorno;
}
public boolean FECAESolicitar (VentaDTO venta) throws SQLException {      
     // Genera  los objetos feauth y fereq para llamar al servicio Web fecaeSolicitar
     // obtiene la respuesta de tipo FECAEResponse la cual contiene el CAE y la Fecha 
     // de vencimiento si todo salió bien y si no devuelve el error producido
     // El objeto VentaDTO contiene todos los datos del comprobante a autorizar (propio 
     // de la implementacion)
     // Se supone que previamente se obtuvo el token  y sign (Autorizacion modulo AfipAuth)     
     boolean retorno       = false;
     int tipodoc   = 0;
     String ncuit  = venta.getCuit().replace("-","");
     long nrocuit   = 0;

     SimpleDateFormat  ffecha = new SimpleDateFormat("yyyyMMdd");
     SimpleDateFormat  f2     = new SimpleDateFormat("dd/MM/yyyy");
     DecimalFormat        fimporte;
     DecimalFormatSymbols simbolo = new DecimalFormatSymbols();
     simbolo.setDecimalSeparator('.');        
     fimporte         = new DecimalFormat("#######0.00",simbolo);   
     long nrocomp = 0;
     
     if (venta.getLetra().equals("A")){
             tipodoc= 80;
             nrocuit  =  Long.parseLong(ncuit);
             nrocomp  = venta.getNumero() - 1000000066;
     } else {
       if  ( venta.getLetra().equals("B")  &&  venta.getCuit().trim().length() == 13){
           tipodoc = 80;
           nrocuit  =  Long.parseLong(ncuit);
           nrocomp  = venta.getNumero() - 1000000030;
       } else {
           if (venta.getCuit().trim().isEmpty()){
               tipodoc = 99;
           } else {
               if (venta.getCuit().trim().length()<13){
                   tipodoc = 96;
                   nrocuit  =  Long.parseLong(ncuit);
               } else {
                   tipodoc = 80;                   
                   nrocuit  =  Long.parseLong(ncuit);
               }
           }
       }
     }
          
     //nrocomp    = 14;  venta.getNroStr();
     String strfecha = ffecha.format(venta.getFecha());
     double totfac   = redondearDecimales(venta.getTotal(),2);
     double sub20    = redondearDecimales(venta.getSubt20(),2);
     double sub10    = redondearDecimales(venta.getSubt10(),2);
     double subtfac  = redondearDecimales(venta.getSubtotal(),2);
     double iva10    = redondearDecimales(venta.getImpiva10(),2);
     double iva21    = redondearDecimales(venta.getImpiva20(),2);
     double totiva   = redondearDecimales(venta.getImpiva10()+venta.getImpiva20(),2);
     
     
     String puntoventa = ptoventa; 
     int compro = determinarTipoComp(venta.getLetra(),venta.getTcomp());
     
     FEAuthRequest feauth = new FEAuthRequest();
     feauth.setToken(token);
     feauth.setSign(sign);
     long cuitemi = Long.parseLong(cuitemisor);
     feauth.setCuit(cuitemi);
     
     FECAERequest fereq = new FECAERequest();// cuerpo del req
     FECAECabRequest cabreq = new FECAECabRequest();// cabecera del reg
     cabreq.setCantReg(1);
     cabreq.setCbteTipo(compro);
     cabreq.setPtoVta(1);     
    
     FECAEDetRequest objdet = new FECAEDetRequest();// Detalle del request
     objdet.setConcepto(1);
     objdet.setDocTipo(tipodoc);
     objdet.setDocNro(nrocuit);
     objdet.setCbteDesde(nrocomp);
     objdet.setCbteHasta(nrocomp);
     objdet.setCbteFch(strfecha);
     objdet.setImpTotal(totfac);
     objdet.setImpTotConc(0d);
     objdet.setImpNeto(subtfac);
     objdet.setImpOpEx(0d);
     objdet.setImpTrib(0d);
     objdet.setImpIVA(totiva);
     objdet.setMonId("PES");
     objdet.setMonCotiz(1d);
     ArrayOfAlicIva losivas = new ArrayOfAlicIva();
     if (iva21 != 0){
        AlicIva ivao1 = new AlicIva();
        ivao1.setId(5);// iva 21%
        ivao1.setBaseImp(sub20);
        ivao1.setImporte(iva21);
        losivas.getAlicIva().add(ivao1);
     }
     if (iva10 != 0){
         AlicIva ivao2 = new AlicIva();
         ivao2.setId(4);// iva 10.5
         ivao2.setBaseImp(sub10);
         ivao2.setImporte(iva10);
         losivas.getAlicIva().add(ivao2);                        
     }
     objdet.setIva(losivas); // agrego los ivas
   
     ArrayOfFECAEDetRequest arrcaereq = new ArrayOfFECAEDetRequest();
     
     fereq.setFeDetReq(arrcaereq);
     fereq.getFeDetReq().getFECAEDetRequest().add(objdet);
     fereq.setFeCabReq(cabreq);
     
     FECAEResponse resp = fecaeSolicitar(feauth, fereq);// llamada a solicitar CAE!!!
     String obser = "";
     String resuu = "";
     if (resp!=null){ // Analizar respuesta
         ArrayOfErr errores = resp.getErrors();
         if (errores!=null){
             Err error = (Err) errores.getErr().get(0);
             ArrayOfObs obs = new ArrayOfObs();
             obs = resp.getFeDetResp().getFECAEDetResponse().get(0).getObservaciones();
             if (obs != null){
                 obser = obs.getObs().get(0).toString();
             };
             JOptionPane.showMessageDialog(null,"Error "+error.getCode()+" - "+error.getMsg());
             retorno = false;
         } else {
             FECAEDetResponse caee = new FECAEDetResponse();    
             caee = resp.getFeDetResp().getFECAEDetResponse().get(0);             
             resuu = resp.getFeDetResp().getFECAEDetResponse().get(0).getResultado();
             ArrayOfObs obs = new ArrayOfObs();
             obs = resp.getFeDetResp().getFECAEDetResponse().get(0).getObservaciones();
             if (obs != null){
                 obser = obs.getObs().get(0).toString();
             };
             if (caee.getCAE()!=null){                              
                 
                 cae      = caee.getCAE();         
                 int anio  = Integer.parseInt(caee.getCAEFchVto().substring(0,4));
                 int mes   = Integer.parseInt(caee.getCAEFchVto().substring(5,6));
                 int dia   = Integer.parseInt(caee.getCAEFchVto().substring(7,8));
                 Calendar cal = Calendar.getInstance();
                 cal.set(Calendar.YEAR, anio);
                 cal.set(Calendar.MONTH, mes-1);
                 cal.set(Calendar.DAY_OF_MONTH, dia);
                 fvtocae = cal.getTime();
                 JOptionPane.showMessageDialog(null,"CAE :"+caee.getCAE()+" - "+caee.getCAEFchVto());
                 GenerarCaeDTO(venta,resuu,obser,compro,caee.getCAEFchVto()); // Genera objeto DTO para grabar CAE
                 CaeDaoJDBC caedao = new CaeDaoJDBC();
                 if (caedao.insert_alta(caedto)==1){  // Graba el CAE obtenido a la tabla "cae"
                     JOptionPane.showMessageDialog(null,"CAE Grabado con éxito");
                     retorno = true;
                 } else {
                     JOptionPane.showMessageDialog(null,"No se pudo grabar el CAE :"+caee.getCAE());
                     retorno = false;
                 }
                 retorno = true;
             } else {
                 JOptionPane.showMessageDialog(null,"El CAE es nulo");
                 retorno = false;
             }
         }
             
    } else {
         JOptionPane.showMessageDialog(null,"No se pudo obtener respuesta de la AFIP");
         retorno = false;
     }

    
     return retorno;
}    
private void GenerarCaeDTO(VentaDTO vendto,String res,String obs,int comp,String fvto){
    // genera el objeto caedto que se utiliza para grabar en la tabla de CAES
    caedto  = new CaeDTO();
    caedto.setCuit(vendto.getCuit().replace("-", ""));
    caedto.setCae(cae);
    caedto.setLetra(vendto.getLetra());
    caedto.setTcomp(vendto.getTcomp());
    caedto.setNcomp(vendto.getNumero());
    caedto.setFecvto(fvtocae);
    caedto.setResul(res);
    caedto.setCodob("0");
    String codc = "0"+Integer.toString(comp).trim();
    caedto.setCodcomp(codc);
    caedto.setObser(obs);
    String pven = "0000".substring(0,4-ptoventa.length())+ptoventa;
    String acalcdig = cuitemisor+codc+pven+cae+fvto;
    String digver = CalcularDigito(acalcdig); // digito verif.del codigo de barra
    caedto.setCbar(acalcdig+digver);    
    caedto.setImporte(vendto.getTotal());
}
private String CalcularDigito (String cadena){
    // calcula el digito verificador para agregar a los 39 del Cod.de Barra
    // Se tomó el algoritmo que especifica la AFIP
    String retorno = "";
    int pares   = 0;      
    int impares = 0;
    String digito="";
    boolean esimpar = true; // la primera es impar
    int lcad = cadena.length();
    for (int i=1;i<=lcad;i++){  
      digito = cadena.substring(i-1,i);
      if (esimpar){              
          impares = impares + Integer.parseInt(digito);
          esimpar = false;
      } else {          
          pares = pares + Integer.parseInt(digito);
          esimpar = true;
      }
    }    
    int resultado = (impares * 3) + pares;
    int resu = (Math.floorDiv((resultado + 9),10) * 10) - resultado;
    retorno = Integer.toString(resu);
    return retorno;
}

 
 
}
  
