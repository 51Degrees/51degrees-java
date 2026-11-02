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

package fiftyone.geolocation.core.data;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.slf4j.Logger;

public class GeoDataDefault extends AspectDataBase implements GeoData {

    public GeoDataDefault(Logger logger, FlowData flowData, AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine, MissingPropertyService missingPropertyService) {
        super(logger, flowData, engine, missingPropertyService);
        setJavaScript(new AspectPropertyValueDefault<JavaScript>(new JavaScript("")));
    }


    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<JavaScript> getJavaScript() {
        return getAs("javascript", AspectPropertyValue.class, JavaScript.class);
    }

    @Override
    public void setJavaScript(AspectPropertyValue<JavaScript> javaScript) {
        put("javascript", javaScript);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getRoad() {
        return getAs("road", AspectPropertyValue.class);
    }

    @Override
    public void setRoad(AspectPropertyValue<String> road) {
        put("road", road);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getTown() {
        return getAs("town", AspectPropertyValue.class);
    }

    @Override
    public void setTown(AspectPropertyValue<String> town) {
        put("town", town);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getSuburb() {
        return getAs("suburb", AspectPropertyValue.class);
    }

    @Override
    public void setSuburb(AspectPropertyValue<String> suburb) {
        put("suburb", suburb);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getCounty() {
        return getAs("county", AspectPropertyValue.class);
    }

    @Override
    public void setCounty(AspectPropertyValue<String> county) {
        put("county", county);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getRegion() {
        return getAs("region", AspectPropertyValue.class);
    }

    @Override
    public void setRegion(AspectPropertyValue<String> region) {
        put("region", region);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getState() {
        return getAs("state", AspectPropertyValue.class);
    }

    @Override
    public void setState(AspectPropertyValue<String> state) {
        put("state", state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getZipCode() {
        return getAs("zipcode", AspectPropertyValue.class);
    }

    @Override
    public void setZipCode(AspectPropertyValue<String> zipCode) {
        put("zipcode", zipCode);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getCountry() {
        return getAs("country", AspectPropertyValue.class);
    }

    @Override
    public void setCountry(AspectPropertyValue<String> country) {
        put("country", country);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getCountryCode() {
        return getAs("countrycode", AspectPropertyValue.class);
    }

    @Override
    public void setCountryCode(AspectPropertyValue<String> countryCode) {
        put("countrycode", countryCode);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getCountryCode3() {
        return getAs("countrycode3", AspectPropertyValue.class);
    }

    @Override
    public void setCountryCode3(AspectPropertyValue<String> countryCode3) {
        put("countrycode3", countryCode3);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<Integer> getTimeZoneOffset() {
        return getAs("timezoneoffset", AspectPropertyValue.class);
    }

    @Override
    public void setTimeZoneOffset(AspectPropertyValue<Integer> timeZoneOffset) {
        put("timezoneoffset", timeZoneOffset);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AspectPropertyValue<String> getAddress() {
        return getAs("address", AspectPropertyValue.class);
    }

    @Override
    public void setAddress(AspectPropertyValue<String> address) {
        put("address", address);
    }

	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getBuilding() {
		return getAs("building", AspectPropertyValue.class);
	}

	@Override
	public void setBuilding(AspectPropertyValue<String> building) {
		put("building", building);		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getStreetNumber() {
		return getAs("streetNumber", AspectPropertyValue.class);
	}

	@Override
	public void setStreetNumber(AspectPropertyValue<String> streetNumber) {
		put("streetNumber", streetNumber);		
	}
}
