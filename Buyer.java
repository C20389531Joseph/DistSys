import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Buyer {
    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            // Read welcome message from server
            System.out.println(in.readLine());

            String command;
            while (true) {
                System.out.println("Enter a command (LIST_ITEMS, BUY_REQUEST:ITEM:QUANTITY, EXIT): ");
                command = scanner.nextLine();
                out.println(command);

                if (command.equals("EXIT")) {
                    break;
                }

                // Print server response
                System.out.println("Response: " + in.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
