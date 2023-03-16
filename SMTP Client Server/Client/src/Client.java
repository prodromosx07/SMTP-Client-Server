import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client
{
    public static void main(String[] args)
    {
        //Using netstat -ano to check which ports are open and choosing one to assign.
        //Declaring the port and IP that we want to connect on a variable.
        int portNumber = 5000;
        String serverIP = "localhost";   
        
        try
        {
            //Create a new socket for communication
            Socket ClientSocket = new Socket(serverIP,portNumber);

            //Use a semaphore for synchronisation of the threads
            AtomicBoolean isDATA = new AtomicBoolean(false);

            //Create new instance of the AccountStorage class
            AccountStorage storage = new AccountStorage();

            //Create new instance of the client writer thread, initialise it and start it running
            ClientReader ReadClient = new ClientReader(ClientSocket, isDATA, storage);
            Thread ClientReadThread = new Thread(ReadClient);
            ClientReadThread.start();
            //Thread.start() is required to actually create a new thread, so that the runnable's run method is executed simultaneously.
            //The difference is that Thread.start() starts a thread that calls the run() method,
            //while Runnable.run() just calls the run() method on the current thread

            //Create new instance of the client writer thread, initialise it and start it running
            ClientWriter clientWrite = new ClientWriter(ClientSocket, isDATA, storage );
            Thread clientWriteThread = new Thread(clientWrite);
            clientWriteThread.start();
        }
        catch (Exception except)
        {
            //Exception thrown (except) when something went wrong, pushing message to the console
            System.out.println("Error in SMTP_Client --> " + except.getMessage());
        }
    }
}

class ClientReader implements Runnable
{
    public static String ClientDomainName = "ServerDomain.com";
    public AccountStorage storage;
    Socket ClientReaderSocket = null;
    AtomicBoolean isDATAflag; //isDATAflag is an AtomicBoolean object which is used to signal whether the client is currently in a DATA state.
    String LetDataIn = ""; //This string is used to store data that is sent by the client to the server.
    
    public ClientReader (Socket InputSocket, AtomicBoolean isDATA, AccountStorage accountStorage) //This is a constructor for the ClientReader class.
    {
        ClientReaderSocket = InputSocket;
        this.isDATAflag = isDATA;
        this.storage = accountStorage; //The accountStorage argument is used to store a reference to the AccountStorage object used to store and retrieve account credentials
      //Two parameters are being passed: InputSocket and isDATA and are being assigned to two instance variables in the ClientReader.
    }
  
