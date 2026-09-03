package eu.pro.dbeaver.indentfolding;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.addins.SQLEditorAddIn;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Per-editor add-in that maintains projection annotations derived from indentation.
 */
public final class IndentationFoldingAddIn implements SQLEditorAddIn, IDocumentListener {
    private static final int ATTACH_RETRY_MS = 100;
    private static final int REFRESH_DEBOUNCE_MS = 160;
    private static final Set<IndentationFoldingAddIn> ACTIVE = new CopyOnWriteArraySet<>();

    private SQLEditor editor;
    private IDocument document;
    private ProjectionAnnotationModel model;
    private final Set<ProjectionAnnotation> annotations = new LinkedHashSet<>();

    private volatile boolean disposed;
    private int refreshGeneration;
    private boolean builtInOverrideHeld;

    @Override
    public void init(@NotNull SQLEditor editor) {
        this.editor = editor;
        ACTIVE.add(this);
        scheduleAttach(0);
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

    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
        // No action required. Positions are maintained by the annotation model/document.
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        scheduleRefresh(REFRESH_DEBOUNCE_MS);
    }

    static void refreshAll() {
        for (IndentationFoldingAddIn addIn : ACTIVE) {
            addIn.applyPreferencesAndRefresh();
        }
    }

    static void disposeAll() {
        for (IndentationFoldingAddIn addIn : new ArrayList<>(ACTIVE)) {
            addIn.dispose();
        }
    }

    private void scheduleAttach(int delayMs) {
        int generation = ++refreshGeneration;
        Display.getDefault().timerExec(delayMs, () -> {
            if (disposed || generation != refreshGeneration) {
                return;
            }
            if (!attachIfReady()) {
                scheduleAttach(ATTACH_RETRY_MS);
            }
        });
    }

    private boolean attachIfReady() {
        if (disposed || editor == null) {
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

        applyPreferencesAndRefresh();
        return true;
    }

    private void applyPreferencesAndRefresh() {
        if (disposed) {
            return;
        }
        runOnUiThread(() -> {
            if (disposed) {
                return;
            }

            IPreferenceStore prefs = preferences();
            boolean enabled = prefs.getBoolean(IndentationFoldingPlugin.PREF_ENABLED);
            boolean suppressBuiltIn = prefs.getBoolean(IndentationFoldingPlugin.PREF_DISABLE_BUILTIN);

            if (enabled && suppressBuiltIn) {
                if (!builtInOverrideHeld) {
                    BuiltInFoldingController.acquire();
                    builtInOverrideHeld = true;
                }
            } else if (builtInOverrideHeld) {
                BuiltInFoldingController.release();
                builtInOverrideHeld = false;
            }

            if (!enabled) {
                removeOwnAnnotations();
                return;
            }

            // Changing DBeaver's own folding preference may clear the projection model.
            // Re-read model/document and apply ours after DBeaver has processed its event.
            scheduleRefresh(100);
        });
    }

    private void scheduleRefresh(int delayMs) {
        int generation = ++refreshGeneration;
        Display.getDefault().timerExec(delayMs, () -> {
            if (disposed || generation != refreshGeneration) {
                return;
            }
            refreshNow();
        });
    }

    private void refreshNow() {
        if (disposed || editor == null) {
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
        if (!prefs.getBoolean(IndentationFoldingPlugin.PREF_ENABLED)) {
            removeOwnAnnotations();
            return;
        }

        int tabWidth = prefs.getInt(IndentationFoldingPlugin.PREF_TAB_WIDTH);
        if (tabWidth < 1 || tabWidth > 16) {
            tabWidth = 4;
        }

        List<IndentationFoldParser.FoldRegion> regions = IndentationFoldParser.parse(document.get(), tabWidth);
        reconcileAnnotations(regions);
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

    private void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        ++refreshGeneration;
        ACTIVE.remove(this);

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
        editor = null;
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
        if (display.getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            display.asyncExec(runnable);
        }
    }

    private record RegionKey(int offset, int length) {}
}
