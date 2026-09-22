package eu.pro.dbeaver.indentfolding;

import org.eclipse.ui.IStartup;

/**
 * Starts Source-viewer discovery even if the user opens a procedure/function
 * Source page before opening a standalone SQL editor.
 */
public final class IndentationFoldingStartup implements IStartup {
    @Override
    public void earlyStartup() {
        SourceViewerFoldingManager.start();
    }
}
