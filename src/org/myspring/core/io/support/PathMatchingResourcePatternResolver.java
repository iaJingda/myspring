package org.myspring.core.io.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.myspring.core.io.DefaultResourceLoader;
import org.myspring.core.io.Resource;
import org.myspring.core.io.ResourceLoader;
import org.myspring.core.io.UrlResource;
import org.myspring.core.util.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

public class PathMatchingResourcePatternResolver  implements ResourcePatternResolver {

    private static final Log logger = LogFactory.getLog(PathMatchingResourcePatternResolver.class);

    private static Method equinoxResolveMethod;

    static {
        try {
            // Detect Equinox OSGi (e.g. on WebSphere 6.1)
            Class<?> fileLocatorClass = ClassUtils.forName("org.eclipse.core.runtime.FileLocator",
                    PathMatchingResourcePatternResolver.class.getClassLoader());
            equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
            logger.debug("Found Equinox FileLocator for OSGi bundle URL resolution");
        }
        catch (Throwable ex) {
            equinoxResolveMethod = null;
        }
    }
    private final ResourceLoader resourceLoader;

    private PathMatcher pathMatcher = new AntPathMatcher();

    public PathMatchingResourcePatternResolver() {
        this.resourceLoader = new DefaultResourceLoader();
    }

    public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
        Assert.notNull(resourceLoader, "ResourceLoader must not be null");
        this.resourceLoader = resourceLoader;
    }

    public PathMatchingResourcePatternResolver(ClassLoader classLoader) {
        this.resourceLoader = new DefaultResourceLoader(classLoader);
    }

    public ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        Assert.notNull(locationPattern, "Location pattern must not be null");
        if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
            // a class path resource (multiple resources for same name possible)
            if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
                // a class path resource pattern
                return findPathMatchingResources(locationPattern);
            }
            else {
                // all class path resources with the given name
                return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
            }
        }
        else {
            // Generally only look for a pattern after a prefix here,
            // and on Tomcat only after the "*/" separator for its "war:" protocol.
            int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
                    locationPattern.indexOf(':') + 1);
            if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
                // a file pattern
                return findPathMatchingResources(locationPattern);
            }
            else {
                // a single resource with the given name
                return new Resource[] {getResourceLoader().getResource(locationPattern)};
            }
        }
    }


    protected Resource[] findAllClassPathResources(String location) throws IOException {
        String path = location;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Set<Resource> result = doFindAllClassPathResources(path);
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved classpath location [" + location + "] to resources " + result);
        }
        return result.toArray(new Resource[result.size()]);
    }

    protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
        Set<Resource> result = new LinkedHashSet<Resource>(16);
        ClassLoader cl = getClassLoader();
        Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
        while (resourceUrls.hasMoreElements()) {
            URL url = resourceUrls.nextElement();
            result.add(convertClassLoaderURL(url));
        }
        if ("".equals(path)) {
            // The above result is likely to be incomplete, i.e. only containing file system references.
            // We need to have pointers to each of the jar files on the classpath as well...
            addAllClassLoaderJarRoots(cl, result);
        }
        return result;
    }
    protected Resource convertClassLoaderURL(URL url) {
        return new UrlResource(url);
    }

    protected void addAllClassLoaderJarRoots(ClassLoader classLoader, Set<Resource> result) {
        if (classLoader instanceof URLClassLoader) {
            try {
                for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                    try {
                        UrlResource jarResource = new UrlResource(
                                ResourceUtils.JAR_URL_PREFIX + url + ResourceUtils.JAR_URL_SEPARATOR);
                        if (jarResource.exists()) {
                            result.add(jarResource);
                        }
                    }
                    catch (MalformedURLException ex) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Cannot search for matching files underneath [" + url +
                                    "] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
                        }
                    }
                }
            }
            catch (Exception ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cannot introspect jar files since ClassLoader [" + classLoader +
                            "] does not support 'getURLs()': " + ex);
                }
            }
        }

        if (classLoader == ClassLoader.getSystemClassLoader()) {
            // "java.class.path" manifest evaluation...
            addClassPathManifestEntries(result);
        }

        if (classLoader != null) {
            try {
                // Hierarchy traversal...
                addAllClassLoaderJarRoots(classLoader.getParent(), result);
            }
            catch (Exception ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cannot introspect jar files in parent ClassLoader since [" + classLoader +
                            "] does not support 'getParent()': " + ex);
                }
            }
        }
    }

    protected void addClassPathManifestEntries(Set<Resource> result) {
        try {
            String javaClassPathProperty = System.getProperty("java.class.path");
            for (String path : StringUtils.delimitedListToStringArray(
                    javaClassPathProperty, System.getProperty("path.separator"))) {
                try {
                    String filePath = new File(path).getAbsolutePath();
                    int prefixIndex = filePath.indexOf(':');
                    if (prefixIndex == 1) {
                        // Possibly "c:" drive prefix on Windows, to be upper-cased for proper duplicate detection
                        filePath = StringUtils.capitalize(filePath);
                    }
                    UrlResource jarResource = new UrlResource(ResourceUtils.JAR_URL_PREFIX +
                            ResourceUtils.FILE_URL_PREFIX + filePath + ResourceUtils.JAR_URL_SEPARATOR);
                    // Potentially overlapping with URLClassLoader.getURLs() result above!
                    if (!result.contains(jarResource) && !hasDuplicate(filePath, result) && jarResource.exists()) {
                        result.add(jarResource);
                    }
                }
                catch (MalformedURLException ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Cannot search for matching files underneath [" + path +
                                "] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
                    }
                }
            }
        }
        catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to evaluate 'java.class.path' manifest entries: " + ex);
            }
        }
    }

    private boolean hasDuplicate(String filePath, Set<Resource> result) {
        if (result.isEmpty()) {
            return false;
        }
        String duplicatePath = (filePath.startsWith("/") ? filePath.substring(1) : "/" + filePath);
        try {
            return result.contains(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX +
                    duplicatePath + ResourceUtils.JAR_URL_SEPARATOR));
        }
        catch (MalformedURLException ex) {
            // Ignore: just for testing against duplicate.
            return false;
        }
    }



    protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
        String rootDirPath = determineRootDir(locationPattern);
        String subPattern = locationPattern.substring(rootDirPath.length());
        Resource[] rootDirResources = getResources(rootDirPath);
        Set<Resource> result = new LinkedHashSet<Resource>(16);
        for (Resource rootDirResource : rootDirResources) {
            rootDirResource = resolveRootDirResource(rootDirResource);
            URL rootDirUrl = rootDirResource.getURL();
            if (equinoxResolveMethod != null) {
                if (rootDirUrl.getProtocol().startsWith("bundle")) {
                    rootDirUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);
                    rootDirResource = new UrlResource(rootDirUrl);
                }
            }
            if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
                result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));
            }
            else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {
                result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));
            }
            else {
                result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved location pattern [" + locationPattern + "] to resources " + result);
        }
        return result.toArray(new Resource[result.size()]);
    }



    @Override
    public Resource getResource(String location) {
        return getResourceLoader().getResource(location);
    }

    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }


    @Override
    public ClassLoader getClassLoader() {
        return null;
    }
}
