# Git Untrack

An IntelliJ IDEA plugin that adds **Untrack…** to the Git menus: it removes the selected files from the Git index (`git rm --cached`) and keeps them on disk. The next commit deletes them from the repository. Nothing is committed automatically.

See [CONTEXT.md](CONTEXT.md) for the terms used.

## Where to find it

- **Git → Current File** and the project view's **Git** context menu, right after **Add**.
- The **Commit** tool window context menu, right after **Add to VCS**.
- The **Staging area** context menu (when staging is enabled), right after **Add**.

## Behaviour

- Folders are untracked recursively. Unversioned and ignored files in the selection are skipped.
- Selections spanning several repositories are confirmed once and reported together.
- Files whose staged content differs from both HEAD and the file on disk are refused by git. They are reported with a **Force untrack** action, which discards those staged changes.
- The success notification has an **Undo** link (`git add -f` of the untracked files), valid while every file is still unversioned.
- Settings → Version Control → Git Untrack: *Confirm before untracking*, *Include files missing from disk*.

## Build

Requires JDK 21.

```sh
./gradlew test buildPlugin   # plugin ZIP in build/distributions/
./gradlew verifyPlugin       # Plugin Verifier on IDEA 2025.1, 2025.2, 2026.1, 2026.2
./gradlew runIde             # sandbox IDE with the plugin installed
```

Install the ZIP with Settings → Plugins → ⚙ → Install Plugin from Disk….

## License

[Apache License 2.0](LICENSE)
