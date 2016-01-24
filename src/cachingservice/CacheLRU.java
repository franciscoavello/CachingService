package cachingservice;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheLRU extends Thread{
    
    int tamaño;
    ArrayList<IndexInvertido> cacheEstatico;
    ArrayList<IndexInvertido> cacheDinamico;
    
    public CacheLRU(int size) {
        this.tamaño = size;
        this.cacheEstatico = new ArrayList<>();
        this.cacheDinamico = new ArrayList<>();
    }
    
    public IndexInvertido revisarCacheEstatico(String query) {
        IndexInvertido result = null;
        for (int i = 0; i < cacheEstatico.size(); i++) {
            if(cacheEstatico.get(i).palabra.equals(query)){
                result = cacheEstatico.get(i);
                return result;
            }
        }
        return result;
    }
    
    public IndexInvertido revisarCacheDinamico(String query) {
        IndexInvertido result = null;
        for (int i = 0; i < cacheDinamico.size(); i++) {
            if(cacheDinamico.get(i).palabra.equals(query)){
                result = cacheDinamico.get(i);
            }
        }        
        if(result != null) {
            cacheDinamico.remove(result);
            cacheDinamico.add(result);
        }
        return result;
    }
    
    public void addEntryToCache(IndexInvertido query) {
        if (cacheDinamico.contains(query)) { // HIT
            // Bring to front
            cacheDinamico.remove(query);
            cacheDinamico.add(query);
        } else { // MISS
            if(cacheDinamico.size() == this.tamaño) {
                IndexInvertido first_element = cacheDinamico.get(0);
                System.out.println("Removiendo: '" + first_element.palabra + "'");
                cacheDinamico.remove(first_element);
            }
            cacheDinamico.add(query);
        }
    }
    
     @Override
    public void run(){        
        try {
            System.out.println("(Cache)("+ getName() +") Soy una particion del cache");
        } catch (Exception ex) {
            Logger.getLogger(CacheLRU.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
