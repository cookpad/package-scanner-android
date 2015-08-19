package com.cookpad.android.package_scanner.test;

import com.cookpad.android.package_scanner.PackageScanner;
import com.cookpad.android.package_scanner.test.fixture.Base;
import com.cookpad.android.package_scanner.test.fixture.ConcreteDerived;
import com.cookpad.android.package_scanner.test.fixture.DerivedFromAbstractDerived;
import com.cookpad.android.package_scanner.test.fixture.DerivedFromConcreteDerived;

import org.junit.Test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import java.util.Set;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

@SuppressWarnings("unchecked")
public class PackageScannerTest {

    Context getContext() {
        return InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testScanClassesInTests() throws Exception {
        Set<Class<? extends Base>> classes = PackageScanner.findSubclasses(getContext(), Base.class);

        assertThat(classes.size(), is(4));

        assertThat(classes, contains(
                ConcreteDerived.class,
                Base.class,
                DerivedFromAbstractDerived.class,
                DerivedFromConcreteDerived.class));
    }
}
