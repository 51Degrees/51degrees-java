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

package pipeline.developerexamples.derivedproperty.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.slf4j.ILoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Supplies the source properties the example's scripts read. A real
 * Pipeline puts device detection or IP intelligence in this position, and
 * this small element stands in for one of those so that the example runs
 * with no resource key, no data file and no network connection.
 * <p>
 * The element reads three items of evidence and publishes each one under
 * the Element Data key {@code session}. Evidence that is not supplied
 * leaves the property with no value and a message saying why, which is how
 * the example shows a script producing no value and naming what was
 * missing.
 */
public final class SessionElement
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    /**
     * The Element Data key, being the part of session.PagesViewed before
     * the dot.
     */
    public static final String ELEMENT_DATA_KEY = "session";

    /**
     * The evidence holding how many pages the visitor has seen.
     */
    public static final String PAGES_VIEWED_EVIDENCE = "query.pagesviewed";

    /**
     * The evidence holding how long the page has been open, in seconds.
     */
    public static final String SECONDS_EVIDENCE = "query.secondssincepageload";

    /**
     * The evidence holding whether the pointer has moved.
     */
    public static final String POINTER_MOVED_EVIDENCE = "query.pointermoved";

    public static final String PAGES_VIEWED = "PagesViewed";
    public static final String SECONDS_SINCE_PAGE_LOAD = "SecondsSincePageLoad";
    public static final String POINTER_MOVED = "PointerMoved";

    private final EvidenceKeyFilter evidenceKeyFilter =
        new EvidenceKeyFilterWhitelist(Arrays.asList(
            PAGES_VIEWED_EVIDENCE, SECONDS_EVIDENCE, POINTER_MOVED_EVIDENCE));

    private final List<ElementPropertyMetaData> properties;

    /**
     * Construct the element. Use {@link SessionElementBuilder} to build one.
     * @param loggerFactory the logger factory for the element and its data
     */
    SessionElement(ILoggerFactory loggerFactory) {
        super(loggerFactory.getLogger(SessionElement.class.getName()),
            (flowData, element) -> new ElementDataBase(
                loggerFactory.getLogger(ElementDataBase.class.getName()),
                flowData) {
            });
        // The Derived Property Element checks, when the Pipeline is built,
        // that some element lists each source property a script names, so
        // these names match the names the scripts use after the dot.
        List<ElementPropertyMetaData> listed = new ArrayList<>();
        listed.add(new ElementPropertyMetaDataDefault(
            PAGES_VIEWED, this, "Example", Integer.class, true));
        listed.add(new ElementPropertyMetaDataDefault(
            SECONDS_SINCE_PAGE_LOAD, this, "Example", Integer.class, true));
        listed.add(new ElementPropertyMetaDataDefault(
            POINTER_MOVED, this, "Example", Boolean.class, true));
        this.properties = Collections.unmodifiableList(listed);
    }

    @Override
    public String getElementDataKey() {
        return ELEMENT_DATA_KEY;
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceKeyFilter;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return properties;
    }

    @Override
    protected void processInternal(FlowData data) {
        ElementData session =
            data.getOrAdd(getTypedDataKey(), getDataFactory());
        session.put(PAGES_VIEWED,
            valueOf(data, PAGES_VIEWED_EVIDENCE, Integer.class));
        session.put(SECONDS_SINCE_PAGE_LOAD,
            valueOf(data, SECONDS_EVIDENCE, Integer.class));
        session.put(POINTER_MOVED,
            valueOf(data, POINTER_MOVED_EVIDENCE, Boolean.class));
    }

    /**
     * The evidence as a property value, or a value with no value saying
     * which evidence the request did not carry.
     */
    private static <T> AspectPropertyValueDefault<T> valueOf(
        FlowData data,
        String evidence,
        Class<T> type) {
        TryGetResult<T> result = data.tryGetEvidence(evidence, type);
        if (result.hasValue()) {
            return new AspectPropertyValueDefault<>(result.getValue());
        }
        AspectPropertyValueDefault<T> missing =
            new AspectPropertyValueDefault<>();
        missing.setNoValueMessage(
            "the request did not carry the evidence '" + evidence + "'");
        return missing;
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
}
