import java.util.*;

import static java.lang.System.exit;

public abstract class Solver {
    //Hash map to store variable assignments, a missing key has not yet had a value assigned
    protected LinkedHashMap<Integer, Integer> assignments;
    //Unassigned variables
    protected LinkedHashSet<Integer> varList;
    //List of variable domains to allow for the removal and addition of values
    protected ArrayList<LinkedHashSet<Integer>> domains;
    //Constraints which represent arcs in the problem
    protected LinkedHashMap<Arc, BinaryConstraint> constraints;

    protected BinaryCSP binaryCSP; //TODO - consider removing
    protected Heuristic heuristic;

    //TODO - remove pointless functions after pseudo code solution is translated

    public Solver(BinaryCSP binaryCSP, Heuristic heuristic) {
        this.domains = new ArrayList<>();
        this.assignments = new LinkedHashMap<>();
        this.binaryCSP = binaryCSP;
        this.heuristic = heuristic;
        this.varList = new LinkedHashSet<>();
        this.constraints = new LinkedHashMap<>();

        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            this.domains.add(new LinkedHashSet<>());

            varList.add(v);

            for (int d = binaryCSP.getLB(v); d <= binaryCSP.getUB(v); d++) {
                this.domains.get(v).add(d);
            }

            for (BinaryConstraint bc : binaryCSP.getConstraints()) {
                constraints.put(new Arc(bc.getFirstVar(), bc.getSecondVar()), bc);
            }

        }

        System.out.println("ASSIGNED DATA:");
        for (Integer v : varList) {
            LinkedHashSet<Integer> domain = domains.get(v);
            System.out.print("Var " + v + ":");
            for (Integer d : domain) {
                System.out.print(" " + d);
            }
            System.out.println("\n");
        }

        for (BinaryConstraint bc : binaryCSP.getConstraints()) {
            System.out.println(constraints.get(new Arc(bc.getFirstVar(), bc.getSecondVar())));
        }
    }

    protected BinaryConstraint arc(int futureVar, int var) {
        return constraints.get(new Arc(futureVar, var));
    }

    protected boolean revise(BinaryConstraint constraint, Stack<BinaryTuple> pruned) throws DomainEmptyException {
        boolean changed = false;
        int Di = constraint.getFirstVar();
        int Dj = constraint.getSecondVar();

        for (Integer di : domains.get(Di)) {
            boolean supported = false;
            for (Integer dj : domains.get(Dj)) {
                if (satisfies(di, dj, constraint)) {
                    supported = true;
                }
            }
            if (!supported) {
                remove(di, Di);
                changed = true;
                pruned.add(new BinaryTuple(Di, di));
            }
        }

        if (empty(domain(Di))) {
            throw new DomainEmptyException("Domain of variable is empty!\n");
        }

        return changed;
    }

    //TODO - look into if this can be made more efficient by storing as a map and hashing the pair of values
    protected boolean satisfies(int xi, int xj, BinaryConstraint c) {
        for (BinaryTuple tuple : c.getTuples()) {
            if (tuple.matches(xi, xj)) {
                return true;
            }
        }
        return false;
    }

    protected boolean empty(LinkedHashSet<Integer> domain) {
        return domain.isEmpty();
    }

    //Removes a val from domain(var)
    protected void remove(int val, int var) {
        domains.get(var).remove(val);
    }

    //Replaces a val in domain(var)
    protected void restore(int val, int var) {
        domains.get(var).add(val);
    }

    protected int selectVar() {
        if (assignments.size() == binaryCSP.getNoVariables()) {
            System.out.println("Error: selectVar() should not have been called!\n");
            exit(1);
        }

        if (heuristic == Heuristic.ASCENDING) {
            return assignments.size();
        } else {
            //TODO
            // return variable with smallest-domain
            System.out.println("Error: smallest-domain first not implemented!\n");
            exit(1);
            //Stops compiler complaining
            return -1;
        }
    }

    protected int selectVal(LinkedHashSet<Integer> domain) {
        return domain.iterator().next();
    }

    protected LinkedHashSet<Integer> domain(int var) {
        return domains.get(var);
    }

    protected void assign(int var, int val) {
        assignments.put(var, val);
        varList.remove(var);
    }

    protected void unassign(int var) {
        assignments.remove(var);
        varList.add(var);
    }

    protected boolean completeAssignment() {
        return (assignments.size() == binaryCSP.getNoVariables());
    }

    protected boolean solution() {
        //If list of unassigned variables is empty
        for (BinaryConstraint constraint : constraints.values()) {
            int val1 = assignments.get(constraint.getFirstVar());
            int val2 = assignments.get(constraint.getSecondVar());

            if (!satisfies(val1, val2, constraint)) {
                return false;
            }
        }

        return true;
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