    public void run() //A run method is created inside the ClientReader, runs continuously in a loop as long as the ClientReaderSocket is open and isDATAflag is false
    //Method that gets executed when the thread is started
    {
        while(!ClientReaderSocket.isClosed() && !isDATAflag.get()) //While connection is open and NOT IN DATA exchange STATE
        {
            //Error messages to get printed on the Client's Console. All Error codes are implemented.
            try
            {
                //Create a DataInputStream object named dataIn, which is used to read data from the input stream of the socket ClientReaderSocket.
                DataInputStream dataIn = new DataInputStream(ClientReaderSocket.getInputStream());
                LetDataIn = dataIn.readUTF(); //Then, the variable LetDataIn is assigned and the value of the data read as a UTF-8 encoded string.
                //The readUTF() method reads these bytes and converts them into a UTF-8 encoded string, which is then stored in the LetDataIn variable.
                if (LetDataIn.contains("221"))
                {
                    System.out.println("...closing socket");
                    ClientReaderSocket.close();
                    return;
                }
                else if (LetDataIn.contains("200"))
                    System.out.println("(nonstandard success response, see rfc876)");
                else if (LetDataIn.contains("211"))
                    System.out.println("System status, or system help reply");
                else if (LetDataIn.contains("214"))
                    System.out.println("Help message");
                else if (LetDataIn.contains("220"))
                    System.out.println("<" + ClientDomainName + ">" + "Service ready");
                else if (LetDataIn.contains("250"))
                    System.out.println("OK -> CLIENT going to state SUCCESS");
                else if (LetDataIn.contains("251"))
                    System.out.println("User not local; will forward to <forward-path>");
                else if (LetDataIn.contains("252"))
                    System.out.println("Cannot VRFY user, but will accept message and attempt delivery");
                else if (LetDataIn.contains("421"))
                    System.out.println(ClientDomainName + "Service not available, closing transmission channel");
                else if (LetDataIn.contains("450"))
                    System.out.println("Requested mail action not taken: mailbox unavailable");
                else if (LetDataIn.contains("451"))
                    System.out.println("Requested action aborted: local error in processing");
                else if (LetDataIn.contains("452"))
                    System.out.println("Requested action not taken: insufficient system storage");
                else if (LetDataIn.contains("500"))
                    System.out.println("Syntax error, command unrecognised");
                else if (LetDataIn.contains("501"))
                    System.out.println("Syntax error in parameters or arguments");
                else if (LetDataIn.contains("502"))
                    System.out.println("Command not implemented");
                else if (LetDataIn.contains("503"))
                    System.out.println("Bad sequence of commands");
                else if (LetDataIn.contains("504"))
                    System.out.println("Command parameter not implemented");
                else if (LetDataIn.contains("521"))
                    System.out.println(ClientDomainName + "does not accept mail (see rfc1846)");
                else if (LetDataIn.contains("530"))
                    System.out.println(ClientDomainName + "does not accept mail (see rfc1846)");
                else if (LetDataIn.contains("550"))
                    System.out.println("Requested mail action aborted: exceeded storage allocation");
                else if (LetDataIn.contains("551"))
                    System.out.println("Requested mail action aborted: exceeded storage allocation");
                else if (LetDataIn.contains("552"))
                    System.out.println("Requested mail action aborted: exceeded storage allocation");
                else if (LetDataIn.contains("553"))
                    System.out.println("Requested action not taken: mailbox name not allowed");
                else if (LetDataIn.contains("554"))
                    System.out.println("Transaction failed");
                else if (LetDataIn.contains("354"))
                    System.out.println("OK -> CLIENT going to state I (wait for data)");
            }  
            catch (Exception except)
            {
              //Exception thrown (except) when something went wrong, pushing message to the console
              System.out.println("Error in ClientReader --> " + except.getMessage());
            }
        }
    }
}
class ClientWriter implements Runnable
{
    public static String CRLF = "\r\n"; //CRLF: a constant string that represents a carriage return and line feed, typically used to indicate the end of a line in a text file or message.
    public static String SP = " ";//SP: a constant string that represents a space character
    Socket ClientWriterSocket = null; //Client Writer Socket object is set null.
    //The reason is that it will be used to store a reference to a socket object that is used for communication between the client and server.
    AtomicBoolean isDATAflag; //Boolean variable that is used to indicate whether the client is in a state of exchanging data with the server.
    public AccountStorage storage; //Account storage object used to store and retrieve account credentials
    
    public ClientWriter (Socket OutputSocket, AtomicBoolean isDATA, AccountStorage accountStorage) //Creating a constructor that gets two arguments
    {
        ClientWriterSocket = OutputSocket; //The Socket object gets assigned OutputSocket.
        // The OutputSocket argument is used to store a reference to the socket object that is used for communication between the client and server
        this.isDATAflag = isDATA; //The isDATA argument is used to store a reference to the AtomicBoolean object that indicates whether the client is in a state
        // of exchanging data with the server.
        this.storage = accountStorage;
        //The accountStorage argument is used to store a reference to the AccountStorage object used to store and retrieve account credentials
    }
    
