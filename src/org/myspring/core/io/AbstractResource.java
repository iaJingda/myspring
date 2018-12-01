package org.myspring.core.io;

import org.myspring.core.NestedIOException;
import org.myspring.core.util.Assert;
import org.myspring.core.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public  abstract class AbstractResource  implements Resource{

    @Override
    public boolean exists() {
        // Try file existence: can we find the file in the file system?
        try {
            return getFile().exists();
        }
        catch (IOException ex) {
            // Fall back to stream existence: can we open the stream?
            try {
                getInputStream().close();
                return true;
            }
            catch (Throwable isEx) {
                return false;
            }
        }
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public URL getURL() throws IOException {
        throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
    }

    @Override
    public URI getURI() throws IOException {
        URL url = getURL();
        try {
            return ResourceUtils.toURI(url);
        }
        catch (URISyntaxException ex) {
            throw new NestedIOException("Invalid URI [" + url + "]", ex);
        }
    }

    @Override
    public File getFile() throws IOException {
        throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
    }

    @Override
    public long contentLength() throws IOException {
        InputStream is = getInputStream();
        Assert.state(is != null, "Resource InputStream must not be null");
        try {
            long size = 0;
            byte[] buf = new byte[256];
            int read;
            while ((read = is.read(buf)) != -1) {
                size += read;
            }
            return size;
        }
        finally {
            try {
                is.close();
            }
            catch (IOException ex) {
            }
        }
    }

    @Override
    public long lastModified() throws IOException {
        File fileToCheck = getFileForLastModifiedCheck();
        long lastModified = fileToCheck.lastModified();
        if (lastModified == 0L && !fileToCheck.exists()) {
            throw new FileNotFoundException(getDescription() +
                    " cannot be resolved in the file system for checking its last-modified timestamp");
        }
        return lastModified;
    }

    protected File getFileForLastModifiedCheck() throws IOException {
        return getFile();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
    }

    @Override
    public String getFilename() {
        return null;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj == this ||
                (obj instanceof Resource && ((Resource) obj).getDescription().equals(getDescription())));
    }

    @Override
    public int hashCode() {
        return getDescription().hashCode();
    }


}
