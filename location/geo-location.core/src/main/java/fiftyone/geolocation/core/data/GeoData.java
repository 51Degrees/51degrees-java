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

import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;

public interface GeoData extends AspectData {
    AspectPropertyValue<JavaScript> getJavaScript();
    void setJavaScript(AspectPropertyValue<JavaScript> javaScript);

    AspectPropertyValue<String> getRoad();
    void setRoad(AspectPropertyValue<String> road);

    AspectPropertyValue<String> getTown();
    void setTown(AspectPropertyValue<String> town);

    AspectPropertyValue<String> getSuburb();
    void setSuburb(AspectPropertyValue<String> suburb);

    AspectPropertyValue<String> getCounty();
    void setCounty(AspectPropertyValue<String> county);

    AspectPropertyValue<String> getRegion();
    void setRegion(AspectPropertyValue<String> region);

    AspectPropertyValue<String> getState();
    void setState(AspectPropertyValue<String> state);

    AspectPropertyValue<String> getZipCode();
    void setZipCode(AspectPropertyValue<String> zipCode);

    AspectPropertyValue<String> getCountry();
    void setCountry(AspectPropertyValue<String> country);

    AspectPropertyValue<String> getCountryCode();
    void setCountryCode(AspectPropertyValue<String> countryCode);

    AspectPropertyValue<String> getCountryCode3();
    void setCountryCode3(AspectPropertyValue<String> countryCode3);

    AspectPropertyValue<Integer> getTimeZoneOffset();
    void setTimeZoneOffset(AspectPropertyValue<Integer> timeZoneOffset);

    AspectPropertyValue<String> getAddress();
    void setAddress(AspectPropertyValue<String> address);
    
    AspectPropertyValue<String> getBuilding();
    void setBuilding(AspectPropertyValue<String> building);
       
    AspectPropertyValue<String> getStreetNumber();
    void setStreetNumber(AspectPropertyValue<String> streetNumber);
}
