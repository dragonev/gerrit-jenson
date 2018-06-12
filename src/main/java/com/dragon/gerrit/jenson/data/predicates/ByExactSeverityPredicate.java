package com.dragon.gerrit.jenson.data.predicates;

import com.dragon.gerrit.jenson.data.entity.ScrIssue;
import com.dragon.gerrit.jenson.data.entity.Severity;
import com.google.common.base.Predicate;

public class ByExactSeverityPredicate implements Predicate<ScrIssue> {
	
    private final Severity severity;

    private ByExactSeverityPredicate(Severity severity) {
        this.severity = severity;
    }

    @Override
    public boolean apply(ScrIssue issue) {
    	Severity sev = Severity.valueOf(issue.getSeverity());
        return sev.equals(severity);
    }

    public static ByExactSeverityPredicate apply(Severity severity) {
        return new ByExactSeverityPredicate(severity);
    }
}
