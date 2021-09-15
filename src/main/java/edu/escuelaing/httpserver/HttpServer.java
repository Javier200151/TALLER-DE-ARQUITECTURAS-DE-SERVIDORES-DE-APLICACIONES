package edu.escuelaing.httpserver;

import java.net.*;
import java.nio.charset.Charset;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import java.io.OutputStream;

import edu.escuelaing.springplus.Service;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author JavierELopez
 */
public class HttpServer {
    public static final Integer PORT = getPort();
    private static final HttpServer _instance = new HttpServer();
    private static final HashMap<String,String> contentType = new HashMap<String,String>();
    private static final String ROOT_PATH = "edu.escuelaing.webapp.";

    private HashMap<String, Method> services =  new HashMap<>();

    private HttpServer(){ }

    
    public static HttpServer getInstance(){
        contentType.put("html","text/html");
        contentType.put("css","text/css");
		contentType.put("js","text/javascript");

		contentType.put("jpeg","image/jpeg");
		contentType.put("jpg","image/jpg");
		contentType.put("png","image/png");
		contentType.put("ico","image/vnd.microsoft.icon");
        return _instance;
    }

    public void startServer(String[] args) throws IOException, URISyntaxException {
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(getPort());
        }catch(IOException e){
            System.err.println("Could not listen on port: "+getPort());
            System.exit(1);
        }
        boolean running = true;
        while (running) {
            Socket clientSocket = null;
            try{
                System.out.println("Listo para recibir en el puerto: "+getPort());
                clientSocket = serverSocket.accept();
            }catch (IOException e){
                System.err.println("Accept Failed.");
                System.exit(1);
            }
            manageConnection(clientSocket);
        }
        serverSocket.close();
    }

    private void loadServices(Class c) {
            for( Method m: c.getDeclaredMethods()){
                if(m.isAnnotationPresent(Service.class)){
                    String uri =  m.getAnnotation(Service.class).uri();
                    services.put(uri, m);
                }
            }
            
    }

    public void manageConnection(Socket clientSocket) throws IOException, URISyntaxException{

        OutputStream outStream=clientSocket.getOutputStream();
		PrintWriter out = new PrintWriter(outStream, true);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        clientSocket.getInputStream()));
        String inputLine, outputLine;
        ArrayList<String> request = new ArrayList<>();
        String sv="";
        

        while ((inputLine = in.readLine()) != null) {
            System.out.println("Received: " + inputLine);
            request.add(inputLine);
            if (!in.ready()) {
                break;
            }
        }

        try {
            String uriStr = request.get(0).split(" ")[1];
            URI resourceURI = new URI(uriStr);
            System.out.println("URI Path: "+ resourceURI.getPath());
            System.out.println("URI query: "+ resourceURI.getQuery());

            if(resourceURI.toString().startsWith("/appuser")){
                outputLine = getComponentResource(resourceURI);
                out.println(outputLine);
            }else if(resourceURI.toString().contains("jpg") || resourceURI.toString().contains("jpeg")){
                outputLine = computeImageResponse(resourceURI.getPath().split("/")[1], outStream);
            }else{
                outputLine = getHTMLResource(resourceURI);
                out.println(outputLine);
            }
        }catch(Exception e){
            System.out.println(e);
        }

        out.close();
        in.close();
        clientSocket.close();
    }

    private String getComponentResource(URI resourceURI) {
        String response = default404HTMLResponse();
        try{
            String classPath = resourceURI.getPath().toString().replaceAll("/appuser/","");
            String className = classPath.substring(0, classPath.indexOf("/"));
            Class component = Class.forName(ROOT_PATH + className);
            for (Method m : component.getDeclaredMethods()){
                if(m.isAnnotationPresent(Service.class)){
                    loadServices(component);
                    response = m.invoke(null).toString();
                    response = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html\r\n"
                    + "\r\n" + response;
                }
            }   
        } catch(ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, ex);
            response = default404HTMLResponse();
        }
        return response;
    }

    public String getHTMLResource(URI resourceURI) {
        Path file = Paths.get("src/main/resources/public_html" + resourceURI.getPath());

        String response="";
        Charset charset = Charset.forName("UTF-8");
        try(BufferedReader reader = Files.newBufferedReader(file, charset)){
            response = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: text/html\r\n"
                    + "\r\n";
            String line = null;
            while ((line = reader.readLine()) != null){
                System.out.println(line);
                response = response + line;
            }
        }catch (IOException x){
            System.err.format("IOException: %s%n",x);
            response=default404HTMLResponse();
        }
        return response;
    }

    public String computeImageResponse(String uriImgType, OutputStream outStream){
        uriImgType=uriImgType.replace("/img","");
        
        String extensionUri = uriImgType.substring(uriImgType.lastIndexOf(".") + 1);

        String content = "HTTP/1.1 200 OK \r\n" 
                            + "Content-Type: "+ contentType.get(extensionUri) + "\r\n"
                            + "\r\n";
        System.out.println("uriImgType " + uriImgType);
        File file = new File("src/main/resources/public/img/"+uriImgType);
        System.out.println("file "+file);
        try {
            BufferedImage bi = ImageIO.read(file);
            ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
            DataOutputStream dataOutputStream= new DataOutputStream(outStream); 
            ImageIO.write(bi, extensionUri, byteArrayOutputStream);
            dataOutputStream.writeBytes(content);
            dataOutputStream.write(byteArrayOutputStream.toByteArray());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public String default404HTMLResponse(){
        String outputLine
                = "HTTP/1.1 404 Not Found\r\n"
                + "Content-Type: text/html\r\n"
                + "\r\n"
                + "<!DOCTYPE html>"
                + "<html>"
                + "     <head>"
                + "         <title>404 Not Found</title>"
                + "         <meta charset=\"URF-8\">"
                + "         <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "     </head>"
                + "     <body>"
                + "         <div><h1>Error 404</h1></div>"
                + "     </body>"
                + "</html>";
        return outputLine;
    }

    public String defaultHttpMessage() {
        String outputLine 
                = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html\r\n"
                + "\r\n" 
                + "<!DOCTYPE html>"
                + " <html>" 
                + "     <head>" 
                + "         <title> TODO supply a title </title>"
                + "         <meta charset=\"URF-8\">"
                + "         <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" 
                + "     </head>"
                + "     <body>" 
                + "         <div><h1>My first page.</h1></div>"
                + "         <img src=\"https://labyes.com/feline/wp-content/uploads/2020/08/28Jul_LabyesNotaWeb1_2-1920x1283.jpg.webp\""  
                + "</html>";
        return outputLine;
    }

    static int getPort() {
        if (System.getenv("PORT") != null) {
            return Integer.parseInt(System.getenv("PORT"));
        }
        return 35000; //returns default port if heroku-port isn't set (i.e. on localhost)
    }

    public static void main(String... args){
        try{
            HttpServer.getInstance().startServer(args);
        }catch (IOException ex) {
            Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }catch (URISyntaxException ex) {
            Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}







/*
public class HttpServer {
    private static final HttpServer _instance = new HttpServer();
    private static final HashMap<String,String> contentType = new HashMap<String,String>();
    public static HttpServer getInstance(){
        contentType.put("html","text/html");
        contentType.put("css","text/css");
		contentType.put("js","text/javascript");

		contentType.put("jpeg","image/jpeg");
		contentType.put("jpg","image/jpg");
		contentType.put("png","image/png");
		contentType.put("ico","image/vnd.microsoft.icon");
        return _instance;
    }

    private HttpServer(){
        
    }

    public void start() throws IOException{
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(getPort());
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }
        boolean running = true;
        while(running){
            Socket clientSocket = null;
            try {
                System.out.println("Listo para recibir ...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
            try {
                serverConnection(clientSocket);
            } catch (URISyntaxException e) {
                System.err.println("URI incorrect.");
                System.exit(1);
            }
        }
        serverSocket.close();
    }

    public void serverConnection(Socket clientSocket) throws IOException, URISyntaxException {
        OutputStream outStream=clientSocket.getOutputStream();
		PrintWriter out = new PrintWriter(outStream, true);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        clientSocket.getInputStream()));
        String inputLine, outputLine;
        ArrayList<String> request = new ArrayList<>();
        String sv="";
        

        while ((inputLine = in.readLine()) != null) {
            System.out.println("Received: " + inputLine);
            request.add(inputLine);
            if (!in.ready()) {
                break;
            }
        }

        String uriContentType="";
		String uri="";
		try {
			
			uriContentType=request.get(0).split(" ")[1];

            URI resource = new URI(uriContentType);
			
			uri=resource.getPath().split("/")[1];
        }catch(Exception e){
            System.out.println(e);
        }
        outputLine = getResource( uri, outStream);
        out.println(outputLine);
        out.close();
        in.close();
        clientSocket.close();
    }
    public String getResource( String uri, OutputStream outStream) throws URISyntaxException{
        if(uri.contains("jpg") || uri.contains("jpeg")){
            return computeImageResponse(uri, outStream);
        }else{
            return computeContentResponse(uri);
        }
    }

    public String computeContentResponse(String uriContentType){
        String extensionUri = uriContentType.substring(uriContentType.lastIndexOf(".") + 1);
        String content = "HTTP/1.1 200 OK \r\n" 
                            + "Content-Type: "+ contentType.get(extensionUri) + "\r\n"
                            + "\r\n";
        File file = new File("src/main/resources/public/"+uriContentType);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line =  br.readLine()) != null) content += line; 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public String computeImageResponse(String uriImgType, OutputStream outStream){
        uriImgType=uriImgType.replace("/img","");
        
        String extensionUri = uriImgType.substring(uriImgType.lastIndexOf(".") + 1);

        String content = "HTTP/1.1 200 OK \r\n" 
                            + "Content-Type: "+ contentType.get(extensionUri) + "\r\n"
                            + "\r\n";
        System.out.println("uriImgType " + uriImgType);
        File file = new File("src/main/resources/public/img/"+uriImgType);
        System.out.println("file "+file);
        try {
            BufferedImage bi = ImageIO.read(file);
            ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
            DataOutputStream dataOutputStream= new DataOutputStream(outStream); 
            ImageIO.write(bi, extensionUri, byteArrayOutputStream);
            dataOutputStream.writeBytes(content);
            dataOutputStream.write(byteArrayOutputStream.toByteArray());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public String computeDefaultResponse(){
        String outputLine =
                "HTTP/1.1 200 OK\n"
                        + "Content-Type: text/html\r\n"
                        + "\r\n"
                        + "<!DOCTYPE html>"
                        + "<html>"
                        + "<head>"
                        + "<meta charset=\"UTF-8\">"
                        + "<title>Title of the document</title>\n"
                        + "</head>"
                        + "<body>"
                        + "My Web Site"
                        + "</body>"
                        + "</html>";
        return outputLine;
    }
    static int getPort() {
        if (System.getenv("PORT") != null) {
            return Integer.parseInt(System.getenv("PORT"));
        }
        return 35000; //returns default port if heroku-port isn't set (i.e. on localhost)
    }
    public static void main(String[] args) throws IOException {
        HttpServer.getInstance().start();
    }
}
*/