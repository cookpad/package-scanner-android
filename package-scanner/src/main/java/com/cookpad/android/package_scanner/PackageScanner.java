package com.cookpad.android.package_scanner;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.multidex.package_scanner_MultiDexExtractor;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dalvik.system.DexFile;

public class PackageScanner {

    private static final String TAG = PackageScanner.class.getSimpleName();

    private static final String SECONDARY_FOLDER_NAME = "code_cache" + File.separator
            + "secondary-dexes";

    private static final String EXTRACTED_SUFFIX = ".zip";

    public static List<File> secondaryDexPath(Context context)
            throws IOException {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        File dexDir = new File(applicationInfo.dataDir, SECONDARY_FOLDER_NAME);
        return package_scanner_MultiDexExtractor.load(context, applicationInfo, dexDir, false);
    }

    public static List<String> secondaryDex(Context context)
            throws PackageManager.NameNotFoundException, IOException {
        List<String> classNames = new ArrayList<>();
        for (File file : secondaryDexPath(context)) {
            String path = file.getAbsolutePath();
            try {
                DexFile dexfile;
                if (path.endsWith(EXTRACTED_SUFFIX)) {
                    //NOT use new DexFile(path), because it will throw "permission error in /data/dalvik-cache"
                    dexfile = DexFile.loadDex(path, path + ".tmp", 0);
                } else {
                    dexfile = new DexFile(path);
                }
                Enumeration<String> dexEntries = dexfile.entries();
                while (dexEntries.hasMoreElements()) {
                    classNames.add(dexEntries.nextElement());
                }
            } catch (IOException e) {
                throw new IOException("Error at loading dex file '" +
                        path + "'");
            }
        }
        return classNames;
    }

    public static <T> List<Class<? extends T>> searchClasses(Context context,
            Class<T> targetClass) {

        String packageName = context.getPackageName();
        String sourcePath = context.getApplicationInfo().sourceDir;
        List<String> paths = new ArrayList<>();

        Set<Class<? extends T>> classes = new HashSet<>();
        try {
            if (sourcePath != null && !(new File(sourcePath).isDirectory())) {
                DexFile dexfile = new DexFile(sourcePath);
                Enumeration<String> entries = dexfile.entries();

                while (entries.hasMoreElements()) {
                    paths.add(entries.nextElement());
                }
            } else { // JVM fallback
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Enumeration<URL> resources = classLoader.getResources("");

                while (resources.hasMoreElements()) {
                    String path = resources.nextElement().getFile();
                    if (path.contains("bin") || path.contains("classes")) {
                        paths.add(path);
                    }
                }
            }

            paths.addAll(secondaryDex(context));

            for (String path : paths) {
                File file = new File(path);
                classes = searchClasses(file, packageName, context.getClassLoader(), targetClass,
                        classes);
            }
        } catch (IOException | PackageManager.NameNotFoundException e) {
            Log.w(TAG, "searchClasses", e);
        }
        return new ArrayList<>(classes);
    }

    private static <T> Set<Class<? extends T>> searchClasses(File path, String packageName,
            ClassLoader classLoader, Class<? extends T> targetClass,
            Set<Class<? extends T>> classes) {
        if (path.isDirectory()) {
            for (File file : path.listFiles()) {
                classes = searchClasses(file, packageName, classLoader, targetClass, classes);
            }
            return classes;
        } else {
            String className = path.getName();

            // JVM fallback
            if (!path.getPath().equals(className)) {
                className = path.getPath();

                if (className.endsWith(".class")) {
                    className = className.substring(0, className.length() - 6);
                } else {
                    return classes;
                }

                className = className.replace(System.getProperty("file.separator"), ".");

                int packageNameIndex = className.lastIndexOf(packageName);
                if (packageNameIndex < 0) {
                    return classes;
                }

                className = className.substring(packageNameIndex);
            }

            try {
                Class<?> discoveredClass = Class.forName(className, false, classLoader);
                if (!discoveredClass.equals(targetClass)
                        && targetClass.isAssignableFrom(discoveredClass)
                        && !Modifier.isAbstract(discoveredClass.getModifiers())) {

                    classes.add(PackageScanner.<T>uncheckedClassCast(discoveredClass));
                }
            } catch (NoClassDefFoundError | ClassNotFoundException | IncompatibleClassChangeError e) {
                Log.w(TAG, "searchClasses", e);
            }

            return classes;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> uncheckedClassCast(Class<?> k) {
        return (Class<T>) k;
    }
}
