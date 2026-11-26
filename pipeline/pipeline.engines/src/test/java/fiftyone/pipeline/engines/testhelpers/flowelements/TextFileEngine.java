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

package fiftyone.pipeline.engines.testhelpers.flowelements;

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngineBase;
import fiftyone.pipeline.engines.testhelpers.data.EmptyEngineData;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * An on-premise engine whose data file is a text file, which it reads when it
 * is built and again whenever its data is refreshed.
 */
public class TextFileEngine
    extends OnPremiseAspectEngineBase<EmptyEngineData, AspectPropertyMetaData> {

    @Override
    protected Class<EmptyEngineData> getDataType() {
        return EmptyEngineData.class;
    }

    private final String dataFilePath;

    private volatile String text;

    private final TypedKey<EmptyEngineData> typedKey =
        new TypedKeyDefault<>(getElementDataKey(), EmptyEngineData.class);

    public TextFileEngine(
        String dataFilePath,
        Logger logger,
        ElementDataFactory<EmptyEngineData> aspectDataFactory,
        String tempDataDirPath) throws IOException {
        super(logger, aspectDataFactory, tempDataDirPath);
        this.dataFilePath = dataFilePath;
        read();
    }

    private void read() throws IOException {
        text = new String(
            Files.readAllBytes(Paths.get(dataFilePath)),
            StandardCharsets.UTF_8);
    }

    /**
     * @return the text the data file held when it was last read
     */
    public String getText() {
        return text;
    }

    @Override
    public Date getDataFilePublishedDate(String dataFileIdentifier) {
        return null;
    }

    @Override
    public Date getDataFileUpdateAvailableTime(String dataFileIdentifier) {
        return null;
    }

    @Override
    public void refreshData(String dataFileIdentifier) {
        try {
            read();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void refreshData(String dataFileIdentifier, byte[] data) {
        text = new String(data, StandardCharsets.UTF_8);
    }

    @Override
    protected void processEngine(FlowData data, EmptyEngineData aspectData) {
        aspectData.setValueOne(text.length());
    }

    @Override
    public String getElementDataKey() {
        return "text-file";
    }

    @Override
    public TypedKey<EmptyEngineData> getTypedDataKey() {
        return typedKey;
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return new EvidenceKeyFilterWhitelist(
            Collections.<String>emptyList());
    }

    @Override
    public List<AspectPropertyMetaData> getProperties() {
        return Collections.emptyList();
    }

    @Override
    public String getDataSourceTier() {
        return "free";
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
}
