package Helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class Kitchen implements Runnable {
    private final Socket serverSockets;
    private BufferedReader bufferReaderIN;
    private PrintWriter printerWriter;
    private String name;
    private String id;
    private static final Set<Kitchen> customer = ConcurrentHashMap.newKeySet();
    private static final BlockingQueue<Order> orders = new LinkedBlockingDeque<>();
    private static final BlockingQueue<Tea> queueForTea = new LinkedBlockingDeque<>();
    private static final BlockingQueue<Coffee> queueForCoffee = new LinkedBlockingDeque<>();
    private static final BlockingQueue<Coffee> coffeeBrewAre = new ArrayBlockingQueue<>(2);
    private static final BlockingQueue<Tea> teaBrewAre = new ArrayBlockingQueue<>(2);

    public Kitchen(Socket socket) { // constructor
        this.serverSockets = socket;

        try {
            this.bufferReaderIN = new BufferedReader(new InputStreamReader(this.serverSockets.getInputStream()));
            this.printerWriter = new PrintWriter(this.serverSockets.getOutputStream(), true);
            this.getName();
        } catch (IOException exception) {
            exception.printStackTrace();
            System.out.println("communication between client and server cannot be established");
        }

    }

    public void run() {
        while(true) {
            try {
                if (this.serverSockets.isConnected()) {
                    String readLine = this.bufferReaderIN.readLine();
                    if (readLine != null && !readLine.equalsIgnoreCase("exit")) {
                        this.inputProcess(readLine);
                        continue;
                    }
                }
            } catch (IOException ex) {
                System.out.println("kitchen '" + this.id + "' couldnt get " + this.name + " order");
            }

            this.closeDownKitchen();
            return;
        }
    }

    public void getName() {
        this.printerWriter.println("--> Hello, can I grab your name please?");

        while(true) {
            try {
                this.name = this.bufferReaderIN.readLine();
                this.id = String.valueOf((new Random()).nextInt(99999999));
                if (this.addCustomer(this.id)) {
                    this.printerWriter.println("--> Welcome to the cafe " + this.name + ", what would you like to order");
                    return;
                }

                this.printerWriter.println("-->Enter a different valid name");
            } catch (IOException var2) {
                System.out.println("could not put a name to a customer");
            }
        }
    }

    public boolean addCustomer(String name) {
        Iterator<Kitchen> kitchenIterator = customer.iterator();

        Kitchen kitchen;
        do {
            if (!kitchenIterator.hasNext()) {
                Kitchen.customer.add(this);
                printStatusLog();
                return true;
            }

            kitchen = kitchenIterator.next();
        } while(!kitchen.customerID().equals(name));

        return false;
    }

    public void inputProcess(String input) {
        String inputConvertedToLowerCase = input.toLowerCase();
        Kitchen.Type_of_inputs typeOfOrder;
        if (inputConvertedToLowerCase.matches("\\border\\b [1-9][0-9]* \\bteas?\\b \\band\\b [1-9][0-9]* \\bcoffees?\\b")) {
            typeOfOrder = Kitchen.Type_of_inputs.Tea_Coffee;
        } else if (inputConvertedToLowerCase.matches("\\border\\b [1-9][0-9]* \\bcoffees?\\b \\band\\b [1-9][0-9]* \\bteas?\\b")) {
            typeOfOrder = Kitchen.Type_of_inputs.Coffee_Tea;
        } else if (inputConvertedToLowerCase.matches("\\border\\b [1-9][0-9]* \\bteas?\\b")) {
            typeOfOrder = Kitchen.Type_of_inputs.JustTea;
        } else {
            if (!inputConvertedToLowerCase.matches("\\border\\b [1-9][0-9]* \\bcoffees?\\b")) { //JustCoffee
                if (inputConvertedToLowerCase.equals("exit")) {
                    this.printerWriter.println("--> Thanks for coming!!!!!");
                    return;
                }

                if (inputConvertedToLowerCase.equals("order status")) {
                    this.printerWriter.println(this.getOrderStatus());
                    return;
                }

                this.printerWriter.println("--> Incorrect order please attempt again");
                return;
            }

            typeOfOrder = Kitchen.Type_of_inputs.JustCoffee;
        }

        this.orderPlacing(inputConvertedToLowerCase, typeOfOrder);
    }

    public static String changeOrderFormat(String beginning, int sizeOfCoffee, int sizeOfTea, String terminate) {
        String teaFormat = String.valueOf(sizeOfTea);
        String coffeeFormat = String.valueOf(sizeOfCoffee);

        if (sizeOfTea == 0 && sizeOfCoffee == 0) {
            return "";
        } else {
            coffeeFormat = coffeeFormat + (sizeOfCoffee > 1 ? " coffees" : " coffee");
            teaFormat = teaFormat + (sizeOfTea > 1 ? " teas" : " tea");
            String update;
            if (sizeOfCoffee > 0 && sizeOfTea > 0) {
                update = beginning + coffeeFormat + " and " + teaFormat;
            } else if (sizeOfTea > 0) {
                update = beginning + teaFormat;
            } else {
                update = beginning + coffeeFormat;
            }

            update = update + terminate;
            return update;
        }
    }

    private void orderPlacing(String order, Kitchen.Type_of_inputs orderType) {
        String string = order;
        String[] tokenToBeRemoved = new String[]{"teas", "tea" ,"order ", " and ", "coffees", "coffee"};
        String[] strings = tokenToBeRemoved;
        int len = tokenToBeRemoved.length;

        int teaSize;
        for(teaSize = 0; teaSize < len; ++teaSize) {
            String string1 = strings[teaSize];
            string = string.replaceAll(string1, "");
        }

        strings = string.split(" ");
        int[] items = new int[]{Integer.parseInt(strings[0]), 0};
        if (strings.length == 2) {
            items[1] = Integer.parseInt(strings[1]);
        }

        int coffeeSize = 0;
        teaSize = 0;

        switch (orderType) {
            case JustTea -> teaSize = items[0];
            case JustCoffee -> coffeeSize = items[0];
            case Tea_Coffee -> {
                teaSize = items[0];
                coffeeSize = items[1];
            }
            case Coffee_Tea -> {
                coffeeSize = items[0];
                teaSize = items[1];
            }
            default -> {
                System.out.println("Invalid order");
                return;
            }
        }

        this.addToQueues(new Order(this.id, this.name, teaSize, coffeeSize));
    }

    private void addToQueues(Order order) {
        boolean isOrderNew = true;
        String stringBuilder = "";
        Order previousOrder = this.findCustomerOrder(order.id);
        if (previousOrder != null) {
            stringBuilder = previousOrder.addordersTO(order);
            isOrderNew = false;
        }

        if (isOrderNew) {
            orders.add(order);
        }

        int i;
        for(i = 0; i < order.finalTeas; ++i) {
            queueForTea.add(new Tea(order.id));
        }

        for(i = 0; i < order.finalCofees; ++i) {
            queueForCoffee.add(new Coffee(order.id));
        }

        this.printerWriter.println(order.FinalizeOrder());
        if (!stringBuilder.isEmpty()) {
            this.printerWriter.println(stringBuilder);
        }

        printStatusLog();
    }



    private String BrewingAreaStatus(int area) {
        String terminate = "";
        int sizeOfTeas = 0;
        int sizeOfCoffees = 0;
        Object teaQueue;
        Object coffeeQueue;
        switch(area) {
            case 0:
                teaQueue = queueForTea;
                coffeeQueue = queueForCoffee;
                terminate = " bufferReaderIN waiting area";
                break;
            case 1:
                teaQueue = teaBrewAre;
                coffeeQueue = coffeeBrewAre;
                terminate = " being prepared right now";
                break;
            case 2:
                return this.getTray();
            default:
                System.out.println("incorrect area status");
                teaQueue = new LinkedBlockingDeque();
                coffeeQueue = new LinkedBlockingDeque();
        }

        Iterator iterateThrough = ((BlockingQueue)teaQueue).iterator();

        while(iterateThrough.hasNext()) {
            Tea tea = (Tea)iterateThrough.next();
            if (tea.id.equals(this.id)) {
                ++sizeOfTeas;
            }
        }

        iterateThrough = ((BlockingQueue)coffeeQueue).iterator();

        while(iterateThrough.hasNext()) {
            Coffee coffee = (Coffee)iterateThrough.next();
            if (coffee.id.equals(this.id)) {
                ++sizeOfCoffees;
            }
        }

        return changeOrderFormat("-->", sizeOfCoffees, sizeOfTeas, terminate);
    }

    private String getTray() {
        int teaSize = 0;
        int coffeeSize = 0;
        Order order = this.findCustomerOrder(this.id);
        if (order != null) {
            teaSize = order.teas;
            coffeeSize = order.coffees;
        }

        return changeOrderFormat("- ", coffeeSize, teaSize, " at the moment, bufferReaderIN the tray");
    }

    private String getOrderStatus() {
        String status = "--> Order status for " + this.name + ":%";
        String[] statusArray = new String[]{this.BrewingAreaStatus(0), this.BrewingAreaStatus(1), this.BrewingAreaStatus(2)};
        if (statusArray[0].isEmpty() && statusArray[1].isEmpty() && statusArray[2].isEmpty()) {
            status = status + "- No order found for " + this.name;
            return status;
        } else {
            String[] var3 = statusArray;
            int var4 = statusArray.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String s = var3[var5];
                if (!s.isEmpty()) {
                    status = status + s + "%";
                }
            }

            return status;
        }
    }

    public static void completeCustomerOrder(Order order) {
        String id = order.id;

        for (Kitchen customer : customer) {
            if (customer.customerID().equals(id)) {
                customer.thePrintWriter().println(order.deliverOrder());
                orders.removeIf((s) -> s.id.equals(id));
                DeleteWaitingAreaCustomers(id);
                break;
            }
        }

        printStatusLog();
    }

    private Order findCustomerOrder(String requiredCustomerID) {
        Iterator orders = Kitchen.orders.iterator();

        Order customerOder;
        do {
            if (!orders.hasNext()) {
                return null;
            }

            customerOder = (Order)orders.next();
        } while(!customerOder.id.equals(requiredCustomerID));

        return customerOder;
    }

    public static boolean addCoffeeToArea(String cofeeID) {
        Iterator orders = Kitchen.orders.iterator();

        Order order;
        do {
            if (!orders.hasNext()) {
                printStatusLog();
                return false;
            }

            order = (Order)orders.next();
        } while(!order.id.equals(cofeeID) || !order.putCofees());

        if (order.finishedOrder()) {
            System.out.println(order.owner + "'s order has been completed");
            completeCustomerOrder(order);
            orders.remove();
        }

        return true;
    }

    public static boolean addTea(String itemId) {
        Iterator orders = Kitchen.orders.iterator();

        Order order;
        do {
            if (!orders.hasNext()) {
                printStatusLog();
                return false;
            }

            order = (Order)orders.next();
        } while(!order.id.equals(itemId) || !order.putTeasTo());

        if (order.finishedOrder()) {
            System.out.println(order.owner + "'s order is finished");
            completeCustomerOrder(order);
            orders.remove();
        }

        return true;
    }

    public void removeCustomer() {
        DeleteWaitingAreaCustomers(this.id);
        this.reUseBrew();
        this.reUseTray();
        orders.removeIf((s) -> {
            return s.id.equals(this.id);
        });
        Iterator Customers = customer.iterator();

        Kitchen customer;
        do {
            if (!Customers.hasNext()) {
                return;
            }

            customer = (Kitchen)Customers.next();
        } while(!customer.customerID().equals(this.id));

        Customers.remove();
    }

    private static void DeleteWaitingAreaCustomers(String customerID) {
        Iterator teasInQueue = queueForTea.iterator();

        while(teasInQueue.hasNext()) {
            Tea teas = (Tea)teasInQueue.next();
            if (teas.id.equals(customerID)) {
                teasInQueue.remove();
            }
        }

        Iterator coffeeinQueue = queueForCoffee.iterator();

        while(coffeeinQueue.hasNext()) {
            Coffee coffees = (Coffee)coffeeinQueue.next();
            if (coffees.id.equals(customerID)) {
                coffeeinQueue.remove();
            }
        }

    }

    private void reUseTray() {

        for (Order order : Kitchen.orders) {
            if (order.id.equals(this.id)) {
                int i;
                String customerID;
                Order CustomerOrders;
                for (i = 0; i < order.coffees; ++i) {
                    customerID = this.findCoffeeFromWaitingArea();
                    if (customerID.isEmpty()) {
                        break;
                    }

                    CustomerOrders = this.findCustomerOrder(customerID);
                    if (CustomerOrders != null && addCoffeeToArea(customerID)) {
                        System.out.println("tea that was for " + this.name + "'s tray has been given to " + CustomerOrders.owner);
                    }
                }

                for (i = 0; i < order.teas; ++i) {
                    customerID = this.findTeaFromWaitingArea();
                    if (customerID.isEmpty()) {
                        return;
                    }

                    CustomerOrders = this.findCustomerOrder(customerID);
                    if (CustomerOrders != null && addTea(customerID)) {
                        System.out.println("tea that was for " + this.name + "'s tray has been given to " + CustomerOrders.owner);
                    }
                }

                return;
            }
        }

    }


    private String findTeaFromWaitingArea() {
        String emptyString = "";
        Iterator<Tea> teaQueueItr = queueForTea.iterator();
        if (teaQueueItr.hasNext()) {
            Tea tea = teaQueueItr.next();
            teaQueueItr.remove();
            return tea.id;
        } else { return emptyString; }
    }

    private void reUseBrew() {

        for (Coffee coffee : coffeeBrewAre) {
            if (coffee.id.equals(this.id)) {
                String newID = this.findCoffeeFromWaitingArea();
                if (newID != null) {
                    coffee.id = newID;
                    Order order = this.findCustomerOrder(newID);
                    if (order != null) {
                        System.out.println("coffee for " + this.name + " was given to " + order.owner);
                    }
                }
            }
        }

        for (Tea tea : teaBrewAre) {
            if (tea.id.equals(this.id)) {
                String newID = this.findTeaFromWaitingArea();
                if (newID != null) {
                    tea.id = newID;
                    Order order = this.findCustomerOrder(newID);
                    if (order != null) {
                        System.out.println("tea for" + this.name + " was given to" + order.owner);
                    }
                }
            }
        }

    }

    private String findCoffeeFromWaitingArea() {
        String emptyString = "";
        Iterator<Coffee> coffeeQueueItr = queueForCoffee.iterator();
        if (coffeeQueueItr.hasNext()) {
            Coffee coffee = coffeeQueueItr.next();
            coffeeQueueItr.remove();
            return coffee.id;
        } else {
            return emptyString;
        }
    }

    public static void takeWaitingAreaCoffees() {
        try {
            coffeeBrewAre.put(queueForCoffee.take());
            printStatusLog();
        } catch (Exception var1) {
            System.out.println("A coffee maker was interrupted");
        }

    }

    public static void takeWaitingAreaTeas() {
        try {

            teaBrewAre.put(queueForTea.take());
            printStatusLog();
        } catch (InterruptedException interruptedException) { System.out.println("Tea machine was Stopped!!!!"); }

    }

    public static Coffee endBrewingCoffee() {
        try {
            return coffeeBrewAre.take();
        } catch (InterruptedException var1) {
            System.out.println("Coffee machine was Stopped!!!!");
            return new Coffee("-2");
        }
    }

    public static Tea endBrewingTea() {
        try {
            return teaBrewAre.take();
        } catch (InterruptedException var1) {
            System.out.println("Tea machine was Stopped!!!!");
            return new Tea("-2");
        }
    }

    public static void printStatusLog() {
        String dateFormat = "dd/MM/yyyy HH:mm:ss";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat);
        LocalDateTime localDateTime = LocalDateTime.now();
        String[] Date = dateTimeFormatter.format(localDateTime).split(" ");
        int clients = customer.size();
        int sizingOfKitchenOrder = Kitchen.orders.size();
        int[] sizeOfTray = new int[]{0, 0};

        Order order;
        for(Iterator orders = Kitchen.orders.iterator(); orders.hasNext(); sizeOfTray[1] += order.teas) {
            order = (Order)orders.next();
            sizeOfTray[0] += order.coffees;
        }

        int[] sizeOfBrewing = new int[]{coffeeBrewAre.size(), teaBrewAre.size()};
        int[] SizeOfWaiting = new int[]{queueForCoffee.size(), queueForTea.size()};


        System.out.println("--> Order Status");
        System.out.println("Date: " + Date[0]);
        System.out.println("Time: " + Date[1]);
        System.out.println("Teas bufferReaderIN waiting area: " + SizeOfWaiting[1]);
        System.out.println("Coffees bufferReaderIN brewing area: " + sizeOfBrewing[0]);
        System.out.println("Teas bufferReaderIN brewing area: " + sizeOfBrewing[1]);
        System.out.println("Clients bufferReaderIN the cafe: " + clients);
        System.out.println("Clients waiting for orders: " + sizingOfKitchenOrder);
        System.out.println("Coffees bufferReaderIN waiting area: " + SizeOfWaiting[0]);
        System.out.println("Coffees bufferReaderIN tray area: " + sizeOfTray[0]);
        System.out.println("Teas bufferReaderIN tray area: " + sizeOfTray[1]);
    }

    public String customerID() {
        return this.id;
    }

    public PrintWriter thePrintWriter() {
        return this.printerWriter;
    }

    public void closeDownKitchen() {
        this.removeCustomer();
        System.out.println(this.name + " has left the cafe");
        this.printerWriter.println( this.name + "Thank you soo much for coming to the cafe\nGoodBye!!!");

        try {
            if (this.serverSockets != null) {
                this.serverSockets.close();
            }

            this.printerWriter.close();
            this.bufferReaderIN.close();
        } catch (IOException exception) {
            System.out.println("Error");
        }

        printStatusLog();
    }

    private enum Type_of_inputs {
        Tea_Coffee,
        Coffee_Tea,
        JustTea,
        JustCoffee;

        Type_of_inputs() {
        }
    }
}
