import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

//This class can be used to send and receive data over the socket.
//To send a message to the server, you could use the outputStreamString object to write the message to the socket
//To receive use the inputStreamObject.
public class socketManager //The socketManager is used to manage a socket and its input and output streams.
{
	public Socket socketObj = null; //Create a new socket, then create a socket object for that socket and assign it to null.
	public DataInputStream inputStreamObject = null;  //Object that can be used to read data from the socket.
	public DataOutputStream outputStreamObject = null;  //Object that can be used to write data from the socket.
	public socketManager(Socket socket) throws IOException //The socketManager constructor has as an argument a socket. This socket is new
	//and it is being used to assign the socket object from the initial created socket in the method above (reminder that socket is assigned to null).
	{
		socketObj = socket; //Happens right here.
		inputStreamObject = new DataInputStream(socketObj.getInputStream());
		//To receive a message from the server, you use the inputStreamObject object to read the message from the socket by calling its readUTF method.
		outputStreamObject = new DataOutputStream(socketObj.getOutputStream()); //Specifically to send a message, you can use the outputStreamObject
		//object to write the message (as mentioned above) to the socket by calling its writeUTF method and passing the message as an argument.
	}
}


