package mari.samba.fstec;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Evaluates FSTEC compliance rule conditions against configuration values.
 */
@Component
public class FstecRuleEvaluator {

    /**
     * Evaluates a value against an expected value using the specified operator.
     *
     * @param operator rule operator name
     * @param current current value from configuration
     * @param expected expected value definition
     * @return true if the condition is satisfied (OK), false otherwise (FAILED)
     */
    public boolean evaluate(String operator, String current, String expected) {
        if (operator == null) {
            return false;
        }

        return switch (operator.toUpperCase(Locale.ROOT)) {
            case "EQUALS" -> evaluateEquals(current, expected);
            case "NOT_EQUALS" -> evaluateNotEquals(current, expected);
            case "ONE_OF" -> evaluateOneOf(current, expected);
            case "CONTAINS_ALL" -> evaluateContainsAll(current, expected);
            case "NOT_PRESENT_OR_NOT_EQUALS" -> evaluateNotPresentOrNotEquals(current, expected);
            case "EQUALS_OR_STRICTER_MASK" -> isEqualsOrStricterMask(current, expected);
            case "AT_LEAST_PROTOCOL" -> evaluateAtLeastProtocol(current, expected);
            default -> false;
        };
    }

    public boolean evaluateEquals(String current, String expected) {
        if (current == null || expected == null) {
            return false;
        }
        return current.trim().equalsIgnoreCase(expected.trim());
    }

    public boolean evaluateNotEquals(String current, String expected) {
        if (expected == null || expected.isBlank()) {
            // "Not equals empty" means value must be present and not empty
            return current != null && !current.trim().isEmpty();
        }
        if (current == null) {
            return true;
        }
        return !current.trim().equalsIgnoreCase(expected.trim());
    }

    public boolean evaluateOneOf(String current, String expected) {
        if (current == null || expected == null) {
            return false;
        }
        String trimmedCurrent = current.trim();
        return Arrays.stream(expected.split("[,;]")).map(String::trim).anyMatch(trimmedCurrent::equalsIgnoreCase);
    }

    public boolean evaluateContainsAll(String current, String expected) {
        if (current == null || expected == null) {
            return false;
        }
        Set<String> currentTokens = tokens(current);
        Set<String> expectedTokens = tokens(expected);

        return currentTokens.containsAll(expectedTokens);
    }

    private Set<String> tokens(String value) {
        return Arrays.stream(value.split("[,\\s]+"))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean evaluateNotPresentOrNotEquals(String current, String expected) {
        if (current == null || current.isBlank()) {
            return true;
        }
        if (expected == null) {
            return false;
        }
        return !current.trim().equalsIgnoreCase(expected.trim());
    }

    public boolean isEqualsOrStricterMask(String currentMaskStr, String expectedMaskStr) {
        if (currentMaskStr == null
                || currentMaskStr.isBlank()
                || expectedMaskStr == null
                || expectedMaskStr.isBlank()) {
            return false;
        }
        try {
            int currentMask = Integer.parseInt(currentMaskStr.trim(), 8);
            int expectedMask = Integer.parseInt(expectedMaskStr.trim(), 8);
            return (currentMask & ~expectedMask) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean evaluateAtLeastProtocol(String currentProtocol, String expectedProtocol) {
        int currentRank = getProtocolRank(currentProtocol);
        int expectedRank = getProtocolRank(expectedProtocol);
        if (currentRank < 0 || expectedRank < 0) {
            return false;
        }
        return currentRank >= expectedRank;
    }

    public int getProtocolRank(String protocol) {
        if (protocol == null) {
            return -1;
        }
        String p = protocol.trim().toUpperCase(Locale.ROOT);
        return switch (p) {
            case "CORE" -> 1;
            case "COREPLUS" -> 2;
            case "LANMAN1" -> 3;
            case "LANMAN2" -> 4;
            case "NT1" -> 5;
            case "SMB2_02", "SMB2_10", "SMB2_22", "SMB2_24", "SMB2" -> 6;
            case "SMB3_00", "SMB3_02", "SMB3_11", "SMB3" -> 7;
            default -> -1;
        };
    }
}
