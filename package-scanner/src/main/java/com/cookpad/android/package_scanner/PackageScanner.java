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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dalvik.system.DexFile;

public class PackageScanner {

    private static final String TAG = PackageScanner.class.getSimpleName();

    private static final String SECONDARY_FOLDER_NAME = "code_cache" + File.separator
            + "secondary-dexes";

    static boolean runningOnAndroid() {
        return System.getProperty("java.vm.name").equals("Dalvik");
    }

    static List<File> secondaryDexFiles(Context context)
            throws IOException {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        File dexDir = new File(applicationInfo.dataDir, SECONDARY_FOLDER_NAME);
        return package_scanner_MultiDexExtractor.load(context, applicationInfo, dexDir, false);
    }

    static List<String> secondaryDexClassNames(Context context)
            throws PackageManager.NameNotFoundException, IOException {
        List<String> classNames = new ArrayList<>();
        for (File file : secondaryDexFiles(context)) {
            String path = file.getAbsolutePath();
            try {
                DexFile dexfile;
                if (path.endsWith(".zip")) {
                    // new DexFile(path) throws "permission error in /data/dalvik-cache"
                    dexfile = DexFile.loadDex(path, path + ".tmp", 0);
                } else {
                    dexfile = new DexFile(path);
                }

                for (String className : asIterable(dexfile.entries())) {
                    classNames.add(className);
                }
            } catch (IOException e) {
                throw new IOException("Error at loading dex file '" + path + "'");
            }
        }
        return classNames;
    }

    public static <T> Set<Class<? extends T>> findConcreteSubclasses(Context context,
            Class<T> targetClass) {
        Set<Class<? extends T>> classes = new HashSet<>();
        for (Class<? extends T> c : findSubclasses(context, targetClass)) {
            if (!Modifier.isAbstract(c.getModifiers())) {
                classes.add(c);
            }
        }
        return classes;
    }

    public static <T> Set<Class<? extends T>> findSubclasses(Context context,
            Class<T> targetClass) {
        List<String> classNames = new ArrayList<>();
        ClassLoader classLoader = context.getClassLoader();

        Set<Class<? extends T>> classes = new HashSet<>();
        try {
            if (runningOnAndroid()) {
                String sourcePath = context.getApplicationInfo().sourceDir;
                DexFile dexfile = new DexFile(sourcePath);
                for (String path : asIterable(dexfile.entries())) {
                    Log.d(TAG, "XXX path=" + path);
                    classNames.add(path);
                }
                classNames.addAll(secondaryDexClassNames(context));
            } else {
                // JVM testing
                for (URL resourceUrl : asIterable(classLoader.getResources(""))) {
                    String path = resourceUrl.getFile();
                    if (path.contains("bin") || path.contains("classes")) {
                        classNames.addAll(listClassNameFromResourcePath(classLoader, path));
                    }
                }
            }

            for (String className : classNames) {
                classes = findSubclasses(className, classLoader, targetClass, classes);
            }
        } catch (IOException | PackageManager.NameNotFoundException e) {
            Log.w(TAG, "findSubclasses", e);
        }
        return classes;
    }

    // for JVM testing
    static List<String> listClassNameFromResourcePath(ClassLoader classLoader, String path) {
        List<String> classNames = new ArrayList<>();

        File file = new File(path);
        if (file.isDirectory()) {
            for (File p : file.listFiles()) {
                classNames.addAll(listClassNameFromResourcePath(classLoader, p.getPath()));
            }
        } else if (path.endsWith(".class")) {
            String className = path.replace(System.getProperty("user.dir"), "")
                    .replace(".class", "")
                    .replace(File.separator, ".");
            if (className.startsWith(".")) {
                className = className.substring(1);
            }

            while (className.contains(".")) {
                try {
                    Class<?> c = Class.forName(className, false, classLoader);
                    break;
                } catch (ClassNotFoundException e) {
                    // no op
                }
                className = className.substring(className.indexOf(".") + 1);
            }

            classNames.add(className);
        }

        return classNames;
    }

    static <T> Set<Class<? extends T>> findSubclasses(String className, ClassLoader classLoader,
            Class<? extends T> targetClass, Set<Class<? extends T>> classes) {

        try {
            Class<?> discoveredClass = Class.forName(className, false, classLoader);
            if (targetClass.isAssignableFrom(discoveredClass)) {
                classes.add(PackageScanner.<T>uncheckedClassCast(discoveredClass));
            }
        } catch (NoClassDefFoundError | ClassNotFoundException | IncompatibleClassChangeError e) {
            Log.w(TAG, "findSubclasses", e);
        }

        return classes;
    }

    @SuppressWarnings("unchecked")
    static <T> Class<? extends T> uncheckedClassCast(Class<?> k) {
        return (Class<T>) k;
    }

    static <T> Iterable<T> asIterable(final Enumeration<T> enumeration) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    @Override
                    public boolean hasNext() {
                        return enumeration.hasMoreElements();
                    }

                    @Override
                    public T next() {
                        return enumeration.nextElement();
                    }

                    @Override
                    public void remove() {
                    }
                };
            }
        };
    }
}
