package com.tools.git.untrack.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

public final class GitUntrackConfigurable implements Configurable {

    private JBCheckBox confirm;
    private JBCheckBox includeMissing;

    @Override
    public @Nls String getDisplayName() {
        return "Git Untrack";
    }

    @Override
    public @Nullable JComponent createComponent() {
        confirm = new JBCheckBox("Confirm before untracking");
        includeMissing = new JBCheckBox("Include files missing from disk");
        includeMissing.setToolTipText("Untracking a tracked file that no longer exists on disk stages its deletion.");
        JPanel panel = FormBuilder.createFormBuilder()
                .addComponent(confirm)
                .addComponent(includeMissing)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        GitUntrackSettings settings = GitUntrackSettings.getInstance();
        return confirm.isSelected() != settings.isConfirmBeforeUntracking()
                || includeMissing.isSelected() != settings.isIncludeMissingFiles();
    }

    @Override
    public void apply() {
        GitUntrackSettings settings = GitUntrackSettings.getInstance();
        settings.setConfirmBeforeUntracking(confirm.isSelected());
        settings.setIncludeMissingFiles(includeMissing.isSelected());
    }

    @Override
    public void reset() {
        GitUntrackSettings settings = GitUntrackSettings.getInstance();
        confirm.setSelected(settings.isConfirmBeforeUntracking());
        includeMissing.setSelected(settings.isIncludeMissingFiles());
    }

    @Override
    public void disposeUIResources() {
        confirm = null;
        includeMissing = null;
    }
}