    public void run() //The run() method is the main method in the ClientWriter class that will be executed when the ClientWriter thread is started.
    {
        String MessageToServer =""; //MessageToServer is a string variable that will be used to store the message that the client wants to send to the server.
        String LetDataIn= ""; //LetDataIn is a string variable that MIGHT be used to store the message that the server sends to the client.
        String ClientDomainName = "ServerDomain.com"; // ClientDomainName is a string variable that holds the domain name of the client.

        try
        {
            try
            {
                // Create a new file object with the file name "username"
                File file = new File("username.txt");
                // Check if the file already exists
                if (!file.exists())
                {
                    // If the file does not exist, create a new file with the given name
                    file.createNewFile();
                }
                // Create a FileWriter and BufferedWriter for writing to the file
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter bw = new BufferedWriter(fileWriter);
                // Write some text to the file
                bw.write("Dorito");
                // Close the BufferedWriter to save the changes to the file
                bw.close();
            }
            catch (IOException e)
            {
                // If an IOException occurs, print the error message
                e.printStackTrace();
            }

            try
            {
                // Create a new file object with the file name "username"
                File file = new File("password.txt");
                // Check if the file already exists
                if (!file.exists())
                {
                    // If the file does not exist, create a new file with the given name
                    file.createNewFile();
                }
                // Create a FileWriter and BufferedWriter for writing to the file
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter bw = new BufferedWriter(fileWriter);
                // Write some text to the file
                bw.write("kingdorito123");
                // Close the BufferedWriter to save the changes to the file
                bw.close();
            }
            catch (IOException e)
            {
                // If an IOException occurs, print the error message
                e.printStackTrace();
            }
            Scanner userName = new Scanner(System.in);
            Scanner password = new Scanner(System.in);
            System.out.println("--------------------------");
            System.out.println("WELCOME, PLEASE LOGIN USING YOUR CREDENTIALS");
            System.out.println("--------------------------");
            System.out.println("Type your username:");
            String username = userName.nextLine();
            System.out.println("--------------------------");
            if (!username.contains("Dorito"))
            {
                System.out.println("Wrong username! Please Try Again");
                return;
            }
            System.out.println("--------------------------");
            System.out.println("Type your password:");
            String passwordd = password.nextLine();
            System.out.println("--------------------------");
            if (!passwordd.contains("kingdorito123"))
            {
                System.out.println("Wrong password! Please Try Again");
                return;
            }
            else if (passwordd.contains("kingdorito123") && username.contains("Dorito"))
            {
                System.out.println("WELCOME!");
            }
            System.out.println ("CLIENT WRITER: SELECT NUMBER CORRESPONDING TO SMTP COMMAND:" + CRLF +  "1:HELO 2:MAIL FROM 3:RCPT TO 4:DATA 5:HELP 6:NOOP 7:RSET 8: VRFY 9: EXPN 10: EHLO 11:QUIT");
            //It then prompts the user to select a number corresponding to an SMTP command, and creates a loop that will continue until the client's socket is closed.
            //The switch statement will allow the user to select a command, and then execute the corresponding code block.
            DataOutputStream getDataOut = new DataOutputStream(ClientWriterSocket.getOutputStream());
            //This block of code is creating an output stream for sending data to the server, and a scanner to allow the user to input a command.

            while (!ClientWriterSocket.isClosed())
            {
                Scanner switchScanner = new Scanner(System.in); //and a scanner to allow the user to use the switch command.
                Scanner sc = new Scanner(System.in);//and a scanner to allow the user to use the input command. Specifically this is for the MAIL FROM

                switch(switchScanner.nextInt())
                {
                    case 1: //HELO
                    {
                        System.out.println("--------------------------");
                        System.out.println("Sending HELO");
                        System.out.println("--------------------------");
                        System.out.println(ConsoleColors.BLUE+"Sending..."+ConsoleColors.RESET+ "HELO"+ SP + ClientDomainName + CRLF);

                        //MessageToServer
                        MessageToServer = ("HELO" + SP + ClientDomainName + CRLF);
                        getDataOut.writeUTF(MessageToServer);
                        getDataOut.flush(); // The "flush" method is called on the DataOutputStream to ensure that all the data in the stream is sent to the server.
                        break;
                    }
                    case 2: // MAIL FROM
                    {
                        System.out.println("--------------------------");
                        System.out.println("Please type your mail address:");
                        String mailFrom = sc.nextLine(); //Assign the user input to the scanner created out of the switch, inside the loop thought
                        System.out.println("--------------------------");

                        MessageToServer = ("MAIL" + SP + "FROM: " + "<" + mailFrom + ">" + CRLF); //Storing in a variable what we want to send to the server.
                        getDataOut.writeUTF(MessageToServer); //This block of code is sending the message stored in the variable MessageToServer to the server through the DataOutputStream dataOut.
                        getDataOut.flush(); //The flush method is then called to flush the output stream and send the message to the server.
                        break; //The break statement is used to exit the switch case (terminate the command) and continue with the next iteration of the while loop.
                    }
                    case 3: //RCPT TO
                    {
                        System.out.println("--------------------------");
                        System.out.println("Please type recipients mail address:");
                        Scanner rcptScanner = new Scanner(System.in); //For the recipient, create another Scanner
                        String rcptTo = rcptScanner.nextLine(); //Assign the user input to a string
                        System.out.println("--------------------------");
                        MessageToServer = ("RCPT" + SP + "TO: " + "<" + rcptTo + ">" + CRLF); //Send it all the way to the server
                        getDataOut.writeUTF(MessageToServer); //This block of code is sending the message stored in the variable MessageToServer to the server through the DataOutputStream dataOut.
                        getDataOut.flush(); //The flush method is then called to flush the output stream and send the message to the server.
                        break; //The break statement is used to exit the switch case (terminate the command) and continue with the next iteration of the while loop.
                    }
                    case 4: //DATA
                    {
                        System.out.println("--------------------------");
                        System.out.println("Please type your mail:");
                        Scanner dataScanner = new Scanner(System.in); //For the data mail (message), create another Scanner
                        String dataString = dataScanner.nextLine(); //Assign the user input to a string
                        System.out.println("--------------------------");

                        MessageToServer = ("DATA" + SP + "<" + dataString + ">" + CRLF); //Send it all the way to the server
                        getDataOut.writeUTF(MessageToServer); //This block of code is sending the message stored in the variable MessageToServer to the server through the DataOutputStream dataOut.
                        getDataOut.flush(); //The flush method is then called to flush the output stream and send the message to the server.
                        break; //The break statement is used to exit the switch case (terminate the command) and continue with the next iteration of the while loop.
                    }
                    case 5://HELP
                    {
                        System.out.println("--------------------------");
                        System.out.println("Sending Help to Server");
                        System.out.println("--------------------------");

                        MessageToServer = ("HELP");
                        getDataOut.writeUTF(MessageToServer);
                        getDataOut.flush();
                        break;
                    }
                    case 6://NOOP
                    {
                        System.out.println("--------------------------");
                        System.out.println("NOOP");
                        System.out.println("--------------------------");

                        MessageToServer = ("NOOP:" + CRLF);
                        getDataOut.writeUTF(MessageToServer);
                        getDataOut.flush();
                        break;
                    }
                    case 7: //RSET
                    {
                        System.out.println("--------------------------");
                        System.out.println("Resetting Client to initial state.....");
                        System.out.println("--------------------------");

                        MessageToServer = ("RSET");
                        getDataOut.writeUTF(MessageToServer);
                        getDataOut.flush();
                        break;
                    }
                    case 8: //VRFY
                    {
                        MessageToServer = ("504"); //Not implemented.
                        getDataOut.writeUTF(MessageToServer);
                        getDataOut.flush();
                        break;
                    }
                    case 9://EXPN
                    {
                        System.out.println("Error 504: Command parameter not implemented");
                        MessageToServer = ("EXPN"); //Not implemented.
                        getDataOut.writeUTF(MessageToServer);
                        getDataOut.flush();
                        break;
                    }
                    case 10: //EHLO
                    {
                        MessageToServer = ("504"); //Not implemented.
                        getDataOut.writeUTF(MessageToServer);
                        getDataOut.flush();
                        break;
                    }
                    case 11: //QUIT
                    {
                        System.out.print("CLIENT : QUITing");
                        MessageToServer = ("QUIT" + CRLF);
                        getDataOut.writeUTF(MessageToServer); //This block of code is sending the message stored in the variable MessageToServer to the server through the DataOutputStream dataOut.
                        getDataOut.flush(); //The flush method is then called to flush the output stream and send the message to the server.
                        System.out.println("...closing socket ");
                        break; //The break statement is used to exit the switch case (terminate the command) and continue with the next iteration of the while loop.
                    }
                }
            }
        }
        catch (Exception except)
        {
            //Exception thrown (except) when something went wrong, pushing message to the console
            System.out.println("Error in ClientWriter --> " + except.getMessage());
        }
    }
}
