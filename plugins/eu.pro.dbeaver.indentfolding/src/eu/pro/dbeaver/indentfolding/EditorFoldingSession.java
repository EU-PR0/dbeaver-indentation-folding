package eu.pro.dbeaver.indentfolding;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPropertyListener;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Folding implementation shared by regular SQL editors and nested object
 * Source viewers.
 */
final class EditorFoldingSession implements IDocumentListener {
    private static final int ATTACH_RETRY_MS = 100;
    private static final int REFRESH_DEBOUNCE_MS = 160;

    private final SQLEditorBase editor;
    private final boolean sourceViewer;
    private final Runnable disposeCallback;
    private final Set<ProjectionAnnotation> annotations = new LinkedHashSet<>();
    private final IPropertyListener propertyListener = (source, propId) -> scheduleAttach(0);

    private IDocument document;
    private ProjectionAnnotationModel model;
    private boolean disposeHookInstalled;
    private boolean builtInOverrideHeld;
    private volatile boolean disposed;
    private int refreshGeneration;

    EditorFoldingSession(SQLEditorBase editor, boolean sourceViewer, Runnable disposeCallback) {
        this.editor = editor;
        this.sourceViewer = sourceViewer;
        this.disposeCallback = disposeCallback;
    }

    void start() {
        if (disposed) {
            return;
        }
        editor.addPropertyListener(propertyListener);
        scheduleAttach(0);
    }

