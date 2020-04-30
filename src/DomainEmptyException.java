/**
 * Custom exception which is thrown when the domain of a variable is empty
 */
public class DomainEmptyException extends Exception {
    public DomainEmptyException(String message) {
        super(message);
    }
}
