# CodigoAfip
Código Java para Autenticar y Utilizar Servicios Web de Afip

Este código se puede utilizar para acceder a los Web Services de la Afip en Argentina
y poder por ejemplo solicitar un CAE (Comprobante de Autorización Electronica) para
emitir una Factura Electrónica. Tambien se explica como se pueden utilizar los servicios
Web de cualquier sitio mediante la lectura del archivo WSDL. Para este ejemplo se utilizó
NetBeans 8.2 como IDE de Java.

El código de autorizacion está basado en el codigo que publicó la Afip para JAVA y que
debió adaptarse para su puesta en producción.
Dentro de cada modulo se incluye información detallada de como utilizarlos.
Si tienen alguna duda, no duden en escribirme un e-mail : scazzolasergio@gmail.com

Se trata de dos modulos : Un módulo (AfipAuth) afectua la autenticación y la autorización 
del servicios "wsfe" (facturacion electronica) obteniendo token y sign. El otro módulo
(AfipServicios) implementa el acceso al resto de los servicios relacionados con Facturacion Electronica.
Los módulos funcionan a la perfeccion en homologación y pueden ser utilizados con pocos 
cambios para producción.

