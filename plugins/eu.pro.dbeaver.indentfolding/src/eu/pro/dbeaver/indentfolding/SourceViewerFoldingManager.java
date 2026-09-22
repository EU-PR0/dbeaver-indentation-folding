package eu.pro.dbeaver.indentfolding;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Discovers nested SQLSourceViewer instances used by database object Source
 * pages (procedures, functions, views, triggers, and similar objects).
 *
 * DBeaver exposes the active nested SQL editor through the top-level editor's
 * adapter. Focus/selection filters cover folder switches that do not generate
 * a top-level workbench part activation event.
 */
final class SourceViewerFoldingManager {
    private static final Map<SQLSourceViewer<?>, EditorFoldingSession> SESSIONS = new IdentityHashMap<>();
    private static final Map<IWorkbenchPage, IPartListener2> PAGE_LISTENERS = new IdentityHashMap<>();

    private static IWindowListener windowListener;
    private static Listener uiActivityListener;
    private static boolean started;
    private static boolean detectionScheduled;

    private SourceViewerFoldingManager() {
    }

    static void start() {
        runOnUiThread(SourceViewerFoldingManager::startOnUiThread);
    }

    static void refreshAll() {
        runOnUiThread(() -> {
            for (EditorFoldingSession session : new ArrayList<>(SESSIONS.values())) {
                session.refreshPreferences();
            }
            scheduleDetection();
        });
    }

    static void stop() {
        runOnUiThreadSync(SourceViewerFoldingManager::stopOnUiThread);
    }

    private static void startOnUiThread() {
        if (started || !PlatformUI.isWorkbenchRunning()) {
            return;
        }
        started = true;

        IWorkbench workbench = PlatformUI.getWorkbench();

        windowListener = new IWindowListener() {
            @Override
            public void windowOpened(IWorkbenchWindow window) {
                attachWindow(window);
                scheduleDetection();
            }

            @Override
            public void windowClosed(IWorkbenchWindow window) {
                detachWindow(window);
            }

            @Override
            public void windowActivated(IWorkbenchWindow window) {
                attachWindow(window);
                scheduleDetection();
            }

            @Override
            public void windowDeactivated(IWorkbenchWindow window) {
                // Nothing to do.
            }
        };
        workbench.addWindowListener(windowListener);

        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            attachWindow(window);
        }

        Display display = Display.getDefault();
        uiActivityListener = event -> scheduleDetection();
        display.addFilter(SWT.FocusIn, uiActivityListener);
        display.addFilter(SWT.Selection, uiActivityListener);

