package Helpers;

public class Bervages {
    public final Name name;
    public String id;

    public Bervages(Name name, String id) {
        this.name = name;
        this.id = id;
    }

    protected enum Name {
        coffee,
        tea;

         Name() {
        }
    }
}
