import java.io.*;
import java.util.Scanner;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class Seller {
    private static final int PORT = 12345;
    private static final String[] ITEMS = {"flower", "sugar", "potato", "oil"};
    private final ConcurrentHashMap<String, Integer> inventory = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();
    private final ConcurrentHashMap<Integer, PrintWriter> buyers = new ConcurrentHashMap<>();
    private int buyerIdCounter = 2; // First buyer starts with nodeID 2. nodeID1 is the seller

    public Seller() {
        // Initialize inventory with 5 units per item
        for (String item : ITEMS) {
            inventory.put(item, 5);
        }
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Seller server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private int clientId;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                // Assign a unique ID to this client (buyer)
                this.clientId = buyerIdCounter++;
                buyers.put(clientId, out);

                out.println("Welcome! Your nodeID is: nodeID" + clientId);
                out.println("Available items: " + inventory);

                String request;
                while ((request = in.readLine()) != null) {
                    String[] parts = request.split(":");
                    if (parts[0].equals("BUY_REQUEST")) {
                        String item = parts[1];
                        int quantity = Integer.parseInt(parts[2]);
                        handlePurchase(out, item, quantity);
                    } else if (request.equals("LIST_ITEMS")) {
                        out.println("Available items: " + inventory);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handlePurchase(PrintWriter out, String item, int quantity) {
            // Ask for approval from the seller
            System.out.println("Buyer nodeID" + clientId + " requests to buy " + quantity + " of " + item);
            System.out.print("Do you approve the purchase? (yes/no): ");
            Scanner scanner = new Scanner(System.in);
            String approval = scanner.nextLine().trim().toLowerCase();

            if ("yes".equals(approval)) {
                lock.lock();
                try {
                    int stock = inventory.getOrDefault(item, 0);
                    if (stock >= quantity) {
                        inventory.put(item, stock - quantity);
                        out.println("Purchase successful! " + quantity + " units of " + item + " bought.");
                        notifyAllClients(item, stock - quantity);
                    } else {
                        out.println("Purchase failed. Insufficient stock.");
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                out.println("Purchase rejected.");
            }
        }

        private void notifyAllClients(String item, int remainingStock) {
            System.out.println(item + " stock updated: " + remainingStock + " left.");
            // Notify other buyers about the stock update
            for (PrintWriter buyerOut : buyers.values()) {
                buyerOut.println(item + " stock updated: " + remainingStock + " left.");
            }
        }
    }

    public static void main(String[] args) {
        new Seller().startServer();
    }
}
