package ru.runa.gpd.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import ru.runa.gpd.Localization;

public class WizardPageUtils {
    public static IResource getInitialElement(IStructuredSelection selection) {
        return selection != null && !selection.isEmpty() && selection.getFirstElement() instanceof IAdaptable
                ? (IResource) ((IAdaptable) selection.getFirstElement()).getAdapter(IResource.class)
                : null;
    }

    public static Composite createPageControl(Composite parent) {
        return new Composite(parent, SWT.NONE) {{
            setLayout(new GridLayout(1, false));
            setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        }};
    }

    public static SashForm createSashForm(Composite pageControl) {
        return new SashForm(pageControl, SWT.HORIZONTAL) {{
            setLayoutData(new GridData(GridData.FILL_BOTH));
        }};
    }

    public static ListViewer createViewer(SashForm sashForm, String label, Set<String> input, Consumer<SelectionChangedEvent> onChange) {
        Group listGroup = new Group(sashForm, SWT.NONE) {{
            setLayout(new GridLayout(1, false));
            setLayoutData(new GridData(GridData.FILL_BOTH));
            setText(Localization.getString(label));
        }};
        return new ListViewer(listGroup, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER) {{
            getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            setContentProvider(new ArrayContentProvider());
            setInput(input);
            addSelectionChangedListener(onChange::accept);
        }};
    }

    public static Group createExportGroup(SashForm sashForm) {
        return new Group(sashForm, SWT.NONE) {{
            setLayout(new GridLayout(1, false));
            setLayoutData(new GridData(GridData.FILL_BOTH));
        }};
    }

    public static void onBrowseButtonSelected(FileDialog dialog, String selectionName, Supplier<String> fileName, String currentSource,
            Runnable setErrorMessage, Consumer<String> setDestinationValue) {
        if (selectionName != null) {
            dialog.setFileName(fileName.get());
        }
        int lastSeparatorIndex = currentSource.lastIndexOf(File.separator);
        if (lastSeparatorIndex != -1) {
            dialog.setFilterPath(currentSource.substring(0, lastSeparatorIndex));
        }
        String selectedFileName = dialog.open();
        if (selectedFileName != null) {
            setErrorMessage.run();
            setDestinationValue.accept(selectedFileName);
        }
    }

    public static void write(ZipOutputStream outputStream, ZipEntry entry, IFile contents) throws IOException, CoreException {
        byte[] readBuffer = new byte[1024];
        outputStream.putNextEntry(entry);
        try (InputStream contentStream = contents.getContents()) {
            int n;
            while ((n = contentStream.read(readBuffer)) > 0) {
                outputStream.write(readBuffer, 0, n);
            }
        }
        outputStream.closeEntry();
    }

    public static void zip(List<File> files, OutputStream os) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(os);
        for (File file : files) {
            ZipEntry newEntry = new ZipEntry(file.getName());
            byte[] readBuffer = new byte[1024];
            zos.putNextEntry(newEntry);
            try (InputStream cos = Files.newInputStream(file.toPath())) {
                int n;
                while ((n = cos.read(readBuffer)) > 0) {
                    zos.write(readBuffer, 0, n);
                }
            }
            zos.closeEntry();
        }
        zos.close();
    }
}