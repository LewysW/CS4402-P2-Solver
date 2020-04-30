import java.util.*;

import static java.lang.System.exit;

public abstract class Solver {
    //Hash map to store variable assignments, a missing key has not yet had a value assigned
    protected LinkedHashMap<Integer, Integer> assignments;
    //List of variable domains to allow for the removal and addition of values
    protected ArrayList<LinkedHashSet<Integer>> domains;
    //Constraints which represent arcs in the problem
    protected LinkedHashMap<Integer, LinkedHashMap<Integer, BinaryConstraint>> constraints;

    protected BinaryCSP binaryCSP;
    protected Heuristic heuristic;

    protected TreeMap<Integer, LinkedHashSet<Integer>> domainSizes;


    //TODO - remove pointless functions after pseudo code solution is translated

    public Solver(BinaryCSP binaryCSP, Heuristic heuristic) {
        this.domains = new ArrayList<>();
        this.assignments = new LinkedHashMap<>();
        this.binaryCSP = binaryCSP;
        this.heuristic = heuristic;
        this.constraints = new LinkedHashMap<>();
        this.domainSizes = new TreeMap<>();

        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            this.domains.add(new LinkedHashSet<>());

            for (int d = binaryCSP.getLB(v); d <= binaryCSP.getUB(v); d++) {
                this.domains.get(v).add(d);
            }

            if (heuristic == Heuristic.SMALLEST_DOMAIN_FIRST) {
                if (!domainSizes.containsKey(domains.get(v).size())) {
                    domainSizes.put(domains.get(v).size(), new LinkedHashSet<>());
                }

                domainSizes.get(domains.get(v).size()).add(v);
            }

            for (BinaryConstraint bc : binaryCSP.getConstraints()) {
                if (!constraints.containsKey(bc.getFirstVar())) {
                    constraints.put(bc.getFirstVar(), new LinkedHashMap<>());
                }

                constraints.get(bc.getFirstVar()).put(bc.getSecondVar(), bc);
            }
        }

        System.out.println(domainSizes);
    }


    protected BinaryConstraint arc(int futureVar, int var) {
        return constraints.get(var).get(futureVar);
    }

    protected boolean revise(BinaryConstraint constraint, Stack<BinaryTuple> pruned) throws DomainEmptyException {
        boolean changed = false;
        int Di_index = constraint.getSecondVar();
        LinkedHashSet<Integer> Di = (LinkedHashSet<Integer>) domains.get(constraint.getSecondVar()).clone();
        LinkedHashSet<Integer> Dj = domains.get(constraint.getFirstVar());

        for (Integer di : Di) {
            boolean supported = false;
            for (Integer dj : Dj) {
                if (satisfies(di, dj, constraint)) {
                    supported = true;
                }
            }
            if (!supported) {
                //remove di from Di (not the clone of Di)
                remove(di, Di_index);
                changed = true;
                pruned.add(new BinaryTuple(Di_index, di));
            }
        }

        if (empty(domain(constraint.getSecondVar()))) {
            throw new DomainEmptyException("Domain of variable is empty!\n");
        }

        return changed;
    }


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
        if (heuristic == Heuristic.SMALLEST_DOMAIN_FIRST) {
            updateDomainSizeMapping(domains.get(var).size(), domains.get(var).size() - 1, var);
        }

        domains.get(var).remove(val);
    }

    //Replaces a val in domain(var)
    protected void restore(int val, int var) {
        //Update size of variables domain after pruning is undone
        if (heuristic == Heuristic.SMALLEST_DOMAIN_FIRST) {
            updateDomainSizeMapping(domains.get(var).size(), domains.get(var).size() + 1, var);
        }

        domains.get(var).add(val);
    }

    protected int selectVar(LinkedHashSet<Integer> varList) {
        if (heuristic == Heuristic.ASCENDING) {
            return assignments.size();
        } else {
            for (Integer key : domainSizes.keySet()) {
                Iterator<Integer> iterator = domainSizes.get(key).iterator();

                while (iterator.hasNext()) {
                    Integer var = iterator.next();

                    if (!assignments.containsKey(var)) {
                        return var;
                    }
                }
            }
        }

        //Error!
        return -1;
    }

    protected int selectVal(LinkedHashSet<Integer> domain) {
        return domain.iterator().next();
    }

    protected LinkedHashSet<Integer> domain(int var) {
        return domains.get(var);
    }

    protected void assign(int var, int val, Stack<BinaryTuple> pruned) {
        assignments.put(var, val);

        LinkedHashSet<Integer> domain = (LinkedHashSet<Integer>) domains.get(var).clone();

        for (int d : domain) {
            if (d != val) {
                remove(d, var);
                pruned.push(new BinaryTuple(var, d));
            }
        }
        System.out.println("Domains: " + domains);
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

    protected void updateDomainSizeMapping(int currentSize, int newSize, int var) {
        //If there exists domains of that size, and var is one of them
        if (domainSizes.containsKey(currentSize)
                && domainSizes.get(currentSize).contains(var)) {
            //Remove var from that domain size
            domainSizes.get(currentSize).remove(var);

            //If there are no longer any variables with that size domain,
            //then the delte the domain size
            if (domainSizes.get(currentSize).isEmpty()) {
                domainSizes.remove(currentSize);
            }
        }

        //If previous variables with that domain size, add domain size
        if (!domainSizes.containsKey(newSize)) {
            domainSizes.put(newSize, new LinkedHashSet<>());
        }

        //Associate var with domain of size newSize
        domains.get(newSize).add(var);
    }
}
