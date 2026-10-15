package mari.samba.fstec;

import java.time.Instant;
import java.util.List;

/**
 * Aggregate report summarizing FSTEC compliance check results.
 *
 * @param checkedAt timestamp when the audit was performed
 * @param compliancePercent percentage of compliance: OK / (OK + FAILED) * 100
 * @param okCount count of passing rules
 * @param failedCount count of failing rules
 * @param manualCount count of rules requiring manual verification
 * @param notApplicableCount count of rules not applicable to this server role
 * @param rules list of detailed rule results
 */
public record FstecReportDto(
        Instant checkedAt,
        int compliancePercent,
        int okCount,
        int failedCount,
        int manualCount,
        int notApplicableCount,
        List<FstecRuleResult> rules) {

    public FstecReportDto {
        if (rules == null) {
            rules = List.of();
        }
    }
}
