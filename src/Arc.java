import java.util.Objects;

//TODO - cite in report
//https://stackoverflow.com/questions/14677993/how-to-create-a-hashmap-with-two-keys-key-pair-value
public class Arc {
    private final int var1;
    private final int var2;

    public Arc(int var1, int var2) {
        this.var1 = var1;
        this.var2 = var2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Arc)) return false;

        Arc arc = (Arc) obj;
        return var1 == arc.var1 && var2 == arc.var2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(var1, var2);
    }
}
