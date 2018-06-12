package com.dragon.gerrit.jenson.data.converter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.dragon.gerrit.jenson.data.entity.ScrIssue;
import com.dragon.gerrit.jenson.util.Localization;

public class CustomIssueFormatter implements IssueFormatter, TagFormatter<CustomIssueFormatter.Tag> {

    public static final String DEFAULT_ISSUE_COMMENT_TEXT = Localization.getLocalized("jenkins.plugin.default.review.body");

    private ScrIssue issue;
    private String text;
    private String host;

    public CustomIssueFormatter(ScrIssue issue, String text, String host) {
        this.issue = issue;
        this.text = prepareText(text, DEFAULT_ISSUE_COMMENT_TEXT);
        this.host = host;
    }

    private static String prepareText(String text, String defaultValue) {
        return text != null && !text.trim().isEmpty() ? text.trim() : defaultValue;
    }

    @Override
    public String getMessage() {
        String res = text;
        for (Tag tag : Tag.values()) {
            if (res.contains(tag.getName())) {
                res = res.replace(tag.getName(), getValueToReplace(tag));
            }
        }
        return res;
    }

    @Override
    public String getValueToReplace(Tag tag) {
        switch (tag) {
            case MESSAGE:
                return issue.getMessage();
            case SEVERITY:
                return issue.getSeverity();
            case RULE_URL:
                return getRuleLink(issue.getId());
            default:
                return null;
        }
    }

    protected String getRuleLink(String rule) {
        if (host != null) {
            StringBuilder sb = new StringBuilder();
            String url = host.trim();
            if (!(url.startsWith("http://") || host.startsWith("https://"))) {
                sb.append("http://");
            }
            sb.append(url);
            if (!(url.endsWith("/"))) {
                sb.append("/");
            }
            sb.append("coding_rules#q=");
            sb.append(escapeHttp(rule));
            return sb.toString();
        }
        return rule;
    }


    protected String escapeHttp(String query) {
        try {
            return URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return query;
        }
    }

    public enum Tag {
        MESSAGE("<message>"),
        SEVERITY("<severity>"),
        RULE_URL("<rule_url>");

        private final String name;

        Tag(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
