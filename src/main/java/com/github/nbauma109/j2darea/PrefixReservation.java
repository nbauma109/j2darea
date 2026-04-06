package com.github.nbauma109.j2darea;

/**
 * Reserved Infinity Engine filename prefix entry loaded from a resource file.
 */
public class PrefixReservation {

    private final String scope;
    private final String prefix;
    private final String project;
    private final String status;
    private final String comments;

    public PrefixReservation(String scope, String prefix, String project, String status, String comments) {
        this.scope = scope;
        this.prefix = prefix;
        this.project = project;
        this.status = status;
        this.comments = comments;
    }

    public String getScope() {
        return scope;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getProject() {
        return project;
    }

    public String getStatus() {
        return status;
    }

    public String getComments() {
        return comments;
    }

    public String getDisplayText() {
        StringBuilder out = new StringBuilder();
        out.append(prefix).append(" - ").append(project).append(" [").append(status).append(']');
        if (comments != null && !comments.trim().isEmpty()) {
            out.append(" - ").append(comments.trim());
        }
        return out.toString();
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}
