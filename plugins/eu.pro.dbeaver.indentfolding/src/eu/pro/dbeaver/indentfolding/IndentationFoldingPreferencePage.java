package eu.pro.dbeaver.indentfolding;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public final class IndentationFoldingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    public IndentationFoldingPreferencePage() {
        super(GRID);
        setPreferenceStore(IndentationFoldingPlugin.getDefault().getPreferenceStore());
        setDescription(
            "Create SQL folding regions from indentation levels. " +
            "Blank lines are ignored when detecting block boundaries."
        );
    }

    @Override
    public void init(IWorkbench workbench) {
        // No workbench state required.
    }

    @Override
    protected void createFieldEditors() {
        addField(new BooleanFieldEditor(
            IndentationFoldingPlugin.PREF_ENABLED,
            "Enable indentation-based folding",
            getFieldEditorParent()
        ));

        IntegerFieldEditor tabWidth = new IntegerFieldEditor(
            IndentationFoldingPlugin.PREF_TAB_WIDTH,
            "Tab width for indentation calculation:",
            getFieldEditorParent()
        );
        tabWidth.setValidRange(1, 16);
        addField(tabWidth);

        addField(new BooleanFieldEditor(
            IndentationFoldingPlugin.PREF_DISABLE_BUILTIN,
            "Temporarily disable DBeaver SQL-structure folding while indentation folding is active",
            getFieldEditorParent()
        ));
    }

    @Override
    public boolean performOk() {
        boolean ok = super.performOk();
        if (ok) {
            IndentationFoldingAddIn.refreshAll();
        }
        return ok;
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
    }
}
