package com.example.indexreviewer.model.trace;

/**
 * Selection outcome of one security. NOT_ELIGIBLE means the security could not be ranked; its findings explain why.
 */
public enum SelectionDecision {
    SELECTED_DIRECT,
    SELECTED_BUFFER_EXISTING_COMPONENT,
    SELECTED_BUFFER_NEWCOMER,
    NOT_SELECTED_BUFFER,
    NOT_SELECTED_BELOW_BUFFER,
    NOT_ELIGIBLE;

    public boolean isSelected() {
        return this == SELECTED_DIRECT || this == SELECTED_BUFFER_EXISTING_COMPONENT
            || this == SELECTED_BUFFER_NEWCOMER;
    }
}
