package edu.escuelaing.webapp;

import java.util.logging.Level;
import java.util.logging.Logger;
import edu.escuelaing.httpserver.HttpServer;
import java.net.URISyntaxException;
import java.io.IOException;

public class AppStarter {
    public static void main(String[] args){
        try{
            HttpServer.getInstance().startServer(args);
        }catch (IOException ex) {
            Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }catch (URISyntaxException ex) {
            Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
