package mari.samba.fstec;

/**
 * Immutable definition of an FSTEC compliance rule loaded from configuration.
 */
public record FstecRuleDefinition(
        String id,
        String section,
        String title,
        String severity,
        String scope,
        String key,
        String operator,
        String expected,
        String appliesTo,
        String howToFix,
        String consequences) {}
