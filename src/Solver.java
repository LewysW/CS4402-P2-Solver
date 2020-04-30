import java.util.*;

import static java.lang.System.exit;

/**
 * Solver base class from which FC and MAC inherit
 */
public abstract class Solver {
    //Hash map to store variable assignments, a missing key has not yet had a value assigned
    protected LinkedHashMap<Integer, Integer> assignments = new LinkedHashMap<>();
    //List of variable domains to allow for the removal and addition of values
    protected ArrayList<LinkedHashSet<Integer>> domains = new ArrayList<>();
    //Constraints which represent arcs in the problem
    protected LinkedHashMap<Integer, LinkedHashMap<Integer, BinaryConstraint>> constraints = new LinkedHashMap<>();
    //Stores map of domain sizes to variables for smallest-domain first ordering
    protected TreeMap<Integer, LinkedHashSet<Integer>> domainSizes = new TreeMap<>();

    //Problem to solve
    protected BinaryCSP binaryCSP;
    //Heuristic strategy used
    protected Heuristic heuristic;

    /**
     * Initialises data structures to attempt to improve access time
     * to variables, domains and constraints
     * @param binaryCSP - problem to access constraints
     * @param heuristic - heuristic technique used (ascending or smallest domain first)
     */
    public Solver(BinaryCSP binaryCSP, Heuristic heuristic) {
        this.binaryCSP = binaryCSP;
        this.heuristic = heuristic;

        //For each variable
        for (int v = 0; v < binaryCSP.getNoVariables(); v++) {
            //Add new domain to domains for variable v
            this.domains.add(new LinkedHashSet<>());

            //Add value d in domain for variable v
            for (int d = binaryCSP.getLB(v); d <= binaryCSP.getUB(v); d++) {
                this.domains.get(v).add(d);
            }

            //If using smallest-domain first ordering
            if (heuristic == Heuristic.SMALLEST_DOMAIN_FIRST) {
                //Initialise map if no other variable has previously had this domain
                if (!domainSizes.containsKey(domains.get(v).size())) {
                    domainSizes.put(domains.get(v).size(), new LinkedHashSet<>());
                }

                //Store key and value representing size of domain and variable with that domain size
                domainSizes.get(domains.get(v).size()).add(v);
            }

            //For each constraint
            for (BinaryConstraint bc : binaryCSP.getConstraints()) {
                //Add entry in map for constraints relating to variable
                if (!constraints.containsKey(bc.getFirstVar())) {
                    constraints.put(bc.getFirstVar(), new LinkedHashMap<>());
                }

                //Add entry to inner map for second variable in constraint
                constraints.get(bc.getFirstVar()).put(bc.getSecondVar(), bc);
            }
        }
    }

    /**
     * Gets arc between two variables
     * @param futureVar - future variable xi
     * @param var - current variable xj
     * @return arc(xi, xj)
     */
    protected BinaryConstraint arc(int futureVar, int var) {
        return constraints.get(var).get(futureVar);
    }

    /**
     * Function to revise domains of variables
     * @param constraint - constraint between variables xi and xj
     * @param pruned - stack storing pruned values
     * @return whether or not change was made
     * @throws DomainEmptyException - throws exception is domain of xi is empty to exit early
     */
    protected boolean revise(BinaryConstraint constraint, Stack<BinaryTuple> pruned) throws DomainEmptyException {
        boolean changed = false;
        int Di_index = constraint.getSecondVar();

        //Gets
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

//                if (heuristic == Heuristic.SMALLEST_DOMAIN_FIRST) {
//                    updateDomainSizeMapping(domains.get(di).size(), domains.get(di).size() - 1, di);
//                }

                changed = true;
                pruned.add(new BinaryTuple(Di_index, di));
            }
        }

        if (domains.get(Di_index).isEmpty()) {
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
            return domainSizes.firstEntry().getValue().iterator().next();
        }
    }

    protected int selectVal(LinkedHashSet<Integer> domain) {
        return domain.iterator().next();
    }

    protected void assign(int var, int val, Stack<BinaryTuple> pruned) {
        assignments.put(var, val);
        LinkedHashSet<Integer> domain = (LinkedHashSet<Integer>) domains.get(var).clone();

        int inititalSize = domain.size();

        for (int d : domain) {
            if (d != val) {
                domains.get(var).remove(d);
                pruned.push(new BinaryTuple(var, d));
            }
        }

        //If using smallest domain first heuristic
        if (heuristic == Heuristic.SMALLEST_DOMAIN_FIRST) {
            //Remove variable from associated domain size
            domainSizes.get(inititalSize).remove(var);

            //If no other variables associated with domain size
            if (domainSizes.get(inititalSize).isEmpty()) {
                //Remove size from domain sizes
                domainSizes.remove(inititalSize);
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
        domainSizes.get(newSize).add(var);
    }
}
