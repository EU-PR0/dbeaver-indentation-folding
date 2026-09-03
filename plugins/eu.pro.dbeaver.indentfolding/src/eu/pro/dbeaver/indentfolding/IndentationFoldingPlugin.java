package eu.pro.dbeaver.indentfolding;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public final class IndentationFoldingPlugin extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "eu.pro.dbeaver.indentfolding";

    public static final String PREF_ENABLED = "folding.enabled";
    public static final String PREF_TAB_WIDTH = "folding.tabWidth";
    public static final String PREF_DISABLE_BUILTIN = "folding.disableBuiltin";

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

        BuiltInFoldingController.recoverStaleOverride();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        try {
            BuiltInFoldingController.forceRestore();
            IndentationFoldingAddIn.disposeAll();
        } finally {
            instance = null;
            super.stop(context);
        }
    }
}
