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
package fiftyone.ipintelligence.shared;
import java.net.InetAddress;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WktString;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.slf4j.Logger;
import java.util.List;
public abstract class IPIntelligenceDataBase extends AspectDataBase implements IPIntelligenceData
{
/**
 * Constructor.
 * @param logger used for logging
 * @param flowData the {@link FlowData} instance this element data will be
 *                 associated with
 * @param engine the engine which created the instance
 * @param missingPropertyService service used to determine the reason for
 *                               a property value being missing
 */
	protected IPIntelligenceDataBase(
		Logger logger,
		FlowData flowData,
		AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine,
		MissingPropertyService missingPropertyService) {
		super(logger, flowData, engine, missingPropertyService);
	}
	/**
	 * Radius in kilometers of the circle centred around the most probable location that encompasses the entire area, measured against the trimmed area. Where multiple areas are returned, this will only cover the area the most probable location is in. See Areas property. This will likely be a very large distance. It is recommended to use the AccuracyRadiusMin property.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Integer> getAccuracyRadiusMax() { return getAs("accuracyradiusmax", AspectPropertyValue.class, Integer.class); }
	/**
	 * Radius in kilometers of the largest circle centred around the most probable location that fits within the area, measured against the trimmed area. Where multiple areas are returned, only the area that the most probable location falls within is considered. See Areas property.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Integer> getAccuracyRadiusMin() { return getAs("accuracyradiusmin", AspectPropertyValue.class, Integer.class); }
	/**
	 * The area the IP range is likely to be in, trimmed to the land of the country the location is in, so it does not reach into neighbouring countries or over the sea. Returned as a WKT string stored as a reduced format of WKB.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">More information</a>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<WktString> getAreas() { return getAs("areas", AspectPropertyValue.class, WktString.class); }
	/**
	 * Autonomous System Number associated with the IP address.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Autonomous_system_(Internet)">More information</a>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getAsn() { return getAs("asn", AspectPropertyValue.class, String.class); }
	/**
	 * The name registered to the Asn associated with the IP address.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Autonomous_system_(Internet)">More information</a>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getAsnName() { return getAs("asnname", AspectPropertyValue.class, String.class); }
	/**
	 * Refers to the diversity of browsers observed from the IP range. An integer between 1-10, a lower value indicates a low number of browsers seen per IP address, while a higher value indicates a high number of browsers seen per IP address. A 0 value indicates that the probability is unknown.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Integer> getBrowserDiversity() { return getAs("browserdiversity", AspectPropertyValue.class, Integer.class); }
	/**
	 * Indicates the type of connection being used. Returns either Broadband, Cellular, or Hosting and Anonymous.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getConnectionType() { return getAs("connectiontype", AspectPropertyValue.class, String.class); }
	/**
	 * The 2-character ISO 3166-1 continent code for the supplied location.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getContinentCode2() { return getAs("continentcode2", AspectPropertyValue.class, String.class); }
	/**
	 * The GeoNames identifier of the continent associated with the supplied location.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getContinentGeoNameId() { return getAs("continentgeonameid", AspectPropertyValue.class, String.class); }
	/**
	 * The name of the continent the supplied location is in.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getContinentName() { return getAs("continentname", AspectPropertyValue.class, String.class); }
	/**
	 * The name of the country that the supplied location is in.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getCountry() { return getAs("country", AspectPropertyValue.class, String.class); }
	/**
	 * The 2-character ISO 3166-1 code of the country that the supplied location is in.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getCountryCode() { return getAs("countrycode", AspectPropertyValue.class, String.class); }
	/**
	 * The 3-character ISO 3166-1 alpha-3 code of the country that the supplied location is in.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getCountryCode3() { return getAs("countrycode3", AspectPropertyValue.class, String.class); }
	/**
	 * A list of countries in ISO 3166-1 alpha-2 country code format that overlap with the area likely associated with the provided evidence, weighted and ordered by each country's proportion of the area. Administrative areas that cannot be cleanly resolved to the country level will not contribute to the resulting data.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<List<IWeightedValue<String>>> getCountryCodesGeographical() { return getAs("countrycodesgeographical", AspectPropertyValue.class, List.class, IWeightedValue.class, String.class); }
	/**
	 * A list of countries in ISO 3166-1 alpha-2 country code format that overlap with the area likely associated with the provided evidence, weighted and ordered by each country's proportion of the total population within the area. Administrative areas that cannot be cleanly resolved to the country level will not contribute to the resulting data.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<List<IWeightedValue<String>>> getCountryCodesPopulation() { return getAs("countrycodespopulation", AspectPropertyValue.class, List.class, IWeightedValue.class, String.class); }
	/**
	 * The GeoNames identifier of the country associated with the supplied location.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getCountryGeoNameId() { return getAs("countrygeonameid", AspectPropertyValue.class, String.class); }
	/**
	 * The name of the county that the supplied location is in. In this case, a county is defined as an administrative sub-section of a country or state.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getCounty() { return getAs("county", AspectPropertyValue.class, String.class); }
	/**
	 * The GeoNames identifier of the county, a second-level administrative subdivision (admin2) associated with the supplied location. Unknown where the nearest subdivision lies in a different country from the location.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getCountyGeoNameId() { return getAs("countygeonameid", AspectPropertyValue.class, String.class); }
	/**
	 * The Alpha-3 ISO 4217 code of the currency associated with the supplied location.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getCurrencyCode() { return getAs("currencycode", AspectPropertyValue.class, String.class); }
	/**
	 * ITU international telephone numbering plan code for the country.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getDialCode() { return getAs("dialcode", AspectPropertyValue.class, String.class); }
	/**
	 * Refers to the diversity of hardware devices observed from the IP range. An integer between 1-10, a lower value indicates a low number of devices seen per IP address, while a higher value indicates a high number of devices seen per IP address. A 0 value indicates that the probability is unknown.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Integer> getHardwareDiversity() { return getAs("hardwarediversity", AspectPropertyValue.class, Integer.class); }
	/**
	 * The confidence that the IP address is a human user versus associated with hosting. A 0-10 value where; 0-3: Low confidence the user is human, 4-6: Medium confidence, 7-10: High confidence. A -1 value indicates that the probability is unknown.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Integer> getHumanProbability() { return getAs("humanprobability", AspectPropertyValue.class, Integer.class); }
	/**
	 * The IPv4 address of the request.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<InetAddress> getIp() { return getAs("ip", AspectPropertyValue.class, InetAddress.class); }
	/**
	 * End of the IP range to which the evidence IP belongs.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<InetAddress> getIpRangeEnd() { return getAs("iprangeend", AspectPropertyValue.class, InetAddress.class); }
	/**
	 * Start of the IP range to which the evidence IP belongs.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<InetAddress> getIpRangeStart() { return getAs("iprangestart", AspectPropertyValue.class, InetAddress.class); }
	/**
	 * The IPv6 address of the request.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<InetAddress> getIpV6() { return getAs("ipv6", AspectPropertyValue.class, InetAddress.class); }
	/**
	 * Indicates whether the IP address is associated with a broadband connection. Includes DSL, Cable, Fibre, and Satellite connections.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Boolean> getIsBroadband() { return getAs("isbroadband", AspectPropertyValue.class, Boolean.class); }
	/**
	 * Indicates whether the IP address is associated with a cellular network.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Boolean> getIsCellular() { return getAs("iscellular", AspectPropertyValue.class, Boolean.class); }
	/**
	 * Indicates whether the country of the supplied location is within the European Union.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Boolean> getIsEu() { return getAs("iseu", AspectPropertyValue.class, Boolean.class); }
	/**
	 * Indicates whether the IP address is associated with hosting. Includes both hosting and anonymised connections such as hosting networks, hosting ASNs, VPNs, proxies, TOR networks, and unreachable IP addresses.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Boolean> getIsHosted() { return getAs("ishosted", AspectPropertyValue.class, Boolean.class); }
	/**
	 * The ISO 3166-2 code for the supplied location. This is using the 'ISO3166-2-lvl4' property from OpenStreetMap.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getIso31662Lvl4() { return getAs("iso31662lvl4", AspectPropertyValue.class, String.class); }
	/**
	 * The alphanumeric code representing the subdivision from the ISO 3166-2 code of the supplied location. This is using the 'ISO3166-2-lvl4' property from OpenStreetMap.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getIso31662Lvl4SubdivisionOnly() { return getAs("iso31662lvl4subdivisiononly", AspectPropertyValue.class, String.class); }
	/**
	 * The ISO 3166-2 code for the supplied location. This is using the 'ISO3166-2-lvl8' property from OpenStreetMap.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getIso31662Lvl8() { return getAs("iso31662lvl8", AspectPropertyValue.class, String.class); }
	/**
	 * The alphanumeric code representing the subdivision from the ISO 3166-2 code of the supplied location. This is using the 'ISO3166-2-lvl8' property from OpenStreetMap.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getIso31662Lvl8SubdivisionOnly() { return getAs("iso31662lvl8subdivisiononly", AspectPropertyValue.class, String.class); }
	/**
	 * Indicates whether the IP address is associated with a Proxy server.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Boolean> getIsProxy() { return getAs("isproxy", AspectPropertyValue.class, Boolean.class); }
	/**
	 * Indicates whether the IP address is associated with a public router.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Boolean> getIsPublicRouter() { return getAs("ispublicrouter", AspectPropertyValue.class, Boolean.class); }
	/**
	 * Indicates whether the IP address is associated with a TOR server.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Boolean> getIsTor() { return getAs("istor", AspectPropertyValue.class, Boolean.class); }
	/**
	 * Indicates whether the IP address is associated with a VPN server.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Boolean> getIsVPN() { return getAs("isvpn", AspectPropertyValue.class, Boolean.class); }
	/**
	 * The Alpha-2 ISO 639 Language code associated with the supplied location.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getLanguageCode() { return getAs("languagecode", AspectPropertyValue.class, String.class); }
	/**
	 * Average latitude of the IP. For privacy, this is randomized within around 1 kilometer of the result. Randomized result will change only once per day.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Float> getLatitude() { return getAs("latitude", AspectPropertyValue.class, Float.class); }
	/**
	 * The confidence in the town and country provided. Returns one of High, Medium, Low, or Unknown. The confidence must be checked before using the returned location values.
	 * <p>
	 * Possible values:
	 * <pre>
	 * High      High confidence results can be relied upon and considered useful for all practical purposes.
	 * Medium    Medium confidence results might be useful where the use case can tolerate a degree of inaccuracy.
	 * Low       Low confidence results should be discarded when the results might be used to make consequential decisions.
	 * Unknown   Unknown confidence results should be treated in the same way as Low and discarded when the results might be used to m...
	 * </pre>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getLocationConfidence() { return getAs("locationconfidence", AspectPropertyValue.class, String.class); }
	/**
	 * Average longitude of the IP. For privacy, this is randomized within around 1 kilometer of the result. Randomized result will change only once per day.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Float> getLongitude() { return getAs("longitude", AspectPropertyValue.class, Float.class); }
	/**
	 * The mobile country code of the network the device is connected to.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<List<IWeightedValue<String>>> getMcc() { return getAs("mcc", AspectPropertyValue.class, List.class, IWeightedValue.class, String.class); }
	/**
	 * Refers to the diversity of software observed from the IP range. An integer between 1-10, a lower value indicates a low number of software seen per IP address, while a higher value indicates a high number of software seen per IP address. A 0 value indicates that the probability is unknown.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Integer> getPlatformDiversity() { return getAs("platformdiversity", AspectPropertyValue.class, Integer.class); }
	/**
	 * The name of the geographical region that the supplied location is in.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getRegion() { return getAs("region", AspectPropertyValue.class, String.class); }
	/**
	 * Country code of the registered range.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getRegisteredCountry() { return getAs("registeredcountry", AspectPropertyValue.class, String.class); }
	/**
	 * The GeoNames identifier of the country in which the IP address range is registered.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getRegisteredCountryGeoNameId() { return getAs("registeredcountrygeonameid", AspectPropertyValue.class, String.class); }
	/**
	 * Name of the IP range. This is usually the owner.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getRegisteredName() { return getAs("registeredname", AspectPropertyValue.class, String.class); }
	/**
	 * Registered owner of the range.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getRegisteredOwner() { return getAs("registeredowner", AspectPropertyValue.class, String.class); }
	/**
	 * The name of the state that the supplied location is in.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getState() { return getAs("state", AspectPropertyValue.class, String.class); }
	/**
	 * The GeoNames database identifier for the state (first-level administrative subdivision, admin1) that the supplied location is in. Unknown where the nearest subdivision lies in a different country from the location.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getStateGeoNameId() { return getAs("stategeonameid", AspectPropertyValue.class, String.class); }
	/**
	 * GeoNames identifiers of the first-level administrative subdivisions (admin1) of the location's country that overlap the area associated with the supplied evidence, weighted and ordered by each subdivision's share of that area. Subdivisions covering less than 1% of the area are omitted, at most the 20 largest are returned, and the weights of those returned are scaled to sum to 100%.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<List<IWeightedValue<String>>> getStateGeoNameIdsGeographical() { return getAs("stategeonameidsgeographical", AspectPropertyValue.class, List.class, IWeightedValue.class, String.class); }
	/**
	 * The name of the suburb that the supplied location is in.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getSuburb() { return getAs("suburb", AspectPropertyValue.class, String.class); }
	/**
	 * The time zone at the supplied location in the IANA Time Zone format.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getTimeZoneIana() { return getAs("timezoneiana", AspectPropertyValue.class, String.class); }
	/**
	 * The offset from UTC in minutes in the supplied location, at the time that the value is produced. A value of -1 means the offset is unknown and is only returned when no location can be determined for the IP address. Real offsets are whole multiples of 15 minutes, so -1 never collides with a genuine value.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<Integer> getTimeZoneOffset() { return getAs("timezoneoffset", AspectPropertyValue.class, Integer.class); }
	/**
	 * The name of the town that the supplied location is in.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getTown() { return getAs("town", AspectPropertyValue.class, String.class); }
	/**
	 * The GeoNames identifier of the town or populated place nearest to the supplied location. Unknown where the nearest populated place lies in a different country from the location.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getTownGeoNameId() { return getAs("towngeonameid", AspectPropertyValue.class, String.class); }
	/**
	 * The zip or postal code that the supplied location falls under.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AspectPropertyValue<String> getZipCode() { return getAs("zipcode", AspectPropertyValue.class, String.class); }
}
