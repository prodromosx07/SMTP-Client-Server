import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {
    
    //Main Method:- called when running the class file.
    public static void main(String[] args){ 
        

        int portNumber = 5000;
        try
        {
            //Setup the socket for communication 
            ServerSocket serverSoc = new ServerSocket(portNumber);
            ArrayList<socketManager> clients = new ArrayList<socketManager>();
            
            while (true)
            {
                //accept incoming communication
                System.out.println("Server Online. Waiting for client");
                Socket soc = serverSoc.accept();
                socketManager temp = new socketManager(soc);
                clients.add(temp); //temp parameter

                //create a new thread for the connection and start it.
                ServerConnectionHandler sch = new ServerConnectionHandler(clients, temp);

                Thread schThread = new Thread(sch); //Server Connection Handler
                schThread.start();
            }
            
        }
        catch (Exception except){
            //Exception thrown (except) when something went wrong, pushing message to the console
            System.out.println("Error --> " + except.getMessage());
        }
    }   
}
    
