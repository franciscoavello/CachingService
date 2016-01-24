package cachingservice;

import java.io.Serializable;
import java.util.ArrayList;

public class IndexInvertido implements Serializable {
    
    String palabra;
    ArrayList<ClaveValorDatos> docFrec;

    public String getPalabra() {
        return palabra;
    }

    public void setPalabra(String palabra) {
        this.palabra = palabra;
    }

    public ArrayList<ClaveValorDatos> getDocFrec() {
        return docFrec;
    }

    public void setDocFrec(ArrayList<ClaveValorDatos> docFrec) {
        this.docFrec = docFrec;
    }

    public IndexInvertido(String palabra, ArrayList<ClaveValorDatos> docFrec) {
        this.palabra = palabra;
        this.docFrec = docFrec;
    }  
    
}