    void refreshPreferences() {
        if (disposed) {
            return;
        }
        runOnUiThread(this::applyPreferencesAndRefresh);
    }

    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
        // Positions are maintained by the document/annotation model.
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        scheduleRefresh(REFRESH_DEBOUNCE_MS);
    }

    private void scheduleAttach(int delayMs) {
        int generation = ++refreshGeneration;
        timerExec(delayMs, () -> {
            if (disposed || generation != refreshGeneration) {
                return;
            }
            if (!attachIfReady()) {
                scheduleAttach(ATTACH_RETRY_MS);
            }
        });
    }

    private boolean attachIfReady() {
        if (disposed) {
            return true;
        }

        IDocument currentDocument = editor.getDocument();
        ProjectionAnnotationModel currentModel = editor.getProjectionAnnotationModel();
        if (currentDocument == null || currentModel == null) {
            return false;
        }

        if (document != currentDocument) {
            if (document != null) {
                document.removeDocumentListener(this);
            }
            document = currentDocument;
            document.addDocumentListener(this);
        }
        model = currentModel;

        if (!disposeHookInstalled) {
            StyledText control = editor.getEditorControl();
            if (control != null && !control.isDisposed()) {
                disposeHookInstalled = true;
                control.addDisposeListener(event -> dispose());
            }
        }

        applyPreferencesAndRefresh();
        return true;
    }

    private void applyPreferencesAndRefresh() {
        if (disposed) {
            return;
        }

        IPreferenceStore prefs = preferences();
        boolean active = isActive(prefs);
        boolean suppressBuiltIn = prefs.getBoolean(IndentationFoldingPlugin.PREF_DISABLE_BUILTIN);

        if (active && suppressBuiltIn) {
            if (!builtInOverrideHeld) {
                BuiltInFoldingController.acquire();
                builtInOverrideHeld = true;
            }
        } else if (builtInOverrideHeld) {
            BuiltInFoldingController.release();
            builtInOverrideHeld = false;
        }

        if (!active) {
            removeOwnAnnotations();
            return;
        }

        // Changing DBeaver's folding preference can clear the projection model.
        // Re-read the model after DBeaver has processed that preference event.
        scheduleRefresh(100);
    }

    private void scheduleRefresh(int delayMs) {
        int generation = ++refreshGeneration;
        timerExec(delayMs, () -> {
            if (disposed || generation != refreshGeneration) {
                return;
            }
            refreshNow();
        });
    }

    private void refreshNow() {
        if (disposed) {
            return;
        }

        IDocument currentDocument = editor.getDocument();
        ProjectionAnnotationModel currentModel = editor.getProjectionAnnotationModel();
        if (currentDocument == null || currentModel == null) {
            scheduleAttach(ATTACH_RETRY_MS);
            return;
        }

        if (document != currentDocument) {
            if (document != null) {
                document.removeDocumentListener(this);
            }
            document = currentDocument;
            document.addDocumentListener(this);
        }
        model = currentModel;

        IPreferenceStore prefs = preferences();
        if (!isActive(prefs)) {
            removeOwnAnnotations();
            return;
        }

        int tabWidth = prefs.getInt(IndentationFoldingPlugin.PREF_TAB_WIDTH);
        if (tabWidth < 1 || tabWidth > 16) {
            tabWidth = 4;
        }

        boolean indentationEnabled = prefs.getBoolean(IndentationFoldingPlugin.PREF_ENABLED);
        boolean regionsEnabled = prefs.getBoolean(IndentationFoldingPlugin.PREF_REGIONS_ENABLED);

        List<IndentationFoldParser.FoldRegion> regions = FoldingRegionCalculator.parse(
            document.get(),
            tabWidth,
            indentationEnabled,
            regionsEnabled
        );
        reconcileAnnotations(regions);
    }

    private boolean isActive(IPreferenceStore prefs) {
        if (sourceViewer && !prefs.getBoolean(IndentationFoldingPlugin.PREF_SOURCE_VIEWERS_ENABLED)) {
            return false;
        }
        return prefs.getBoolean(IndentationFoldingPlugin.PREF_ENABLED)
            || prefs.getBoolean(IndentationFoldingPlugin.PREF_REGIONS_ENABLED);
    }

    private void reconcileAnnotations(List<IndentationFoldParser.FoldRegion> regions) {
        if (model == null) {
            return;
        }

        Map<RegionKey, ProjectionAnnotation> reusable = new HashMap<>();
        for (ProjectionAnnotation annotation : annotations) {
            Position position = model.getPosition(annotation);
            if (position != null && !position.isDeleted()) {
                reusable.put(new RegionKey(position.getOffset(), position.getLength()), annotation);
            }
        }

        Set<ProjectionAnnotation> next = new LinkedHashSet<>();
        Map<Annotation, Position> additions = new LinkedHashMap<>();

        for (IndentationFoldParser.FoldRegion region : regions) {
            RegionKey key = new RegionKey(region.offset(), region.length());
            ProjectionAnnotation annotation = reusable.remove(key);
            if (annotation == null) {
                annotation = new ProjectionAnnotation();
                additions.put(annotation, new Position(region.offset(), region.length()));
            }
            next.add(annotation);
        }

        List<Annotation> deletions = new ArrayList<>();
        for (ProjectionAnnotation annotation : annotations) {
            if (!next.contains(annotation) && model.getPosition(annotation) != null) {
                deletions.add(annotation);
            }
        }

        if (!deletions.isEmpty() || !additions.isEmpty()) {
            model.modifyAnnotations(
                deletions.toArray(Annotation[]::new),
                additions,
                null
            );
        }

        annotations.clear();
        annotations.addAll(next);
    }

    private void removeOwnAnnotations() {
        if (model != null && !annotations.isEmpty()) {
            List<Annotation> deletions = new ArrayList<>();
            for (ProjectionAnnotation annotation : annotations) {
                if (model.getPosition(annotation) != null) {
                    deletions.add(annotation);
                }
            }
            if (!deletions.isEmpty()) {
                model.modifyAnnotations(deletions.toArray(Annotation[]::new), null, null);
            }
        }
        annotations.clear();
    }

    void dispose() {
        runOnUiThreadSync(this::disposeOnUiThread);
    }

    private void disposeOnUiThread() {
        if (disposed) {
            return;
        }
        disposed = true;
        ++refreshGeneration;

        editor.removePropertyListener(propertyListener);

        if (document != null) {
            document.removeDocumentListener(this);
            document = null;
        }

        removeOwnAnnotations();
        model = null;

        if (builtInOverrideHeld) {
            BuiltInFoldingController.release();
            builtInOverrideHeld = false;
        }

        if (disposeCallback != null) {
            disposeCallback.run();
        }
    }

    private static IPreferenceStore preferences() {
        IndentationFoldingPlugin plugin = IndentationFoldingPlugin.getDefault();
        if (plugin == null) {
            throw new IllegalStateException("Indentation folding plug-in is not active");
        }
        return plugin.getPreferenceStore();
    }

    private static void timerExec(int delayMs, Runnable runnable) {
        Display display = Display.getDefault();
        if (!display.isDisposed()) {
            display.timerExec(delayMs, runnable);
        }
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

    private record RegionKey(int offset, int length) {
    }
}
