package eu.pro.dbeaver.indentfolding;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public final class IndentationFoldingPlugin extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "eu.pro.dbeaver.indentfolding";

    /**
     * Historical key retained for compatibility. It controls indentation-based
     * folding only; explicit region folding has its own switch.
     */
    public static final String PREF_ENABLED = "folding.enabled";
    public static final String PREF_TAB_WIDTH = "folding.tabWidth";
    public static final String PREF_DISABLE_BUILTIN = "folding.disableBuiltin";
    public static final String PREF_REGIONS_ENABLED = "folding.regions.enabled";
    public static final String PREF_SOURCE_VIEWERS_ENABLED = "folding.sourceViewers.enabled";

    private static IndentationFoldingPlugin instance;

    public static IndentationFoldingPlugin getDefault() {
        return instance;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;

        IPreferenceStore store = getPreferenceStore();
        store.setDefault(PREF_ENABLED, true);
        store.setDefault(PREF_TAB_WIDTH, 4);
        store.setDefault(PREF_DISABLE_BUILTIN, true);
        store.setDefault(PREF_REGIONS_ENABLED, true);
        store.setDefault(PREF_SOURCE_VIEWERS_ENABLED, true);

        BuiltInFoldingController.recoverStaleOverride();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        try {
            SourceViewerFoldingManager.stop();
            IndentationFoldingAddIn.disposeAll();
            BuiltInFoldingController.forceRestore();
        } finally {
            instance = null;
            super.stop(context);
        }
    }
}
