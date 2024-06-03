import java.io.*;
import java.net.*;
import java.util.*;

public class ProServer {
     static final int PORT = 9081;
     static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());
     static int clientIDCounter = 0;

    public static void main(String[] args) {
        System.out.println("Server is listening on port " + PORT);

        try (ServerSocket ss = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = ss.accept();
                System.out.println("New client connected");
                ClientHandler handler = new ClientHandler(clientSocket, clientIDCounter++);
                clientHandlers.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessageToClient(String message, int clientID, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers) {
                if (handler.getClientID() == clientID && handler != sender) {
                    handler.sendMessage(message);
                    return;
                }
            }
            sender.sendMessage("Client with ID " + clientID + " not found.");
        }
    }


    public static void removeClient(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    private static class ClientHandler extends Thread {
         Socket socket;
         PrintWriter out;
         BufferedReader in;
         int clientID;

        public ClientHandler(Socket socket, int clientID) {
            this.socket = socket;
            this.clientID = clientID;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                sendMessage("Welcome! Your client ID is " + clientID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public int getClientID() {
            return clientID;
        }

        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received from client " + clientID + ": " + message);
                    if (message.startsWith("#")) {

                    	int separatorIndex = message.indexOf(":");
                        if (separatorIndex != -1) {
                            String recipientIDStr = message.substring(1, separatorIndex);
                            String privateMessage = message.substring(separatorIndex + 1);
                            try {
                                int recipientID = Integer.parseInt(recipientIDStr);
                                ProServer.sendMessageToClient("Client " + clientID + ": " + privateMessage, recipientID, this);
                            } catch (NumberFormatException e) {
                                sendMessage("Invalid recipient ID format.");
                            }
                        } else {
                            sendMessage("Invalid private message format. Use #<recipientID>:<message>");
                        }}

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
               ProServer.removeClient(this);
                System.out.println("Client " + clientID + " disconnected");
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
