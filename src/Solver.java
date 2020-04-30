import java.util.*;

import static java.lang.System.exit;

public abstract class Solver {
    //Hash map to store variable assignments, a missing key has not yet had a value assigned
    protected LinkedHashMap<Integer, Integer> assignments;
    //List of variable domains to allow for the removal and addition of values
    protected ArrayList<LinkedHashSet<Integer>> domains;
    //Constraints which represent arcs in the problem
    protected LinkedHashMap<Integer, LinkedHashMap<Integer, BinaryConstraint>> constraints;

    protected BinaryCSP binaryCSP; //TODO - consider removing
    protected Heuristic heuristic;

    //TODO - remove pointless functions after pseudo code solution is translated

    public Solver(BinaryCSP binaryCSP, Heuristic heuristic) {
        this.domains = new ArrayList<>();
        this.assignments = new LinkedHashMap<>();
        this.binaryCSP = binaryCSP;
        this.heuristic = heuristic;
        this.constraints = new LinkedHashMap<>();

        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            this.domains.add(new LinkedHashSet<>());

            for (int d = binaryCSP.getLB(v); d <= binaryCSP.getUB(v); d++) {
                this.domains.get(v).add(d);
            }

            for (BinaryConstraint bc : binaryCSP.getConstraints()) {
                if (!constraints.containsKey(bc.getFirstVar())) {
                    constraints.put(bc.getFirstVar(), new LinkedHashMap<>());
                }

                constraints.get(bc.getFirstVar()).put(bc.getSecondVar(), bc);
            }


        }

        System.out.println("ASSIGNED DATA:");
        System.out.println("Variables:");
        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            LinkedHashSet<Integer> domain = domains.get(v);
            System.out.print("Var " + v + ":");
            System.out.println("Domain:");
            for (Integer d : domain) {
                System.out.print(" " + d);
            }
            System.out.println("\n");
        }

        System.out.println("Constraints:");
        for (BinaryConstraint bc : binaryCSP.getConstraints()) {
            System.out.println(constraints.get(bc.getFirstVar()).get(bc.getSecondVar()));
        }
    }

    protected BinaryConstraint arc(int futureVar, int var) {
        return constraints.get(var).get(futureVar);
    }

    protected boolean revise(BinaryConstraint constraint, Stack<BinaryTuple> pruned) throws DomainEmptyException {
        boolean changed = false;
        int Di = constraint.getSecondVar();
        int Dj = constraint.getFirstVar();
        Iterator<Integer> iterator = domains.get(constraint.getSecondVar()).iterator();

        while (iterator.hasNext()) {
            Integer di = iterator.next();
            boolean supported = false;
            for (Integer dj : domains.get(Dj)) {
                if (satisfies(di, dj, constraint)) {
                    supported = true;
                }
            }
            if (!supported) {
                iterator.remove();
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

    protected int selectVar(LinkedHashSet<Integer> varList) {
        if (assignments.size() == binaryCSP.getNoVariables()) {
            System.out.println("Error: selectVar() should not have been called!\n");
            exit(1);
        }

        if (heuristic == Heuristic.ASCENDING) {
            return assignments.size();
        } else {
            final int UNINITIALISED = -1;
            int domain = UNINITIALISED;

            //Gets minimum domain first
            for (int v : varList) {
                if (domain == UNINITIALISED || domains.get(v).size() > domains.get(domain).size()) {
                    domain = v;
                }
            }

            return domain;
        }
    }

    protected int selectVal(LinkedHashSet<Integer> domain) {
        return domain.iterator().next();
    }

    protected LinkedHashSet<Integer> domain(int var) {
        return domains.get(var);
    }

    protected void assign(int var, int val, Stack<BinaryTuple> pruned) {
        assignments.put(var, val);
        Iterator<Integer> iterator = domains.get(var).iterator();
        while (iterator.hasNext()) {
            Integer integer = iterator.next();

            if (integer != val) {
                iterator.remove();
                pruned.push(new BinaryTuple(var, integer));
            }
        }
    }

    protected void unassign(int var) {
        assignments.remove(var);
    }

    protected boolean completeAssignment() {
        return (assignments.size() == binaryCSP.getNoVariables());
    }

    public void printSolution() {
        System.out.println(this.toString());
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("CSP Solution running with the ");
        result.append(heuristic.name());
        result.append(" variable ordering strategy:\n");
        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            result.append("Var ");
            result.append(v);
            result.append(": ");
            result.append(assignments.get(v));
            result.append("\n");
        }

        return result.toString();
    }

    //Restores pruned value to variable
    protected void undoPruning(Stack<BinaryTuple> pruned) {
        while (!pruned.empty()) {
            int val = pruned.peek().getVal2();
            int var = pruned.pop().getVal1();
            restore(val, var);
        }
    }
}
