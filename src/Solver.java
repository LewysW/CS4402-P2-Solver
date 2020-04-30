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

    //Marks if solution to CSP has been found
    protected boolean solved = false;

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

        //Get domain of xi
        LinkedHashSet<Integer> Di = (LinkedHashSet<Integer>) domains.get(constraint.getSecondVar()).clone();
        //Get domain of xj
        LinkedHashSet<Integer> Dj = domains.get(constraint.getFirstVar());

        //For each value di in the domain Di of xi
        for (Integer di : Di) {
            boolean supported = false;

            //For each value in the domain Dj of xj
            for (Integer dj : Dj) {
                //if xi = di and xj = dj satisfies the constraint
                if (satisfies(di, dj, constraint)) {
                    //Supported is set to true
                    supported = true;
                }
            }
            //If no pair of values xi = di and xj = dj satisfy the constraint
            if (!supported) {
                //Remove (prune) di from Di the domain of xi
                remove(di, Di_index);
                //Mark variable domain as changed
                changed = true;
                //Store pruned value for later in case it needs to be restored
                pruned.add(new BinaryTuple(Di_index, di));
            }
        }

        //If the domain Di is empty
        if (domains.get(Di_index).isEmpty()) {
            //Fail and exit early
            throw new DomainEmptyException("Domain of variable is empty!\n");
        }

        //Return whether change to domain Di was made
        return changed;
    }


    /**
     * Returns whether assignments to variables xi and xj satisfy constraint c
     * @param xi - first variable in arc
     * @param xj - second variable in arc
     * @param c - constraint
     * @return whether (xi, xj) satisfies c
     */
    protected boolean satisfies(int xi, int xj, BinaryConstraint c) {
        //For each pair of valid values in the constraint
        for (BinaryTuple tuple : c.getTuples()) {
            //If one of them matches the values of the variables
            if (tuple.matches(xi, xj)) {
                //Then the constraint is satisfied
                return true;
            }
        }

        //If none of the tuples match the variable
        // values then the constraint is not satisfied
        return false;
    }

    /**
     * Removes a value from the domain of a variable
     * @param val - value to remove
     * @param var - variable to reduce the domain of
     */
    protected void remove(int val, int var) {
        //If smallest-domain first is being used
        if (heuristic == Heuristic.SMALLEST_DOMAIN_FIRST) {
            //Change which domain size this variable is associated with
            updateDomainSizeMapping(domains.get(var).size(), domains.get(var).size() - 1, var);
        }

        //Remove value from domain of variable
        domains.get(var).remove(val);
    }

    /**
     * Restores a value to the domain of a variable
     * @param val - value to restore
     * @param var - variable to have value added to its domain
     */
    protected void restore(int val, int var) {
        //If smallest-domain first is being used
        if (heuristic == Heuristic.SMALLEST_DOMAIN_FIRST) {
            //Change which domain size this variable is associated with
            updateDomainSizeMapping(domains.get(var).size(), domains.get(var).size() + 1, var);
        }

        //Add value to domain of variable
        domains.get(var).add(val);
    }

    /**
     * Selects a variable depending on the variable ordering heuristic in use
     * @return next variable to assign
     */
    protected int selectVar() {
        //If heuristic is ascending
        if (heuristic == Heuristic.ASCENDING) {
            //Get value of next variable which has not be assigned a value
            return assignments.size();
        } else {
            //Otherwise get first entry in smallest domain
            return domainSizes.firstEntry().getValue().iterator().next();
        }
    }

    /**
     * Select value from domain of chosen variable
     * @param domain - to select value from
     * @return first value in domain
     */
    protected int selectVal(LinkedHashSet<Integer> domain) {
        return domain.iterator().next();
    }

    /**
     * Assign selected value to selected variable
     * @param var - chosen variable
     * @param val - chosen value
     * @param pruned - stack of pruned domain values
     */
    protected void assign(int var, int val, Stack<BinaryTuple> pruned) {
        assignments.put(var, val);
        LinkedHashSet<Integer> domain = (LinkedHashSet<Integer>) domains.get(var).clone();

        //Get size of domain of var
        int inititalSize = domain.size();

        //For each value di in the domain of var
        for (int di : domain) {
            //If value di is not the selected value val
            if (di != val) {
                //Prune the value di from the domain
                domains.get(var).remove(di);
                //Store the value on the stack in case it needs to be restored
                pruned.push(new BinaryTuple(var, di));
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

    /**
     * Remove from assignments
     * @param var - variable to unassign
     */
    protected void unassign(int var) {
        assignments.remove(var);
    }

    /**
     * Checks if solution has been found
     * @return whether every variable has an assignment
     */
    protected boolean completeAssignment() {
        return (assignments.size() == binaryCSP.getNoVariables());
    }

    /**
     * Prints the solution
     */
    public void printSolution() {
        System.out.println(this.toString());
    }

    /**
     * Converts the problem solution to a string
     * @return string solution
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(" Solver CSP Solution running with the ");
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

    /**
     * Restores pruned values using stack
     * @param pruned - pruned tree values to be restored
     */
    protected void undoPruning(Stack<BinaryTuple> pruned) {
        //Loops until all pruned values have been restored
        while (!pruned.empty()) {
            //Get value and variable from stack
            int val = pruned.peek().getVal2();
            int var = pruned.pop().getVal1();

            //Restore value to domain of variable
            restore(val, var);
        }
    }

    /**
     * Updates mapping from size of domain to
     * variables with that domain size
     * @param currentSize - current size of domain of variable
     * @param newSize - new size of domain of variable
     * @param var - variable to update domain size of
     */
    protected void updateDomainSizeMapping(int currentSize, int newSize, int var) {
        //If there exists a set of variables with that domain size, and var is one of them
        if (domainSizes.containsKey(currentSize) && domainSizes.get(currentSize).contains(var)) {
            //Remove var from set with that domain size
            domainSizes.get(currentSize).remove(var);

            //If there are no longer any variables with that size domain,
            if (domainSizes.get(currentSize).isEmpty()) {
                //then the delete the set with that domain size
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
