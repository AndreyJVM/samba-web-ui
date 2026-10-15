package mari.samba.fstec;

import java.util.List;

/**
 * Result of evaluating a specific FSTEC compliance rule.
 *
 * @param id unique identifier of the rule (e.g. "2.1-guest-ok")
 * @param section section / clause number in FSTEC recommendations (e.g. "2.1")
 * @param title human-readable rule summary
 * @param severity rule severity: HIGH, MEDIUM, LOW
 * @param status compliance status: OK, FAILED, NOT_APPLICABLE, MANUAL
 * @param currentValue actual value detected on server
 * @param expectedValue expected value recommended by FSTEC
 * @param howToFix remediation instructions / smb.conf directives
 * @param consequences warning about potential side-effects or impacts
 * @param shareResults per-share results if this rule evaluates shares
 */
public record FstecRuleResult(
        String id,
        String section,
        String title,
        String severity,
        FstecStatus status,
        String currentValue,
        String expectedValue,
        String howToFix,
        String consequences,
        List<ShareResult> shareResults) {

    public FstecRuleResult {
        if (shareResults == null) {
            shareResults = List.of();
        }
    }

    public FstecRuleResult(
            String id,
            String section,
            String title,
            String severity,
            FstecStatus status,
            String currentValue,
            String expectedValue,
            String howToFix) {
        this(id, section, title, severity, status, currentValue, expectedValue, howToFix, null, List.of());
    }
}
