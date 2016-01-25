package cachingservice;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;



public class CachingService extends Thread{
    
    static String ipFrontService="localhost";
    
    static ArrayList<IndexInvertido> consultas = new ArrayList<IndexInvertido>();
    
    public static IndexInvertido getEntry(String query) {
        for (int i = 0; i < consultas.size(); i++) {            
            if (consultas.get(i).palabra.equals(query)) {
                return consultas.get(i);
            }
        }
        return null;
    }
    
    public static int hashParticionCache(String consulta, int numParticiones){
        int resParticion=0;
        for ( int i = 0; i < consulta.length(); ++i ) {
            char c = consulta.charAt( i );
            int j = (int) c;
            resParticion=resParticion+j;
        }
        return (resParticion/consulta.length())%numParticiones;
    }
    
    // Para enviar al FrontService
    
    public static void socketClienteDesdeCachingServiceHaciaFrontService(String respuestaAFrontService) throws Exception{        
        //Socket para el cliente (host, puerto)
        Socket socketHaciaFrontService = new Socket(ipFrontService, 5002);
        
        //Buffer para enviar el dato al server
        DataOutputStream haciaElFrontService = new DataOutputStream(socketHaciaFrontService.getOutputStream());
        
        haciaElFrontService.writeBytes(respuestaAFrontService + '\n');
        
        socketHaciaFrontService.close();  
    }
    
    // Para recibir desde el FrontService
    
