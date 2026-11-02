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

import fiftyone.ipintelligence.engine.onpremise.data.IPIntelligenceDataHash;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.*;
import fiftyone.ipintelligence.shared.IPIntelligenceDataBaseOnPremise;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.data.WeightedValue;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.slf4j.Logger;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Internal implementation of the {@link IPIntelligenceDataHash} interface. This can
 * only be constructed by the {@link IPIntelligenceOnPremiseEngine}.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/ip-intelligence-specification/pipeline-elements/ip-intelligence-on-premise.md#element-data">Specification</a>
 */
public class IPIntelligenceDataHashDefault
    extends IPIntelligenceDataBaseOnPremise
    implements IPIntelligenceDataHash, AutoCloseable {

    /**
     * True if the {@link #close()} method has been called.
     */
    private boolean closed = false;

    /**
     * List of native results which make up this result instance. Usually, this
     * will only be one.
     */
    private final List<ResultsIpiSwig> resultsList = new ArrayList<>();

    /**
     * Constructs a new instance.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param engine the engine which created the instance
     * @param missingPropertyService service used to determine the reason for
     *                               a property value being missing
     */
    IPIntelligenceDataHashDefault(
        Logger logger,
        FlowData flowData,
        IPIntelligenceOnPremiseEngine engine,
        MissingPropertyService missingPropertyService) {
        super(logger, flowData, engine, missingPropertyService);
    }
    
    /**
     * Add the native results to the list of results contained in this instance.
     * @param results the results to add
     */
    void setResults(ResultsIpiSwig results) {
        checkState();
        resultsList.add(results);
    }

    /**
     * Get a single native results instance. If there is only one available,
     * that is what is returned. If there are more, then the first one which
     * contains values for the requested property is returned.
     * @param propertyName used to select results from list
     * @return single native results instance
     */
    private ResultsIpiSwig getSingleResults(String propertyName) {
        return resultsList.size() == 1 ?
            resultsList.get(0) : getResultsContainingProperty(propertyName);
    }

    /**
     * Get the first native results in {@link #resultsList} which contains
     * values for the requested property.
     * @param propertyName name of the requested property
     * @return native results containing values, or null if none were found
     */
    private ResultsIpiSwig getResultsContainingProperty(String propertyName) {
        for (ResultsIpiSwig results : resultsList) {
            if (results.containsProperty(propertyName)) {
                return results;
            }
        }
        return null;
    }

    @Override
    protected boolean propertyIsAvailable(String propertyName) {
        checkState();
        for (ResultsIpiSwig results : resultsList) {
            if (results.containsProperty(propertyName)) {
                return true;
            }
        }
        return false;
    }

    private static <
            TListValueSwig extends AutoCloseable,
            TListSwig extends AutoCloseable & List<TWeightedValueSwig>,
            TWeightedValueSwig,
            TElement>
    void populateWeightedValues(
            AspectPropertyValue<List<?>> result,
            Supplier<TListValueSwig> listValueSwigSupplier,
            Predicate<TListValueSwig> listValueSwigHasValue,
            Function<TListValueSwig, String> listValueSwigGetNoValueMessage,
            Function<TListValueSwig, TListSwig> listSwigExtractor,
            Function<TWeightedValueSwig, TElement> elementConverter) {
        try (TListValueSwig listValueSwig = listValueSwigSupplier.get())
        {
            if (listValueSwigHasValue.test(listValueSwig)) {
                try (TListSwig listSwig = listSwigExtractor.apply(listValueSwig)) {
                    List<?> convertedWeightedElements = listSwig.stream()
                            .map(elementConverter)
                            .collect(Collectors.toList());
                    result.setValue(Collections.unmodifiableList(convertedWeightedElements));
                }
            }
            else {
                result.setNoValueMessage(listValueSwigGetNoValueMessage.apply(listValueSwig));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AspectPropertyValue<List<?>> getValues(String propertyName,
                                                  Class<?>[] parametrizedTypes) {
        checkState();
        AspectPropertyValue<List<?>> result =
            new AspectPropertyValueDefault<>();
        ResultsIpiSwig results = getSingleResults(propertyName);
        if (results == null) {
            return result;
        }
        if (parametrizedTypes.length < 2) {
            return null;
        }
        Class<?> listedType = parametrizedTypes[1];
        boolean isWeighted = listedType.equals(IWeightedValue.class) || listedType.equals(WeightedValue.class);
        if (!isWeighted) {
            return null;
        }
        if (parametrizedTypes.length < 3) {
            return null;
        }
        Class<?> valueType = parametrizedTypes[2];
        if (valueType.equals(Boolean.class)) {
            populateWeightedValues(
                    result,
                    () -> results.getValuesAsWeightedBoolList(propertyName),
                    WeightedBoolListValueSwig::hasValue,
                    WeightedBoolListValueSwig::getNoValueMessage,
                    WeightedBoolListValueSwig::getValue,
                    x -> new WeightedValue<>(x.getShortRawWeight(), x.getValue())
            );
        } else if (valueType.equals(Double.class)) {
            populateWeightedValues(
                    result,
                    () -> results.getValuesAsWeightedDoubleList(propertyName),
                    WeightedDoubleListValueSwig::hasValue,
                    WeightedDoubleListValueSwig::getNoValueMessage,
                    WeightedDoubleListValueSwig::getValue,
                    x -> new WeightedValue<>(x.getShortRawWeight(), x.getValue())
            );
        } else if (valueType.equals(Float.class)) {
            populateWeightedValues(
                    result,
                    () -> results.getValuesAsWeightedDoubleList(propertyName),
                    WeightedDoubleListValueSwig::hasValue,
                    WeightedDoubleListValueSwig::getNoValueMessage,
                    WeightedDoubleListValueSwig::getValue,
                    x -> new WeightedValue<>(x.getShortRawWeight(), (float)x.getValue())
            );
        } else if (valueType.equals(Integer.class)) {
            populateWeightedValues(
                    result,
                    () -> results.getValuesAsWeightedIntegerList(propertyName),
                    WeightedIntListValueSwig::hasValue,
                    WeightedIntListValueSwig::getNoValueMessage,
                    WeightedIntListValueSwig::getValue,
                    x -> new WeightedValue<>(x.getShortRawWeight(), x.getValue())
            );
        } else if (valueType.equals(InetAddress.class)
                || valueType.equals(Inet4Address.class)
                || valueType.equals(Inet6Address.class)) {
            populateWeightedValues(
                    result,
                    () -> results.getValuesAsWeightedStringList(propertyName),
                    WeightedStringListValueSwig::hasValue,
                    WeightedStringListValueSwig::getNoValueMessage,
                    WeightedStringListValueSwig::getValue,
                    x -> {
                        try {
                            return new WeightedValue<>(x.getShortRawWeight(), InetAddress.getByName(x.getValue()));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        } else if (valueType.equals(String.class)) {
            populateWeightedValues(
                    result,
                    () -> results.getValuesAsWeightedUTF8StringList(propertyName),
                    WeightedUTF8StringListValueSwig::hasValue,
                    WeightedUTF8StringListValueSwig::getNoValueMessage,
                    WeightedUTF8StringListValueSwig::getValue,
                    x -> {
                        String theString;
                        try (UTF8StringSwig stringSwig = x.getValue()) {
                            byte[] bytes = new byte[stringSwig.size()];
                            for (int i = 0; i < stringSwig.size(); i++) {
                                bytes[i] = (byte)(short)stringSwig.get(i);
                            }
                            theString = new String(bytes, StandardCharsets.UTF_8);
                        }
                        return new WeightedValue<>(x.getShortRawWeight(), theString);
                    }
            );
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + valueType.getName());
        }
        return result;
    }

    @Override
    protected AspectPropertyValue<String> getValueAsString(String propertyName) {
        AspectPropertyValue<String> result = new AspectPropertyValueDefault<>();
        ResultsIpiSwig results = getSingleResults(propertyName);
        if (results != null) {

            try (StringValueSwig value = results.getValueAsString(
                propertyName)) {
                if (value.hasValue()) {
                    result.setValue(value.getValue());
                }
                else {
                    result.setNoValueMessage(value.getNoValueMessage());
                }
            }
        }
        return result;
    }

    @Override
    protected AspectPropertyValue<JavaScript> getValueAsJavaScript(
        String propertyName) {
        AspectPropertyValue<JavaScript> result =
            new AspectPropertyValueDefault<>();
        ResultsIpiSwig results = getSingleResults(propertyName);
        if (results != null) {
            try (StringValueSwig value = results.getValueAsString(
                propertyName)) {
                if (value.hasValue()) {
                    result.setValue(new JavaScript(value.getValue()));
                }
                else {
                    result.setNoValueMessage(value.getNoValueMessage());
                }
            }
        }
        return result;
    }

    @Override
    protected AspectPropertyValue<Integer> getValueAsInteger(
        String propertyName) {
        AspectPropertyValue<Integer> result = new AspectPropertyValueDefault<>();
        ResultsIpiSwig results = getSingleResults(propertyName);
        if (results != null) {
            try (IntegerValueSwig value = results.getValueAsInteger(
                propertyName)) {
                if (value.hasValue()) {
                    result.setValue(value.getValue());
                }
                else {
                    result.setNoValueMessage(value.getNoValueMessage());
                }
            }
        }
        return result;
    }

    @Override
    protected AspectPropertyValue<Boolean> getValueAsBool(String propertyName) {
        AspectPropertyValue<Boolean> result = new AspectPropertyValueDefault<>();
        ResultsIpiSwig results = getSingleResults(propertyName);
        if (results != null) {
            try (BoolValueSwig value = results.getValueAsBool(
                propertyName)) {
                if (value.hasValue()) {
                    result.setValue(value.getValue());
                }
                else {
                    result.setNoValueMessage(value.getNoValueMessage());
                }
            }
        }
        return result;
    }

    @Override
    protected AspectPropertyValue<Double> getValueAsDouble(String propertyName) {
        AspectPropertyValue<Double> result = new AspectPropertyValueDefault<>();
        ResultsIpiSwig results = getSingleResults(propertyName);
        if (results != null) {
            try (DoubleValueSwig value = results.getValueAsDouble(
                propertyName)) {
                if (value.hasValue()) {
                    result.setValue(value.getValue());
                }
                else {
                    result.setNoValueMessage(value.getNoValueMessage());
                }
            }
        }
        return result;
    }

    @Override
    protected <T> TryGetResult<T> tryGetValue(
        String key,
        Class<T> type,
        Class<?>... parameterisedTypes) {
        checkState();
        return super.tryGetValue(key, type, parameterisedTypes);
    }

    private void checkState() {
        if (closed) {
            throw new IllegalStateException("The IPIntelligenceDataHash instance has " +
                "been closed, and cannot be used. Any result processing should " +
                "be carried out within a 'try-with-resource' block which " +
                "closes the FlowData and any AutoCloseable elements.");
        }
    }

    @Override
    public void close() {
        closed = true;
        for (ResultsIpiSwig result : resultsList) {
            try {
                result.close();
            } catch (Exception e) {
                logger.error("Failed to close native results instance. " +
                    "A IPIntelligenceDataHash instance contains native unmanaged " +
                    "memory which needs to be closed. Failing to close " +
                    "could lead to memory leaks.", e);
            }
        }
    }
}