import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class Seller {
    private static final int PORT = 12345;
    private static final int SELL_DURATION = 60000; // 60 seconds
    private static final String[] ITEMS = {"flower", "sugar", "potato", "oil"};
    private final ConcurrentHashMap<String, Integer> inventory = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();

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

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                // Notify the client of available items
                out.println("Welcome! Available items: " + inventory);

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
        }

        private void notifyAllClients(String item, int remainingStock) {
            System.out.println(item + " stock updated: " + remainingStock + " left.");
        }
    }

    public static void main(String[] args) {
        new Seller().startServer();
    }
}