    public static void socketServidorCachingServiceParaFrontService(int numParticiones, CacheLRU[] particionesCache) throws Exception{    
        
        //Variables
        String desdeFrontService;
        String respuestaAFrontService="";    
        
        //Socket para el servidor en el puerto 5000
        ServerSocket socketDesdeFrontService = new ServerSocket(5001);
        
        while(true){
            //Socket listo para recibir 
            Socket connectionSocket = socketDesdeFrontService.accept();
            //Buffer para recibir desde el cliente
            BufferedReader inDesdeFrontService = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            //Buffer para enviar al cliente
            DataOutputStream haciaElFrontService = new DataOutputStream(connectionSocket.getOutputStream());
            
            //Recibimos el dato del cliente y lo mostramos en el server
            desdeFrontService =inDesdeFrontService.readLine();
            
            String[] tokens = desdeFrontService.split(" ");
            String metodoHTTP = tokens[0];
            String[] tokens_parametros = tokens[1].split("/");
            System.out.println("-----------------------------------------------------------------");
            System.out.println("(Caching Service) Recibí la consulta HTTP desde el Front Service:");           
            System.out.println("(Caching Service) Consulta: " + tokens_parametros[2]);
            System.out.println("(Caching Service) Metodo HTTP: " + metodoHTTP);
            System.out.println("(Caching Service) Resource: " + tokens_parametros[1]);
            System.out.println("-----------------------------------------------------------------");
            String consulta = tokens_parametros[2].replaceAll("-", " ");      
            String[] palabrasConsulta = consulta.split(" ");
            String paraEnviar = new String();
            for(int k=0; k<palabrasConsulta.length;k++){
                consulta=palabrasConsulta[k];
                int particionAEnviar= hashParticionCache(consulta,numParticiones);
                System.out.println("Enviando la consulta '"+ consulta + "' a la particion numero " + particionAEnviar + " del cache");
                if(particionesCache[particionAEnviar].revisarCacheEstatico(consulta)!= null){
                    System.out.println("(Particion "+ particionAEnviar + " Cache) HIT! en el cache estatico.");
                    /*if(k==0){
                        paraEnviar= new String();
                    }*/
                    for (int i = 0; i < particionesCache[particionAEnviar].revisarCacheEstatico(consulta).docFrec.size(); i++) {
                        System.out.println("--------------------");
                        System.out.println("Está en el documento: " + particionesCache[particionAEnviar].revisarCacheEstatico(consulta).docFrec.get(i).idDocumento);
                        System.out.println("Con una frecuencia de: " + particionesCache[particionAEnviar].revisarCacheEstatico(consulta).docFrec.get(i).frecuencia);
                        System.out.println("En la URL de wikipedia: " + particionesCache[particionAEnviar].revisarCacheEstatico(consulta).docFrec.get(i).URL);
                        paraEnviar = paraEnviar.concat(particionesCache[particionAEnviar].revisarCacheEstatico(consulta).docFrec.get(i).idDocumento);
                        paraEnviar = paraEnviar.concat("#");
                        paraEnviar = paraEnviar.concat(particionesCache[particionAEnviar].revisarCacheEstatico(consulta).docFrec.get(i).frecuencia.toString());
                        paraEnviar = paraEnviar.concat("#");
                        paraEnviar = paraEnviar.concat(particionesCache[particionAEnviar].revisarCacheEstatico(consulta).docFrec.get(i).URL);
                        if(i != particionesCache[particionAEnviar].revisarCacheEstatico(consulta).docFrec.size()-1){
                            paraEnviar = paraEnviar.concat(",");
                        }                    
                    }
                    if(k==palabrasConsulta.length-1){
                        socketClienteDesdeCachingServiceHaciaFrontService(paraEnviar);
                    }
                    else{
                        paraEnviar = paraEnviar.concat(",");
                    }                    
                }            
                else{
                    System.out.println("(Particion "+ particionAEnviar + " Cache) MISS! en el cache estatico");
                    IndexInvertido resultadoCacheDinamico = particionesCache[particionAEnviar].revisarCacheDinamico(consulta);
                    if (resultadoCacheDinamico == null) { // MISS
                        System.out.println("(Particion "+ particionAEnviar + " Cache) MISS! en el cache dinamico");    
                        socketClienteDesdeCachingServiceHaciaFrontService("MISS!");
                        socketServidorCachingServiceParaIndexService(numParticiones, particionesCache);
                        break;          
                    }else{
                        System.out.println("(Particion "+ particionAEnviar + " Cache) HIT! en el cache dinamico");
                       // String paraEnviar= new String();
                        for (int i = 0; i < resultadoCacheDinamico.docFrec.size(); i++) {
                            System.out.println("--------------------");
                            System.out.println("Está en el documento: " + resultadoCacheDinamico.docFrec.get(i).idDocumento);
                            System.out.println("Con una frecuencia de: " + resultadoCacheDinamico.docFrec.get(i).frecuencia);
                            System.out.println("En la URL de wikipedia: " + resultadoCacheDinamico.docFrec.get(i).URL);
                            paraEnviar = paraEnviar.concat(resultadoCacheDinamico.docFrec.get(i).idDocumento);
                            paraEnviar = paraEnviar.concat("#");
                            paraEnviar = paraEnviar.concat(resultadoCacheDinamico.docFrec.get(i).frecuencia.toString());
                            paraEnviar = paraEnviar.concat("#");
                            paraEnviar = paraEnviar.concat(resultadoCacheDinamico.docFrec.get(i).URL);
                            if(i != resultadoCacheDinamico.docFrec.size()-1){
                                paraEnviar = paraEnviar.concat(",");
                            }                    
                        }
                        if(k==palabrasConsulta.length-1){
                            socketClienteDesdeCachingServiceHaciaFrontService(paraEnviar);
                        }
                        else{
                            paraEnviar = paraEnviar.concat(",");
                        }   
                        //socketClienteDesdeCachingServiceHaciaFrontService(paraEnviar);
                    }
                    }
                }
        }
    }
      
    // Para recibir desde el IndexService
    
