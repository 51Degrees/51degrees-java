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

package fiftyone.pipeline.engines.fiftyone.data;

import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;

import java.io.File;
import java.util.function.Function;

/**
 * Default implementation of the {@link FiftyOneDataFile} interface.
 */
public class FiftyOneDataFileDefault extends AspectEngineDataFileDefault implements FiftyOneDataFile {

    /**
     * The start of a download type and the prefix of the product names of
     * its data files. The product name is the prefix followed by the tier
     * of the data file, so a Device Detection Hash file of tier TAC is
     * V4TAC and an IP Intelligence file of tier Enterprise is
     * IPIV4Enterprise.
     */
    private static final String[][] PREFIXES = {
        {"Hash", "V4"},
        {"IPI", "IPIV4"},
    };

    private String downloadType;

    private Function<String, String> productReader;

    /**
     * Set the function that reads the product of a data file from its
     * header without loading it, given the path of the file. It is used
     * until an engine has loaded the data file, for example by the update
     * on start-up, which runs before the engine is created. The function
     * may return null, or throw, when the product cannot be read.
     * @param productReader the function, or null for none
     */
    public void setProductReader(Function<String, String> productReader) {
        this.productReader = productReader;
    }

    /**
     * {@inheritDoc}
     * Once an engine has loaded the data file, the name is made from the
     * tier of the data the engine has loaded. Before then, the tier is read
     * from the header of the data file at the configured location, if a
     * product reader is set. No product is known where there is no reader,
     * no file at that location, or a read that fails.
     */
    @Override
    public String getDataUpdateProduct() {
        String type = getDataUpdateDownloadType();
        String tier = getEngine() != null ?
            getEngine().getDataSourceTier() :
            readProductFromFile();
        if (type == null || tier == null || tier.isEmpty()) {
            return null;
        }
        for (String[] row : PREFIXES) {
            if (startsWith(type, row[0])) {
                // A tier that already starts with the prefix is the
                // product name as it is.
                return startsWith(tier, row[1]) ? tier : row[1] + tier;
            }
        }
        return null;
    }

    /**
     * The product read from the header of the data file at the configured
     * location, or null if it cannot be read.
     */
    private String readProductFromFile() {
        Function<String, String> reader = productReader;
        if (reader == null || getConfiguration() == null) {
            return null;
        }
        String path = getDataFilePath();
        if (path == null || path.isEmpty() || new File(path).isFile() == false) {
            return null;
        }
        try {
            return reader.apply(path);
        } catch (RuntimeException | LinkageError e) {
            // A file that cannot be read, or a native library that cannot
            // be loaded, gives no product.
            return null;
        }
    }

    private static boolean startsWith(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    @Override
    public String getDataUpdateDownloadType() {
        return downloadType;
    }

    @Override
    public void setDataUpdateDownloadType(String type) {
        this.downloadType = type;
    }
}
