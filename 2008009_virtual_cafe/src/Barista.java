import Helpers.Coffee;
import Helpers.Kitchen;
import Helpers.Tea;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class Barista {
    private static final int COFFEE_DELAY = 45000;
    private static final int TEA_DELAY = 30000;
    public static final int PORT = 9090;

    private ServerSocket serverSocket;

    private final Runnable coffeeBrewer = createBrewer("Coffee", COFFEE_DELAY);
    private final Runnable teaBrewer = createBrewer("Tea", TEA_DELAY);

    public Barista() {
        initializeServerSocket();
        startBrewing();
        startServer();
    }

    private void initializeServerSocket() {
        try {
            this.serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            handleInitializationError(e, "server socket");
        }
    }

    private void startServer() {
        try {
            System.out.println("The Virtual Café is now opened!");

            while (true) {
                Socket client = this.serverSocket.accept();
                System.out.println("Customer entered Virtual Café");
                Kitchen.printStatusLog();
                Kitchen customer = new Kitchen(client);
                Thread thread = new Thread(customer);
                thread.start();
            }
        } catch (IOException e) {
            handleServerError(e);
        }
    }

    private Runnable createBrewer(String beverageType, int delay) {
        return () -> {
            while (true) {
                try {
                    brewAndServe(beverageType, delay);
                } catch (InterruptedException e) {
                    handleBrewerInterruption(beverageType);
                }
            }
        };
    }

    private void brewAndServe(String beverageType, int delay) throws InterruptedException {
        if ("Coffee".equals(beverageType)) {
            Kitchen.takeWaitingAreaCoffees();
            Thread.sleep(delay);
            Coffee coffee = Kitchen.endBrewingCoffee();
            Kitchen.addCoffeeToArea(coffee.id);
        } else if ("Tea".equals(beverageType)) {
            Kitchen.takeWaitingAreaTeas();
            Thread.sleep(delay);
            Tea tea = Kitchen.endBrewingTea();
            Kitchen.addTea(tea.id);
        }
    }

    private void startBrewing() {
        new Thread(coffeeBrewer).start();
        new Thread(coffeeBrewer).start();
        new Thread(teaBrewer).start();
        new Thread(teaBrewer).start();
    }

    private void closeServerSocket() {
        try {
            if (this.serverSocket != null && !this.serverSocket.isClosed()) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            handleClosingError(e, "server socket");
        }
    }

    private void handleInitializationError(Exception e, String component) {
        e.printStackTrace();
        System.out.println("Failed to initialize " + component);
        System.out.println("Terminating JVM...");
        System.exit(-1);
    }

    private void handleServerError(IOException e) {
        closeServerSocket();
        System.out.println("Failed to start server");
    }

    private void handleBrewerInterruption(String beverageType) {
        System.out.println(beverageType + " maker was interrupted");
    }

    private void handleClosingError(IOException e, String component) {
        System.out.println("Failed to close " + component);
    }

    public static void main(String[] args) {
        new Barista();
    }
}
