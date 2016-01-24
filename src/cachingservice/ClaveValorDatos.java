package cachingservice;

public class ClaveValorDatos implements Comparable<ClaveValorDatos>{
    
    String idDocumento;
    Integer frecuencia;
    String URL;

    public ClaveValorDatos(String idDocumento, Integer frecuencia, String URL) {
        this.idDocumento = idDocumento;
        this.frecuencia = frecuencia;
        this.URL = URL;
    }
        
    public String getIdDocumento() {
        return idDocumento;
    }

    public void setIdDocumento(String idDocumento) {
        this.idDocumento = idDocumento;
    }

    public Integer getFrecuencia() {
        return frecuencia;
    }

    public void setFrecuencia(Integer frecuencia) {
        this.frecuencia = frecuencia;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }
    
    @Override
    public int compareTo(ClaveValorDatos o) {
        return this.frecuencia > o.frecuencia ? 1 : (this.frecuencia < o.frecuencia ? -1 : 0);
    }
    

}
