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

package fiftyone.devicedetection.examples.console;

import fiftyone.devicedetection.examples.shared.DataFileHelper;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Runs the UpdateDataFile example against a stand-in for the Distributor on
 * this machine, so the test needs no license key and reaches no 51Degrees
 * service. The stand-in lists two products, as the Distributor does for a
 * license key that holds several, and serves a copy of the data file the
 * build fetched, with its published date moved on to tomorrow.
 */
public class UpdateDataFileTest {

    /**
     * A made-up license key. The example accepts it as a key, and the
     * stand-in serves the data file whatever key it is sent.
     */
    private static final String LICENSE_KEY = "TESTKEYNOTREALABCDEFGHIJ1234";

    /**
     * The data file the example starts from, which the build fetches. The
     * example will not download over the free Lite data file, so the test
     * needs a file the Distributor serves.
     */
    private static final String DATA_FILE =
        "device-detection/" + DataFileHelper.TAC_HASH_DATA_FILE_NAME;

    /**
     * The product of that data file.
     */
    private static final String PRODUCT = "V4TAC";

    /**
     * A product the stand-in lists beside the data file's own, as the
     * Distributor does for a license key that holds a Device Detection and
     * an IP Intelligence product.
     */
    private static final String OTHER_PRODUCT = "IPIV4Enterprise";

    /**
     * With no product named, the first request gets the list of products,
     * and the second asks for the product of the data file the example
     * starts from.
     */
    @Test
    public void updateExample_NoProduct_AsksForTheProductOfTheDataFile()
        throws Exception {
        UpdateRun run = runUpdateExample(null);

        assertTrue("The example should have asked the stand-in twice. " +
            "Requests: " + run.requests, run.requests.size() >= 2);
        assertRequest("The first request should name no product, and get " +
            "the list of products.", null, 300, run, 0);
        assertRequest("The second request should ask for the product of " +
            "the data file, and get the newer data file.", PRODUCT, 200,
            run, 1);
        run.assertCompleted();
    }

    /**
     * With a product given as an argument, the first request asks for it.
     */
    @Test
    public void updateExample_ProductArgument_AsksForThatProduct()
        throws Exception {
        UpdateRun run = runUpdateExample(PRODUCT);

        assertFalse("The example should have asked the stand-in.",
            run.requests.isEmpty());
        assertRequest("The first request should ask for the product given, " +
            "and get the newer data file.", PRODUCT, 200, run, 0);
        for (DataUpdateStandIn.Request request : run.requests) {
            assertNotNull("No request should leave out the product given. " +
                "Requests: " + run.requests, request.product);
        }
        run.assertCompleted();
    }

    private static void assertRequest(String message, String product,
                                      int status, UpdateRun run, int index) {
        DataUpdateStandIn.Request request = run.requests.get(index);
        String detail = message + " Requests: " + run.requests;
        assertEquals(detail, product, request.product);
        assertEquals(detail, status, request.status);
    }

    /**
     * Runs the example on a copy of the data file, with its update URL
     * pointed at a stand-in for the Distributor. The stand-in serves
     * another copy of the data file with its published date moved on to
     * tomorrow, so the copy is newer than the file the example starts from.
     * @param productArgument the product to give the example, or null for
     *                        none
     * @return the requests the stand-in received, and how the example ended
     */
    private static UpdateRun runUpdateExample(String productArgument)
        throws Exception {
        Path fetched = fetched(DATA_FILE);
        Path directory = Files.createTempDirectory("51Degrees-UpdateDataFile-");
        try {
            Path dataFile = directory.resolve(fetched.getFileName().toString());
            Files.copy(fetched, dataFile);
            Path served = directory.resolve("served.hash");
            Files.copy(fetched, served);
            LocalDate newer = LocalDate.now(ZoneOffset.UTC).plusDays(1);
            DataUpdateStandIn.writePublished(served, newer);

            try (DataUpdateStandIn standIn = DataUpdateStandIn.start(
                    Arrays.asList(OTHER_PRODUCT, PRODUCT), served)) {
                Exception failure = null;
                try {
                    UpdateDataFile.run(dataFile.toString(), LICENSE_KEY,
                        productArgument, standIn.getUrl(), false);
                } catch (Exception e) {
                    failure = e;
                }
                return new UpdateRun(standIn.requests(), failure,
                    DataUpdateStandIn.readPublished(dataFile), newer);
            }
        } finally {
            delete(directory);
        }
    }

    /**
     * The data file the build fetched.
     */
    private static Path fetched(String file) {
        try {
            return DataFileHelper.findDataFile(file).toPath();
        } catch (IllegalArgumentException e) {
            throw new AssertionError("The test needs the data file '" + file +
                "', which the build fetches into cxx/data.", e);
        }
    }

    /**
     * Deletes the folder and the files in it that can be deleted.
     */
    private static void delete(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            // The folder is in the temporary folder, which the system clears.
        }
    }

    /**
     * The requests the stand-in received during a run of the example, and
     * how the example ended.
     */
    private static final class UpdateRun {
        final List<DataUpdateStandIn.Request> requests;
        final Exception failure;
        final LocalDate published;
        final LocalDate newer;

        UpdateRun(List<DataUpdateStandIn.Request> requests,
                  Exception failure, LocalDate published, LocalDate newer) {
            this.requests = requests;
            this.failure = failure;
            this.published = published;
            this.newer = newer;
        }

        /**
         * Fails unless the example ended without an error and its data file
         * holds the newer data the stand-in served.
         */
        void assertCompleted() {
            if (failure != null) {
                throw new AssertionError("The example failed: " + failure,
                    failure);
            }
            assertEquals("The example's data file should hold the newer " +
                "data the stand-in served.", newer, published);
        }
    }
}
