package ru.runa.gpd.util.files;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import ru.runa.gpd.util.WizardPageUtils;

public class FileExporter {
    private final ZipOutputStream outputStream;

    public FileExporter(OutputStream outputStream) {
        this.outputStream = new ZipOutputStream(outputStream, Charsets.UTF_8);
    }

    public void finished() throws IOException {
        outputStream.close();
    }

    public void write(IFile resource, String destinationPath) throws IOException, CoreException {
        WizardPageUtils.write(outputStream, new ZipEntry(destinationPath), resource);
    }
}
