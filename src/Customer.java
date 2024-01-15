import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public final class Customer {
    private static final int SERVER_PORT = 9090;
    private static final String SERVER_HOST = "localhost";

    private Socket customerSocket;
    private BufferedReader serverInput;
    private BufferedReader userInput;
    private PrintWriter serverOutput;
    private Thread readThread;
    private Thread writeThread;

    private final Runnable serverReader = () -> {
        while (customerSocket.isConnected()) {
            try {
                String responseFromBarista = serverInput.readLine();
                handleServerResponse(responseFromBarista);
            } catch (IOException e) {
                e.printStackTrace(); // Handle or log the exception appropriately
            }
        }
    };

    private final Runnable serverWriter = () -> {
        while (customerSocket.isConnected()) {
            try {
                String customerOrder = userInput.readLine();
                sendCustomerOrder(customerOrder);
            } catch (IOException e) {
                System.out.println("Failed to retrieve server response in server writer");
            }
        }
    };

    public Customer() {
        try {
            initialize();
            startThreads();
        } catch (IOException e) {
            handleInitializationError(e);
        }
    }

    private void initialize() throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> cleanExit()));
        this.customerSocket = new Socket(SERVER_HOST, SERVER_PORT);
        this.serverInput = new BufferedReader(new InputStreamReader(this.customerSocket.getInputStream()));
        this.userInput = new BufferedReader(new InputStreamReader(System.in));
        this.serverOutput = new PrintWriter(this.customerSocket.getOutputStream(), true);
        setCustomerName();
    }

    private void startThreads() {
        this.readThread = new Thread(this.serverReader);
        this.writeThread = new Thread(this.serverWriter);
        this.readThread.start();
        this.writeThread.start();
    }

    private void cleanExit() {
        try {
            interruptThreads();
            closeResources();
        } catch (IOException e) {
            System.out.println("Failed to close customer resources");
        }
    }

    private void interruptThreads() {
        if (this.readThread != null) this.readThread.interrupt();
        if (this.writeThread != null) this.writeThread.interrupt();
    }

    private void closeResources() throws IOException {
        if (this.serverOutput != null) this.serverOutput.close();
        if (this.serverInput != null) this.serverInput.close();
        if (this.customerSocket != null && !this.customerSocket.isClosed()) this.customerSocket.close();
    }

    private void handleServerResponse(String responseFromBarista) {
        if (responseFromBarista != null) {
            if (responseFromBarista.toLowerCase().contains("thank")) {
                System.out.println(responseFromBarista);
                exitRestaurant();
            } else if (responseFromBarista.toLowerCase().contains("order status for")) {
                displayOrderStatus(responseFromBarista);
            } else {
                System.out.println(responseFromBarista);
            }
        }
    }

    private void displayOrderStatus(String responseFromBarista) {
        String[] orderStatuses = responseFromBarista.split("%");
        for (String status : orderStatuses) {
            System.out.println(status);
        }
    }

    private void sendCustomerOrder(String customerOrder) {
        if (!customerOrder.isEmpty()) {
            this.serverOutput.println(customerOrder);
        }
    }

    private void setCustomerName() {
        try {
            String welcomeMessage = this.serverInput.readLine();
            System.out.println(welcomeMessage);

            while (customerSocket.isConnected()) {
                String userName = this.userInput.readLine();
                this.serverOutput.println(userName);
                welcomeMessage = this.serverInput.readLine();
                System.out.println(welcomeMessage);

                if (welcomeMessage != null && welcomeMessage.toLowerCase().contains("welcome")) {
                    return;
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to retrieve server response");
            exitRestaurant();
        }
    }

    private static void exitRestaurant() {
        System.exit(0);
    }

    private void handleInitializationError(IOException e) {
        System.out.println("Failed to initialize customer");
        System.out.println("Terminating JVM...");
        System.exit(-1);
    }

    public static void main(String[] args) {
        new Customer();
    }
}
