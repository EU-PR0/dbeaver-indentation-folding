package eu.pro.dbeaver.indentfolding;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

/**
 * Temporarily disables DBeaver's SQL-structure folding while indentation folding is active.
 * The original value is restored when the last editor releases the override.
 */
final class BuiltInFoldingController {
    private static final String DBEAVER_FOLDING_ENABLED = "SQLEditor.Folding.enabled";
    private static final String PREF_OVERRIDE_ACTIVE = "builtinOverride.active";
    private static final String PREF_ORIGINAL_VALUE = "builtinOverride.originalValue";

    private static int holders;
    private static Boolean originalValue;

    private BuiltInFoldingController() {
    }

    static synchronized void recoverStaleOverride() {
        IndentationFoldingPlugin plugin = IndentationFoldingPlugin.getDefault();
        if (plugin == null) {
            return;
        }
        IPreferenceStore own = plugin.getPreferenceStore();
        if (own.getBoolean(PREF_OVERRIDE_ACTIVE)) {
            boolean original = own.getBoolean(PREF_ORIGINAL_VALUE);
            dbeaverStore().setValue(DBEAVER_FOLDING_ENABLED, original);
            own.setValue(PREF_OVERRIDE_ACTIVE, false);
        }
        holders = 0;
        originalValue = null;
    }

    static synchronized void acquire() {
        if (holders++ > 0) {
            return;
        }

        DBPPreferenceStore store = dbeaverStore();
        originalValue = store.getBoolean(DBEAVER_FOLDING_ENABLED);

        IndentationFoldingPlugin plugin = IndentationFoldingPlugin.getDefault();
        if (plugin != null) {
            IPreferenceStore own = plugin.getPreferenceStore();
            own.setValue(PREF_ORIGINAL_VALUE, originalValue.booleanValue());
            own.setValue(PREF_OVERRIDE_ACTIVE, true);
        }

        if (originalValue.booleanValue()) {
            store.setValue(DBEAVER_FOLDING_ENABLED, false);
        }
    }

    static synchronized void release() {
        if (holders == 0) {
            return;
        }
        holders--;
        if (holders == 0) {
            restoreInternal();
        }
    }

    static synchronized void forceRestore() {
        holders = 0;
        restoreInternal();
    }

    private static void restoreInternal() {
        if (originalValue != null) {
            dbeaverStore().setValue(DBEAVER_FOLDING_ENABLED, originalValue.booleanValue());
            originalValue = null;
        }

        IndentationFoldingPlugin plugin = IndentationFoldingPlugin.getDefault();
        if (plugin != null) {
            plugin.getPreferenceStore().setValue(PREF_OVERRIDE_ACTIVE, false);
        }
    }

    private static DBPPreferenceStore dbeaverStore() {
        return DBWorkbench.getPlatform().getPreferenceStore();
    }
}
