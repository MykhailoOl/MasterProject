package com.example.masterproject.web;

import com.example.masterproject.model.enums.RequirementCategory;
import com.example.masterproject.model.taxonomy.TaxonomyCatalog;
import org.springframework.stereotype.Component;

@Component("taxonomyLabels")
public class TaxonomyLabels {

    public String resolution(String value) {
        return switch (value) {
            case "CAPTURED" -> "Please check";
            case "OPEN" -> "Decision needed";
            case "DEFERRED" -> "Decide later";
            case "CONFLICT" -> "Needs clarification";
            case "NOT_APPLICABLE" -> "Does not apply";
            default -> value;
        };
    }

    public String name(RequirementCategory category) {
        if (category == null) {
            return "";
        }
        return TaxonomyCatalog.require(category).displayName();
    }

    public String description(RequirementCategory category) {
        if (category == null) {
            return "";
        }
        return TaxonomyCatalog.require(category).description();
    }

    public String status(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        return switch (status.toUpperCase()) {
            case "DRAFT" -> "Draft";
            case "IN_PROGRESS" -> "In progress";
            case "REVIEW" -> "Ready to review";
            case "COMPLETED" -> "Completed";
            case "ARCHIVED" -> "Archived";
            default -> status;
        };
    }
}
