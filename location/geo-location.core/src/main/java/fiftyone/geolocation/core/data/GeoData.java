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

/** The location results an engine returns for the evidence it was given. */
public interface GeoData extends AspectData {
    /**
     * The JavaScript that sends the position from the client.
     *
     * @return the JavaScript that sends the position from the client
     */
    AspectPropertyValue<JavaScript> getJavaScript();
    /**
     * Set the JavaScript that sends the position from the client.
     *
     * @param javaScript the JavaScript that sends the position from the client
     */
    void setJavaScript(AspectPropertyValue<JavaScript> javaScript);

    /**
     * The road name.
     *
     * @return the road name
     */
    AspectPropertyValue<String> getRoad();
    /**
     * Set the road name.
     *
     * @param road the road name
     */
    void setRoad(AspectPropertyValue<String> road);

    /**
     * The town name.
     *
     * @return the town name
     */
    AspectPropertyValue<String> getTown();
    /**
     * Set the town name.
     *
     * @param town the town name
     */
    void setTown(AspectPropertyValue<String> town);

    /**
     * The suburb name.
     *
     * @return the suburb name
     */
    AspectPropertyValue<String> getSuburb();
    /**
     * Set the suburb name.
     *
     * @param suburb the suburb name
     */
    void setSuburb(AspectPropertyValue<String> suburb);

    /**
     * The county name.
     *
     * @return the county name
     */
    AspectPropertyValue<String> getCounty();
    /**
     * Set the county name.
     *
     * @param county the county name
     */
    void setCounty(AspectPropertyValue<String> county);

    /**
     * The region name.
     *
     * @return the region name
     */
    AspectPropertyValue<String> getRegion();
    /**
     * Set the region name.
     *
     * @param region the region name
     */
    void setRegion(AspectPropertyValue<String> region);

    /**
     * The state name.
     *
     * @return the state name
     */
    AspectPropertyValue<String> getState();
    /**
     * Set the state name.
     *
     * @param state the state name
     */
    void setState(AspectPropertyValue<String> state);

    /**
     * The zip or postal code.
     *
     * @return the zip or postal code
     */
    AspectPropertyValue<String> getZipCode();
    /**
     * Set the zip or postal code.
     *
     * @param zipCode the zip or postal code
     */
    void setZipCode(AspectPropertyValue<String> zipCode);

    /**
     * The country name.
     *
     * @return the country name
     */
    AspectPropertyValue<String> getCountry();
    /**
     * Set the country name.
     *
     * @param country the country name
     */
    void setCountry(AspectPropertyValue<String> country);

    /**
     * The two letter country code.
     *
     * @return the two letter country code
     */
    AspectPropertyValue<String> getCountryCode();
    /**
     * Set the two letter country code.
     *
     * @param countryCode the two letter country code
     */
    void setCountryCode(AspectPropertyValue<String> countryCode);

    /**
     * The three letter country code.
     *
     * @return the three letter country code
     */
    AspectPropertyValue<String> getCountryCode3();
    /**
     * Set the three letter country code.
     *
     * @param countryCode3 the three letter country code
     */
    void setCountryCode3(AspectPropertyValue<String> countryCode3);

    /**
     * The time zone offset.
     *
     * @return the time zone offset
     */
    AspectPropertyValue<Integer> getTimeZoneOffset();
    /**
     * Set the time zone offset.
     *
     * @param timeZoneOffset the time zone offset
     */
    void setTimeZoneOffset(AspectPropertyValue<Integer> timeZoneOffset);

    /**
     * The full address.
     *
     * @return the full address
     */
    AspectPropertyValue<String> getAddress();
    /**
     * Set the full address.
     *
     * @param address the full address
     */
    void setAddress(AspectPropertyValue<String> address);
    
    /**
     * The building name.
     *
     * @return the building name
     */
    AspectPropertyValue<String> getBuilding();
    /**
     * Set the building name.
     *
     * @param building the building name
     */
    void setBuilding(AspectPropertyValue<String> building);
       
    /**
     * The street number.
     *
     * @return the street number
     */
    AspectPropertyValue<String> getStreetNumber();
    /**
     * Set the street number.
     *
     * @param streetNumber the street number
     */
    void setStreetNumber(AspectPropertyValue<String> streetNumber);
}
