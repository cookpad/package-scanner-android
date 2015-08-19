package android.support.multidex;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Proxy class to use package-private {@link MultiDexExtractor}
 */
public class package_scanner_MultiDexExtractor {

    public static List<File> load(Context context, ApplicationInfo applicationInfo, File dexDir,
            boolean forceReload) throws IOException {
        return MultiDexExtractor.load(context, applicationInfo, dexDir, forceReload);
    }
}
