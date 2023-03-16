import com.sun.net.httpserver.Authenticator;

import java.util.ArrayList;

public class ServerConnectionHandler implements Runnable
//This is a class that defines a server connection handler object.
// It is used to manage the connection between the server and a client.
{
    public static String CRLF = "\r\n";
    public static String NL = "\n";
    public static String ServerDomainName = "ServerDomain.com"; //Declare a static string variable named 'ServerDomainName' and initialize it with the value "ServerDomain.com"
    //This variable is used to store the domain name of the server.
    private static String CommandStack = ""; // is a string that holds the commands received from the client. It's initilised to null.
    socketManager SocketManagerObjectVar = null; //object of the socketManager class. It is used to manage the socket and its input and output streams.
    ArrayList<socketManager> OnlineClients = null;

    public ServerConnectionHandler(ArrayList<socketManager> getInArrayListVar, socketManager InSocketManagerVar)
    //Inside the constructor, the value of the SocketManagerObjectVar field is set to InSocketManagerVar and the value of the OnlineClients field is set to getInArrayListVar.
    {
        SocketManagerObjectVar = InSocketManagerVar; //InSocketManagerVar: This is a socketManager object that represents a connection between the server and a client.
        OnlineClients = getInArrayListVar; //getInArrayListVar: This is an ArrayList of socketManager objects, which represents a list of connected clients.
    }

    //Quick reminder before going into public void run. the "socketObj" is called from the socketManager

    public void run()
    //This block of code is the run method of the ServerConnectionHandler class, which implements the Runnable interface.
    //It handles the communication between the server and a client through a socket connection.
    {
        try
        {
            System.out.println("0 Client " + SocketManagerObjectVar.socketObj.getPort() + " Connected");
            System.out.println("0 SERVER : active clients : " + OnlineClients.size());
            while (!SocketManagerObjectVar.socketObj.isClosed())
            {
                String MessClients = SocketManagerObjectVar.inputStreamObject.readUTF();
                System.out.println("SERVER : Message FROM CLIENT : " + SocketManagerObjectVar.socketObj.getPort() + " --> " + MessClients);

                //Check for Quit message for client. When a message is received from the client, the run method checks if it contains the string "QUIT",
                //which signifies that the client wants to close the connection. If the message does contain "QUIT", the run method sends a message to
                //the client with a status code of 221 and removes the client from the list of online clients.
                if (MessClients.contains("QUIT"))
                {
                    System.out.println("5 SERVER : Quiting client");
                    //
                    // SYNTAX (page 12 RFC 821)
                    // QUIT <SP> <SERVER domain> <SP> Service closing transmission channel<CRLF>
                    //          
                    SocketManagerObjectVar.outputStreamObject.writeUTF("221" + NL + ServerDomainName + NL + " Service closing transmission channel" + CRLF);
                    OnlineClients.remove(SocketManagerObjectVar);
                    System.out.print("5 SERVER : active clients : " + OnlineClients.size());
                    CommandStack = "";
                    return; //Exiting thread
                }
                //If the message does not contain "QUIT", the run method passes the message and the socketManager object to the Server_SMTP_Handler method for further processing.
                //If an exception is thrown during this process, it is caught and the exception message is printed to the console.
                Server_SMTP_Handler(SocketManagerObjectVar, MessClients);
            }   //while socket NOT CLOSED
        }
        //The catch block specifies that it will handle any Exception type. The except variable is an instance of the Exception class, and it contains information
        //about the error that occurred.
        catch (Exception except)
        {
            //Exception thrown (except) when something went wrong, pushing clientMSG to the console
            System.out.println("Error in Server Connection Handler --> " + except.getMessage());
            //The getMessage method of the Exception class is called to retrieve the error message associated with the exception and it gets printed.
        }
    }

    private void Server_SMTP_Handler(socketManager SocketManager, String MessClients)
    {
        //This block of code is the Server_SMTP_Handler method is responsible for handling commands and responses from the client.
        //It receives two arguments: a socketManager object and a String object representing the message received from the client.

        //These variables are flags and/or strings that are used within the Server_SMTP_Handler method to control the flow of the program and store information.
        boolean RequestedDomainNameNotAvailable = false;
        //The RequestedDomainNameNotAvailable flag is set to false and is used to indicate whether the requested domain name is not available.
        String ServerDomainName = "ServerDomain.com";
        //The ServerDomainName string is set to "ServerDomain.com" and represents the domain name of the server
        boolean smtpNoStorage = false;
        //The smtpNoStorage flag is set to false and is used to indicate whether the server is unable to store the message.
        boolean smtpInsufficientStore = false;
        //The smtpInsufficientStore flag is set to false and is used to indicate whether the server has insufficient storage space to store the message.
        boolean smtpLocalProcessingError = false;
        //The smtpLocalProcessingError flag is set to false and is used to indicate whether there is a local processing error on the server.
        boolean successState = false;
        //The successState flag is set to false and is used to indicate whether the operation was successful.
        boolean waitState = true;
        //The waitState flag is set to true and is used to indicate whether the server is waiting for a response.
        String AnswerToClient = "";
        //The AnswerToClient string is initialized to an empty string and is used to store the answer to be sent to the client.

        //This code creates a new ArrayList object called "UsersInServerDomain" which can store elements of type String.
        //Then, it adds three String elements to this ArrayList, with the values "Alice", "Bob", and "Mike".
        // This ArrayList can be used to store a list of user names for the "ServerDomain.com" domain.
        ArrayList<String> UsersInServerDomain = new ArrayList<String>();
        UsersInServerDomain.add("Alice");
        UsersInServerDomain.add("Bob");
        UsersInServerDomain.add("Mike");

        //This code creates a new ArrayList object called 'KnownDomains' which will store a list of strings representing the known domains in the server.
        //Three strings are added to the list, representing the domains "ServerDomain.com", "MyServerDomain.com", and "AnotherServerDomain.com".
        //This list may be used later to verify if a domain is known to the server.
        ArrayList<String> KnownDomains = new ArrayList<String>();
        KnownDomains.add("ServerDomain.com");
        KnownDomains.add("MyServerDomain.com");
        KnownDomains.add("AnotherServerDomain.com");


        ArrayList<String> MailDataBuffer = new ArrayList<String>();
        //The MailDataBuffer variable is an ArrayList of type String that is used to store the data of an email message as it is being sent from a client to a server.
        ArrayList<String> ForwardPathBuffer = new ArrayList<String>();
        //The ForwardPathBuffer variable is an ArrayList of type String that is used to store the email address of the sender of an email message.
        // This information is used when the server receives the message and needs to know where to send a response or acknowledgement.
        ArrayList<String> ReversePathBuffer = new ArrayList<String>();
        //The ReversePathBuffer variable is an ArrayList of type String that is used to store the email address of the recipient of an email message.
        // This information is used when the server receives the message and needs to know where to send a response or acknowledgement.
        boolean checks = true;
        try
        {
            if (MessClients.contains(CRLF))
            {
                //System.out.println("SERVER SIDE command RECEIVED--> " + MessClients);
                // error 500 -> Line too long ! COMMAND CASE = 512
                // you can use case here
                if (MessClients.contains("QUIT"))
                {
                    checks = false;
                    CommandStack = "";
                }
                else if (MessClients.length() > 512 && checks)
                {
                    AnswerToClient = "500" + CRLF;
                    System.out.println("error 500 -> Line too long");
                    successState = false;
                    checks = false;
                }
                // error 501 -> Syntax error in parameters or arguments
                else if (MessClients.split(" ").length < 1 && checks)
                {
                    AnswerToClient = "501" + CRLF;
                    //System.out.println("error 501 -> Syntax error in parameters or arguments");
                    successState = false;
                    checks = false;
                }
                // error 504 -> Command parameter not implemented
                else if (MessClients.length() < 4 && checks)
                {
                    AnswerToClient = "504" + CRLF;
                    System.out.println("Error 504 -> Command parameter not implemented");
                    successState = false;
                    checks = false;
                }
                // error 421 -> <domain> Service not available
                else if (RequestedDomainNameNotAvailable && checks)
                {
                    AnswerToClient = "421" + CRLF;
                    String domain_not_found = MessClients.replaceAll("HELO ", "");
                    domain_not_found = domain_not_found.replaceAll(CRLF, "");
                    //System.out.println("error 421 -> "+ domain_not_found +" Service not available");
                    successState = false;
                    checks = false;
                }
                else if (MessClients.contains("HELO") && checks)
                {
                    AnswerToClient = "250" + NL + ServerDomainName + CRLF;
                    //System.out.println("SERVER responce: "+ sResponceToClient);
                    successState = true;
                    checks = false;
                    CommandStack = CommandStack + "HELO ";
                    System.out.println(CommandStack);
                    if (CommandStack.contains("HELO") && CommandStack.contains("MAIL FROM") && CommandStack.contains("RCPT TO"))
                    {
                        AnswerToClient = ("354" + CRLF);
                    }
                }
                else if (MessClients.contains("MAIL FROM: ") && checks)
                {
                    //This block of code is handling the "MAIL FROM" command.
                    //If the message received from the client contains the string "MAIL FROM: " and the "checks" variable is true,
                    //then the email address specified in the message is added to the ReversePathBuffer array list.
                    ReversePathBuffer.add(MessClients.replace("MAIL FROM:<", "").replace(">" + CRLF, ""));
                    //This replacement basically removes "<", ">" symbols. It just replaces them with an empty string. *And the mail from string.
                    //Output: Is only the stored mail address from the user input scanner
                    AnswerToClient = ("250" + CRLF);
                    successState = true;
                    //The server sends a response to the client with a status code of 250, indicating that the requested mail action has been completed.
                    //The "successState" is set to true, and a message is printed to the console indicating the same.
                    System.out.println("250: Requested mail action completed");
                    CommandStack = CommandStack + "MAIL FROM ";
                    if (CommandStack.contains("HELO") && CommandStack.contains("MAIL FROM") && CommandStack.contains("RCPT TO"))
                    {
                        AnswerToClient = ("354" + CRLF);
                    }
                    //The string "MAIL FROM" is added to the CommandStack variable, which keeps track of the commands that have been received from the client.
                    //If the CommandStack variable contains the strings "HELO", "MAIL FROM", and "RCPT TO",
                    //then the server sends a response to the client with a status code of 354, indicating that the client should start entering the message data.
                }
                else if (MessClients.contains("RCPT TO: ") && checks)
                {
                    //This block of code is handling the "RCPT TO" command.
                    //If the message received from the client contains the string "RCPT TO" and the "checks" variable is true,
                    //then the email address specified in the message is added to the ForwardPathBuffer array list.
                    ForwardPathBuffer.add(MessClients.replace("RCPT TO:<", "").replace(">" + CRLF, ""));
                    //This replacement basically removes "<", ">" symbols, and the RCPT TO string. It just replaces them with an empty string.
                    //Output: Is only the stored mail address from the user input scanner
                    AnswerToClient = ("250" + CRLF);
                    successState = true;
                    CommandStack = CommandStack + "RCPT TO ";
                    System.out.println("250: Requested mail action completed");
                    //The server sends a response to the client with a status code of 250, indicating that the requested mail action has been completed.
                    //The "successState" is set to true, and a message is printed to the console indicating the same.
                    if (CommandStack.contains("HELO") && CommandStack.contains("MAIL FROM") && CommandStack.contains("RCPT TO"))
                    {
                        AnswerToClient = ("354" + CRLF);
                    }
                    //The string "RCPT TO" is added to the CommandStack variable, which keeps track of the commands that have been received from the client.
                    //If the CommandStack variable contains the strings "HELO", "MAIL FROM", and "RCPT TO",
                    //then the server sends a response to the client with a status code of 354, indicating that the client should start entering the message data.
                }
                else if (MessClients.contains("DATA ") && checks)
                {
                    //This code block checks if the message received from the client contains the string "DATA" and the boolean variable "checks" is true.
                    //f both conditions are met, it checks if the "CommandStack" string contains the strings "HELO", "MAIL FROM", and "RCPT TO".
                    //If all three strings are present in "CommandStack", it adds the message received from the client to the "MailDataBuffer" array list,
                    //sets the "AnswerToClient" string to "250" and "successState" to true, and prints a message indicating that the requested mail action has been completed.
                    if (CommandStack.contains("HELO") && CommandStack.contains("MAIL FROM") && CommandStack.contains("RCPT TO"))
                    {
                        MailDataBuffer.add(MessClients.replace("DATA <", "").replace(">" + CRLF, ""));
                        AnswerToClient = ("250" + CRLF);
                        successState = true;
                        System.out.println("250: Requested mail action completed");
                    }
                    else //If the "CommandStack" does not contain all three strings, it sets the "AnswerToClient" string to "503".
                    {
                        AnswerToClient = ("503" + CRLF);
                    }
                }
                else if (MessClients.contains("HELP") && checks)
                {
                    AnswerToClient = ("214" + CRLF);
                    successState = true;
                    System.out.println("Sending help to client");
                }
                else if (MessClients.contains("RSET")) //RSET
                {
                    //The RSET command's job is to clear everything.
                    //The mails from RCPT TO/MAIL FROM are stored here.
                    //The mail message is the MailDataBuffer
                    ForwardPathBuffer.clear();
                    ReversePathBuffer.clear();
                    MailDataBuffer.clear();
                    CommandStack.replace("MAIL FROM  ", "").replace("RCPT TO ", "");
                    AnswerToClient = ("250 OK" + CRLF);
                    //In case a new mail is on the way
                }
                else if (MessClients.contains("EXPN") && checks || (MessClients.contains("EHLO")))
                {
                    AnswerToClient = ("502");
                    successState = true;
                    System.out.println("Command not implemented");
                }
                else if (MessClients.contains("NOOP:") && checks) //NOOP
                    //RSET & NOOP have the same reply.
                {
                    AnswerToClient = ("250 OK" + CRLF);
                    successState = true;
                    System.out.println (AnswerToClient + "Ping Successful");
                }
                    MessClients = ""; //Empty buffer after CRLF
            }
            SocketManager.outputStreamObject.writeUTF(AnswerToClient);
            //Writing a message to the client through the socket using the DataOutputStream object outputStreamObject and the method writeUTF.
            //The message being sent is stored in the AnswerToClient variable.
        }
        catch (Exception except)
        {
            //Exception thrown (except) when something went wrong, pushing message to the console
            System.out.println("Error --> " + except.getMessage());
        }
    }
}





