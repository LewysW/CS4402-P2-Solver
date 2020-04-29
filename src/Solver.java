import java.util.*;

public abstract class Solver {
    //Hash map to store variable assignments, a missing key has not yet had a value assigned
    private LinkedHashMap<Integer, Integer> assignments;
    //List of variable domains to allow for the removal and addition of values
    private ArrayList<LinkedHashSet<Integer>> domains;
    private BinaryCSP binaryCSP;
    private Heuristic heuristic;

    //TODO - remove pointless functions after pseudo code solution is translated



    public Solver(BinaryCSP binaryCSP, Heuristic heuristic) {
        this.domains = new ArrayList<>();
        this.assignments = new LinkedHashMap<>();
        this.binaryCSP = binaryCSP;
        this.heuristic = heuristic;

        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            LinkedHashSet<Integer> domain = new LinkedHashSet<>();

            for (int d = binaryCSP.getLB(v); d <= binaryCSP.getUB(v); d++) {
                domain.add(d);
            }
        }
    }

    private boolean empty(LinkedHashSet<Integer> domain) {
        return domain.isEmpty();
    }

    //Removes a val from domain(var)
    private void remove(int val, int var) {
        domains.get(var).remove(val);
    }

    //TODO
    // private int selectVar()

    //TODO private void undoPruning() (should probably put this in subclasses)

    private int selectVal(LinkedHashSet<Integer> domain) {
        return domain.iterator().next();
    }

    private LinkedHashSet<Integer> domain(int var) {
        return domains.get(var);
    }

    private void assign(int var, int val) {
        assignments.put(var, val);
    }

    private void unassign(int var) {
        assignments.remove(var);
    }

    private boolean completeAssignment() {
        return assignments.size() == binaryCSP.getNoVariables();
    }

    public void printSolution() {
        System.out.println(this.toString());
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("CSP Solution running with the ");
        result.append(heuristic.name());
        result.append(" variable ordering strategy:\n");
        for (Map.Entry<Integer, Integer> entry : assignments.entrySet()) {
            result.append("Var ");
            result.append(entry.getKey());
            result.append(": ");
            result.append(entry.getValue());
            result.append("\n");
        }

        return result.toString();
    }
}