        scheduleDetection();
    }

    private static void stopOnUiThread() {
        if (!started) {
            return;
        }
        started = false;
        detectionScheduled = false;

        if (PlatformUI.isWorkbenchRunning()) {
            IWorkbench workbench = PlatformUI.getWorkbench();
            if (windowListener != null) {
                workbench.removeWindowListener(windowListener);
            }
        }
        windowListener = null;

        Display display = Display.getDefault();
        if (!display.isDisposed() && uiActivityListener != null) {
            display.removeFilter(SWT.FocusIn, uiActivityListener);
            display.removeFilter(SWT.Selection, uiActivityListener);
        }
        uiActivityListener = null;

        for (Map.Entry<IWorkbenchPage, IPartListener2> entry : new ArrayList<>(PAGE_LISTENERS.entrySet())) {
            entry.getKey().removePartListener(entry.getValue());
        }
        PAGE_LISTENERS.clear();

        for (EditorFoldingSession session : new ArrayList<>(SESSIONS.values())) {
            session.dispose();
        }
        SESSIONS.clear();
    }

    private static void attachWindow(IWorkbenchWindow window) {
        for (IWorkbenchPage page : window.getPages()) {
            attachPage(page);
        }
    }

    private static void detachWindow(IWorkbenchWindow window) {
        for (IWorkbenchPage page : window.getPages()) {
            IPartListener2 listener = PAGE_LISTENERS.remove(page);
            if (listener != null) {
                page.removePartListener(listener);
            }
        }
    }

    private static void attachPage(IWorkbenchPage page) {
        if (PAGE_LISTENERS.containsKey(page)) {
            return;
        }

        IPartListener2 listener = new IPartListener2() {
            @Override
            public void partActivated(IWorkbenchPartReference partRef) {
                scheduleDetection();
            }

            @Override
            public void partBroughtToTop(IWorkbenchPartReference partRef) {
                scheduleDetection();
            }

            @Override
            public void partOpened(IWorkbenchPartReference partRef) {
                scheduleDetection();
            }

            @Override
            public void partVisible(IWorkbenchPartReference partRef) {
                scheduleDetection();
            }

            @Override
            public void partInputChanged(IWorkbenchPartReference partRef) {
                scheduleDetection();
            }

            @Override
            public void partClosed(IWorkbenchPartReference partRef) {
                // Nested source sessions dispose themselves with their StyledText control.
            }

            @Override
            public void partDeactivated(IWorkbenchPartReference partRef) {
                // Nothing to do.
            }

            @Override
            public void partHidden(IWorkbenchPartReference partRef) {
                // Nothing to do.
            }
        };

        PAGE_LISTENERS.put(page, listener);
        page.addPartListener(listener);
    }

    private static void scheduleDetection() {
        if (!started || detectionScheduled) {
            return;
        }

        Display display = Display.getDefault();
        if (display.isDisposed()) {
            return;
        }

        detectionScheduled = true;
        display.asyncExec(() -> {
            detectionScheduled = false;
            detectActiveSourceViewers();
        });
    }

    private static void detectActiveSourceViewers() {
        if (!started || !PlatformUI.isWorkbenchRunning()) {
            return;
        }

        IPreferenceStore prefs = preferences();
        if (!prefs.getBoolean(IndentationFoldingPlugin.PREF_SOURCE_VIEWERS_ENABLED)) {
            return;
        }

        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            IWorkbenchPage page = window.getActivePage();
            if (page == null) {
                continue;
            }

            IEditorPart activeEditor = page.getActiveEditor();
            if (activeEditor == null) {
                continue;
            }

            if (activeEditor instanceof SQLSourceViewer<?> sourceViewer) {
                ensureSession(sourceViewer);
                continue;
            }

            SQLEditorBase adapted = activeEditor.getAdapter(SQLEditorBase.class);
            if (adapted instanceof SQLSourceViewer<?> sourceViewer) {
                ensureSession(sourceViewer);
            }
        }
    }

    private static void ensureSession(SQLSourceViewer<?> sourceViewer) {
        if (SESSIONS.containsKey(sourceViewer)) {
            return;
        }

        EditorFoldingSession[] holder = new EditorFoldingSession[1];
        holder[0] = new EditorFoldingSession(
            sourceViewer,
            true,
            () -> {
                EditorFoldingSession current = SESSIONS.get(sourceViewer);
                if (current == holder[0]) {
                    SESSIONS.remove(sourceViewer);
                }
            }
        );

        SESSIONS.put(sourceViewer, holder[0]);
        holder[0].start();
    }

    private static IPreferenceStore preferences() {
        IndentationFoldingPlugin plugin = IndentationFoldingPlugin.getDefault();
        if (plugin == null) {
            throw new IllegalStateException("Indentation folding plug-in is not active");
        }
        return plugin.getPreferenceStore();
    }

    private static void runOnUiThread(Runnable runnable) {
        Display display = Display.getDefault();
        if (display.isDisposed()) {
            return;
        }
        if (display.getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            display.asyncExec(runnable);
        }
    }

    private static void runOnUiThreadSync(Runnable runnable) {
        Display display = Display.getDefault();
        if (display.isDisposed()) {
            return;
        }
        if (display.getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            display.syncExec(runnable);
        }
    }
}