    public static void socketServidorCachingServiceParaIndexService(int numParticiones, CacheLRU[] particionesCache) throws Exception{    
        
        //Variables
        String desdeIndexService;        
        //Socket para el servidor en el puerto 5000
        ServerSocket socketDesdeIndexService = new ServerSocket(5005);
        
        //Socket listo para recibir 
        Socket connectionSocket = socketDesdeIndexService.accept();
        //Buffer para recibir desde el cliente
        BufferedReader inDesdeIndexService = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        //Buffer para enviar al cliente
            
        //Recibimos el dato del cliente y lo mostramos en el server
        desdeIndexService =inDesdeIndexService.readLine();
        System.out.println("Recibido desde Index Service para agregar al caché" + desdeIndexService);
        if(!desdeIndexService.equals("NO")){            
            System.out.println("Añadiendo al caché dinámico");
            String[] tokens = desdeIndexService.split(",");
            System.out.println("Hay que agregar " + tokens[tokens.length-1] + " palabras");
            int recorre=0;
            int contador=0;
            for(int k=0;k<Integer.parseInt(tokens[tokens.length-1]);k++){
                String palabra = tokens[contador];
                contador++;
                ArrayList<ClaveValorDatos> clavVal = new ArrayList<>();                 
                while(!tokens[contador].equals("*")){
                    String[] tokensDocs = tokens[contador].split("#");
                    System.out.println(tokensDocs[0] + "assada");
                    ClaveValorDatos docFrecValor = new ClaveValorDatos(tokensDocs[0],Integer.parseInt(tokensDocs[1]),tokensDocs[2]);                    
                    clavVal.add(docFrecValor);
                    contador++;                    
                }
                contador++;
                /*
                for(int i=0; i<tokens.length-1;i++){
                    String[] tokensDocs = tokens[i].split("#");
                    ClaveValorDatos docFrecValor = new ClaveValorDatos(tokensDocs[0],Integer.parseInt(tokensDocs[1]),tokensDocs[2]);            
                    clavVal.add(docFrecValor);            
                }*/
                IndexInvertido agregar = new IndexInvertido(palabra, clavVal);
                int particionaEnviar = hashParticionCache(palabra, numParticiones);
                System.out.println("La palabra "+ palabra + " va a la particion " + particionaEnviar);
                particionesCache[particionaEnviar].cacheDinamico.add(agregar);
                System.out.println("Añadido al caché dinámico");
            }
        }
        socketDesdeIndexService.close();
    }
    
    public static void main(String args[]) throws Exception{
        File archivo = new File ("entrada.txt");
        FileReader fr = new FileReader (archivo);
        BufferedReader br = new BufferedReader(fr);
        String linea = br.readLine();
        int cantRespuestas= Integer.parseInt(linea);        
        linea = br.readLine();
        int tamCache=Integer.parseInt(linea);
        linea = br.readLine();
        int numParticiones=Integer.parseInt(linea);
        fr.close();
        if(numParticiones<tamCache){
            if(tamCache%numParticiones==0){
                System.out.println("Auto generando preguntas y respuestas...");
                for(int i=0; i<cantRespuestas; i++){
                    ClaveValorDatos docFrecValor = new ClaveValorDatos("1", 1,"http://www.google.cl");
                    ClaveValorDatos docFrecValor2 = new ClaveValorDatos("2", 3, "http://www.google.cl");
                    ArrayList<ClaveValorDatos> clavVal = new ArrayList<>();
                    clavVal.add(docFrecValor);
                    clavVal.add(docFrecValor2);
                    IndexInvertido agregar = new IndexInvertido("palabra"+(i+1), clavVal);
                    consultas.add(agregar);
                }
                System.out.println("Preguntas y respuestas generadas."); 
                CacheLRU[] particionesCache = new CacheLRU[numParticiones];
                for(int i=0; i<numParticiones;i++){
                    particionesCache[i] = new CacheLRU(tamCache/numParticiones);
                    for(int j=0; j<consultas.size(); j++){
                        if (hashParticionCache(consultas.get(j).palabra,numParticiones)==i){
                            particionesCache[i].cacheEstatico.add(consultas.get(j));
                            break;
                        }
                    }                    
                }      
                socketServidorCachingServiceParaFrontService(numParticiones,particionesCache);
            }
            else{
                System.out.println("Los tamaños de cache y numero de particiones no son multiplos entre si. Ingrese valores correctos en el archivo de entrada");
            }         
        }
        else{
            System.out.println("El numero de particiones es menor al tamaño del cache. Ingrese valores correctos en el archivo de entrada");
        }
        
    }
    
}
