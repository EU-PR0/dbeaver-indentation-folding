package eu.pro.dbeaver.indentfolding;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.addins.SQLEditorAddIn;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Installs the folding session into regular SQL editors through DBeaver's
 * SQL Editor Add-in extension point.
 */
public final class IndentationFoldingAddIn implements SQLEditorAddIn {
    private static final Set<IndentationFoldingAddIn> ACTIVE = new CopyOnWriteArraySet<>();

    private EditorFoldingSession session;

    @Override
    public void init(@NotNull SQLEditor editor) {
        ACTIVE.add(this);
        session = new EditorFoldingSession(editor, false, null);
        session.start();

        // Backup initialization path for installations where early-startup
        // extensions are delayed until after the first editor is opened.
        SourceViewerFoldingManager.start();
    }

    @Override
    public void cleanup(@NotNull SQLEditor editor) {
        dispose();
    }

    @Override
    @Nullable
    public PrintWriter getServerOutputConsumer() {
        return null;
    }

    static void refreshAll() {
        for (IndentationFoldingAddIn addIn : ACTIVE) {
            if (addIn.session != null) {
                addIn.session.refreshPreferences();
            }
        }
    }

    static void disposeAll() {
        for (IndentationFoldingAddIn addIn : new ArrayList<>(ACTIVE)) {
            addIn.dispose();
        }
    }

    private void dispose() {
        ACTIVE.remove(this);
        if (session != null) {
            session.dispose();
            session = null;
        }
    }
}
