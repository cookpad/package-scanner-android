package com.cookpad.android.package_scanner.example.test;

import com.cookpad.android.package_scanner.PackageScanner;
import com.cookpad.android.package_scanner.example.MainActivity;

import org.hamcrest.Matchers;
import org.junit.Test;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("unchecked")
public class MultiDexApplicationTest extends Matchers {

    Context getContext() {
        return InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testMultiDexPackageScanner() throws Exception {
        Set<Class<? extends Activity>> classes = PackageScanner
                .findConcreteSubclasses(getContext(), Activity.class);

        assertThat(classes.size(), is(not(0)));
        assertThat(classes,
                Matchers.<Class<? extends Activity>>contains(MainActivity.class));
    }
}
