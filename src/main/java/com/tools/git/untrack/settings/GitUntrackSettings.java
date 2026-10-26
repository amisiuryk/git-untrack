package com.tools.git.untrack.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.APP)
@State(name = "GitUntrackSettings", storages = @Storage("gitUntrack.xml"))
public final class GitUntrackSettings implements PersistentStateComponent<GitUntrackSettings.Data> {

    public static final class Data {
        public boolean confirmBeforeUntracking = true;
        public boolean includeMissingFiles = false;
    }

    private Data data = new Data();

    public static GitUntrackSettings getInstance() {
        return ApplicationManager.getApplication().getService(GitUntrackSettings.class);
    }

    @Override
    public @NotNull Data getState() {
        return data;
    }

    @Override
    public void loadState(@NotNull Data state) {
        data = state;
    }

    public boolean isConfirmBeforeUntracking() {
        return data.confirmBeforeUntracking;
    }

    public void setConfirmBeforeUntracking(boolean value) {
        data.confirmBeforeUntracking = value;
    }

    public boolean isIncludeMissingFiles() {
        return data.includeMissingFiles;
    }

    public void setIncludeMissingFiles(boolean value) {
        data.includeMissingFiles = value;
    }
}
