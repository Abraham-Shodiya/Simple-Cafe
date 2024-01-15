package Helpers;

public class Order {
    public final String owner;
    public final String id;
    public int finalTeas;
    public int finalCofees;
    public int teas = 0;
    public int coffees = 0;

    public Order(String CustID, String CustName, int tLim, int coffeeLimit) {
        this.id = CustID;
        this.owner = CustName;
        this.finalTeas = tLim;
        this.finalCofees = coffeeLimit;
    }

    public String addordersTO(Order order) {
        this.finalTeas += order.finalTeas;
        this.finalCofees += order.finalCofees;
        String tempOwner = this.owner;
        return "> last order" + tempOwner + " was changed to (" + Kitchen.changeOrderFormat("", this.finalCofees, this.finalTeas, ")");
    }

    public synchronized boolean putCofees() {
        if (this.coffees >= this.finalCofees) {
            return false;
        } else {
            ++this.coffees;
            return true;
        }
    }

    public synchronized boolean putTeasTo() {
        if (this.teas >= this.finalTeas) {
            return false;
        } else {
            ++this.teas;
            return true;
        }
    }

    public String FinalizeOrder() {
        String tempOwner = this.owner;
        return "> order made for " + tempOwner + " (" + Kitchen.changeOrderFormat("", this.finalCofees, this.finalTeas, ")");
    }

    public String deliverOrder() {
        String tempOwner = this.owner;
        return "> order made to " + tempOwner + " (" + Kitchen.changeOrderFormat("", this.finalCofees, this.finalTeas, ")");
    }

    public boolean finishedOrder() {
        return this.coffees == this.finalCofees && this.teas == this.finalTeas;
    }
}
