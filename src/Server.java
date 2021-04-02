import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class Server {

    public static void main(String[] args) throws Throwable {
        ServerSocket ss = new ServerSocket(8080);
        
        while (true) {
            Socket s = ss.accept();
            System.out.println("Client accepted");
            new Thread(new SocketProcessor(s)).start();
        }
    }

    private static class SocketProcessor implements Runnable {

        private Socket s;
        private InputStream is;
        private OutputStream os;
        private String data, index_path = "D:\\Documents\\Study\\OOP\\SocketTest\\src\\index.html";
        private Pattern source_pattern = Pattern.compile("^\\/\\S+");
        private Matcher matcher;

        private SocketProcessor(Socket s) throws Throwable {
            this.s = s;
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
            
    		try (FileInputStream fin = new FileInputStream(index_path)) {
    			byte[] buffer = new byte[fin.available()];
    			fin.read(buffer, 0, buffer.length);
    			data = new String(buffer, StandardCharsets.UTF_8);
    		}
    		catch(IOException ex) {
    			System.out.println(ex.getMessage());
    			data = "<html><body><h1>Hello from Habrahabr</h1></body></html>";
    		}
        }

        public void run() {
            try {
                String resource = readInputHeaders();
                System.out.println(resource);
                writeResponse(resource);
            } catch (Throwable t) {
                System.err.println("error: " + t);
            } finally {
                try {
                    s.close();
                } catch (Throwable t) {
                    /*do nothing*/
                }
            }
            System.out.println("Client processing finished");
        }
        
        private void writeResponse(String resource) throws Throwable {
        	if (resource.equals(index_path)) {
        		sendIndex();
        	}
        	else if (resource.endsWith(".jpg")) {
        		sendImageResponse(resource);
        	}
        	else {
        		sendVoidResponse();
        	}
        }
        
        private void sendVoidResponse() throws IOException {
        	System.out.println("void");
            String response = "HTTP/1.1 404\r\n" +
                    "Server: SadServer/25.03.2021\r\n" +
                    "Connection: close\r\n\r\n";
            os.write(response.getBytes());
            os.flush();
        }
        
        private void sendIndex() throws IOException {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Server: SadServer/25.03.2021\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + data.length() + "\r\n" +
                    "Connection: close\r\n\r\n";
            String result = response + data;
            os.write(result.getBytes());
            os.flush();
        }

        private void sendImageResponse(String resource) throws Throwable {
    		byte[] image_data = readImage(resource);
    		
            byte[] response = ("HTTP/1.1 200 OK\r\n" +
                    "Server: SadServer/25.03.2021\r\n" +
                    "Content-Type: image/jpeg\r\n" +
                    "Content-Length: " + image_data.length + "\r\n" +
                    "Connection: close\r\n\r\n").getBytes();

            byte[] combined = new byte[response.length + image_data.length];

            for (int i = 0; i < combined.length; i++) {
            	if (i < response.length) {
            		combined[i] = response[i];
            	}
            	else {
            		combined[i] = image_data[i - response.length];
            	}
            }
            os.write(combined);
            os.flush();
        }
        
        private byte[] readImage(String path) throws IOException {
            BufferedImage bImage = ImageIO.read(new File(path));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(bImage, "jpg", bos);
            byte[] data = bos.toByteArray();
            
            return data;
        }

        private String readInputHeaders() throws Throwable {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String resource = "/";
             
            while(true) {
                String s = br.readLine();
                
                if (s.startsWith("GET")) {
                	s = s.substring(4, s.length());
                	matcher = source_pattern.matcher(s);
                	if (matcher.find())	{
                		resource = s.substring(matcher.start(), matcher.end());
                	}
                }
                if(s == null || s.trim().length() == 0) {
                    break;
                }
            }
            
            String dir = System.getProperty("user.dir");
            File f_temp = new File(dir, "/src");
            File f = new File(f_temp.getPath(), resource);
            
            if (f.exists() && !f.isDirectory() && resource != "/") {
            	return f.getPath();
            } else {
            	return index_path;
            }
        }
    }
}