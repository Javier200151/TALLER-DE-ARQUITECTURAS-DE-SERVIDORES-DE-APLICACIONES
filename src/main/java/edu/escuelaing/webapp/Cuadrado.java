package edu.escuelaing.webapp;

import edu.escuelaing.springplus.Component;
import edu.escuelaing.springplus.Service;

@Component
public class Cuadrado {
    
    @Service(uri="/cuadrado")
    public static Double cuadrado(){
        return 2.0 * 2.0;
    }
    @Service(uri="/cubo")
    public static Double cubo(){
        return 2.0 * 2.0 * 2.0;
    }
}
