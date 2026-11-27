/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.ipintelligence.engine.onpremise.flowelements;

import fiftyone.ipintelligence.engine.onpremise.interop.swig.EngineIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.VectorStringSwig;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

/**
 * The table of property indexes is made once, from the names the native
 * engine gives when the engine is created, and is not made again from a
 * later data set, even one whose native engine gives other names. The
 * engine is built from the Lite file with no names, then its native engine
 * is replaced by one the engine creates from the ASN data file.
 */
public class PropertyIndexLaterDataSetTests {

    private static final String ASN_IPI_DATA_FILE_NAME =
        "51Degrees-IPIV4AsnIpiV41.ipi";

    @Test
    public void LaterDataSet_TableNotMadeAgain() throws Exception {
        File asn;
        try {
            asn = getFilePath(ASN_IPI_DATA_FILE_NAME);
        }
        catch (IllegalArgumentException e) {
            assumeTrue(e.getMessage(), false);
            return;
        }
        try (IPIntelligenceOnPremiseEngine engine =
                 EngineNamesTests.build(null)) {
            Map<String, Integer> before = table(engine);
            assertFalse("Expected a table from the first data set.",
                before.isEmpty());

            Field field = IPIntelligenceOnPremiseEngine.class
                .getDeclaredField("engine");
            field.setAccessible(true);
            EngineIpiSwig first = (EngineIpiSwig) field.get(engine);
            field.set(engine, null);
            try {
                engine.refreshData(
                    engine.getDataFileMetaData().getIdentifier(),
                    Files.readAllBytes(asn.toPath()));
            }
            finally {
                first.delete();
            }

            List<String> laterNames;
            EngineIpiSwig later = (EngineIpiSwig) field.get(engine);
            try (VectorStringSwig names = later.getRequiredProperties()) {
                laterNames = new ArrayList<>(names);
            }
            assertFalse(
                "Expected the later data set to give names the first did " +
                    "not, so a table made again would differ.",
                before.keySet().containsAll(laterNames));

            assertEquals(before, table(engine));
            for (String name : laterNames) {
                if (before.containsKey(name) == false) {
                    assertNull(
                        "'" + name + "' is only in the later data set.",
                        engine.getPropertyIndex(name));
                }
            }
        }
    }

    private static Map<String, Integer> table(
        IPIntelligenceOnPremiseEngine engine) throws Exception {
        Field field = IPIntelligenceOnPremiseEngine.class
            .getDeclaredField("propertyIndexes");
        field.setAccessible(true);
        Map<String, Integer> table = new TreeMap<>();
        for (Map.Entry<?, ?> entry :
            ((Map<?, ?>) field.get(engine)).entrySet()) {
            table.put((String) entry.getKey(), (Integer) entry.getValue());
        }
        return table;
    }
}
