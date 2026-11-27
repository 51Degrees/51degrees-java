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
/**
 * Abstract base class for properties relating to an IP.
 * This includes the network, and location.
 */
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
	 *
	 * @return the value of the AccuracyRadiusMax property
	 */
	@Override
	public AspectPropertyValue<Integer> getAccuracyRadiusMax() { return getValue("accuracyradiusmax", Integer.class); }
	/**
	 * Radius in kilometers of the largest circle centred around the most probable location that fits within the area, measured against the trimmed area. Where multiple areas are returned, only the area that the most probable location falls within is considered. See Areas property.
	 *
	 * @return the value of the AccuracyRadiusMin property
	 */
	@Override
	public AspectPropertyValue<Integer> getAccuracyRadiusMin() { return getValue("accuracyradiusmin", Integer.class); }
	/**
	 * The area the IP range is likely to be in, trimmed to the land of the country the location is in, so it does not reach into neighbouring countries or over the sea. Returned as a WKT string stored as a reduced format of WKB.
	 *
	 * @return the value of the Areas property
	 * @see <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">More information</a>
	 */
	@Override
	public AspectPropertyValue<WktString> getAreas() { return getValue("areas", WktString.class); }
	/**
	 * Autonomous System Number associated with the IP address.
	 *
	 * @return the value of the Asn property
	 * @see <a href="https://en.wikipedia.org/wiki/Autonomous_system_(Internet)">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getAsn() { return getValue("asn", String.class); }
	/**
	 * The name registered to the Asn associated with the IP address.
	 *
	 * @return the value of the AsnName property
	 * @see <a href="https://en.wikipedia.org/wiki/Autonomous_system_(Internet)">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getAsnName() { return getValue("asnname", String.class); }
	/**
	 * Refers to the diversity of browsers observed from the IP range. An integer between 1-10, a lower value indicates a low number of browsers seen per IP address, while a higher value indicates a high number of browsers seen per IP address. A 0 value indicates that the probability is unknown.
	 *
	 * @return the value of the BrowserDiversity property
	 */
	@Override
	public AspectPropertyValue<Integer> getBrowserDiversity() { return getValue("browserdiversity", Integer.class); }
	/**
	 * Indicates the type of connection being used. Returns either Broadband, Cellular, or Hosting and Anonymous.
	 *
	 * @return the value of the ConnectionType property
	 */
	@Override
	public AspectPropertyValue<String> getConnectionType() { return getValue("connectiontype", String.class); }
	/**
	 * The 2-character ISO 3166-1 continent code for the supplied location.
	 *
	 * @return the value of the ContinentCode2 property
	 */
	@Override
	public AspectPropertyValue<String> getContinentCode2() { return getValue("continentcode2", String.class); }
	/**
	 * The GeoNames identifier of the continent associated with the supplied location.
	 *
	 * @return the value of the ContinentGeoNameId property
	 */
	@Override
	public AspectPropertyValue<String> getContinentGeoNameId() { return getValue("continentgeonameid", String.class); }
	/**
	 * The name of the continent the supplied location is in.
	 *
	 * @return the value of the ContinentName property
	 */
	@Override
	public AspectPropertyValue<String> getContinentName() { return getValue("continentname", String.class); }
	/**
	 * The name of the country that the supplied location is in.
	 *
	 * @return the value of the Country property
	 */
	@Override
	public AspectPropertyValue<String> getCountry() { return getValue("country", String.class); }
	/**
	 * The 2-character ISO 3166-1 code of the country that the supplied location is in.
	 *
	 * @return the value of the CountryCode property
	 */
	@Override
	public AspectPropertyValue<String> getCountryCode() { return getValue("countrycode", String.class); }
	/**
	 * The 3-character ISO 3166-1 alpha-3 code of the country that the supplied location is in.
	 *
	 * @return the value of the CountryCode3 property
	 */
	@Override
	public AspectPropertyValue<String> getCountryCode3() { return getValue("countrycode3", String.class); }
	/**
	 * A list of countries in ISO 3166-1 alpha-2 country code format that overlap with the area likely associated with the provided evidence, weighted and ordered by each country's proportion of the area. Administrative areas that cannot be cleanly resolved to the country level will not contribute to the resulting data.
	 *
	 * @return the value of the CountryCodesGeographical property
	 */
	@Override
	public AspectPropertyValue<List<IWeightedValue<String>>> getCountryCodesGeographical() { return getWeightedList("countrycodesgeographical", String.class); }
	/**
	 * A list of countries in ISO 3166-1 alpha-2 country code format that overlap with the area likely associated with the provided evidence, weighted and ordered by each country's proportion of the total population within the area. Administrative areas that cannot be cleanly resolved to the country level will not contribute to the resulting data.
	 *
	 * @return the value of the CountryCodesPopulation property
	 */
	@Override
	public AspectPropertyValue<List<IWeightedValue<String>>> getCountryCodesPopulation() { return getWeightedList("countrycodespopulation", String.class); }
	/**
	 * The GeoNames identifier of the country associated with the supplied location.
	 *
	 * @return the value of the CountryGeoNameId property
	 */
	@Override
	public AspectPropertyValue<String> getCountryGeoNameId() { return getValue("countrygeonameid", String.class); }
	/**
	 * The name of the county that the supplied location is in. In this case, a county is defined as an administrative sub-section of a country or state.
	 *
	 * @return the value of the County property
	 */
	@Override
	public AspectPropertyValue<String> getCounty() { return getValue("county", String.class); }
	/**
	 * The GeoNames identifier of the county, a second-level administrative subdivision (admin2) associated with the supplied location. Unknown where the nearest subdivision lies in a different country from the location.
	 *
	 * @return the value of the CountyGeoNameId property
	 */
	@Override
	public AspectPropertyValue<String> getCountyGeoNameId() { return getValue("countygeonameid", String.class); }
	/**
	 * The Alpha-3 ISO 4217 code of the currency associated with the supplied location.
	 *
	 * @return the value of the CurrencyCode property
	 */
	@Override
	public AspectPropertyValue<String> getCurrencyCode() { return getValue("currencycode", String.class); }
	/**
	 * ITU international telephone numbering plan code for the country.
	 *
	 * @return the value of the DialCode property
	 */
	@Override
	public AspectPropertyValue<String> getDialCode() { return getValue("dialcode", String.class); }
	/**
	 * Refers to the diversity of hardware devices observed from the IP range. An integer between 1-10, a lower value indicates a low number of devices seen per IP address, while a higher value indicates a high number of devices seen per IP address. A 0 value indicates that the probability is unknown.
	 *
	 * @return the value of the HardwareDiversity property
	 */
	@Override
	public AspectPropertyValue<Integer> getHardwareDiversity() { return getValue("hardwarediversity", Integer.class); }
	/**
	 * The confidence that the IP address is a human user versus associated with hosting. A 0-10 value where; 0-3: Low confidence the user is human, 4-6: Medium confidence, 7-10: High confidence. A -1 value indicates that the probability is unknown.
	 *
	 * @return the value of the HumanProbability property
	 */
	@Override
	public AspectPropertyValue<Integer> getHumanProbability() { return getValue("humanprobability", Integer.class); }
	/**
	 * The IPv4 address of the request.
	 *
	 * @return the value of the Ip property
	 */
	@Override
	public AspectPropertyValue<InetAddress> getIp() { return getValue("ip", InetAddress.class); }
	/**
	 * End of the IP range to which the evidence IP belongs.
	 *
	 * @return the value of the IpRangeEnd property
	 */
	@Override
	public AspectPropertyValue<InetAddress> getIpRangeEnd() { return getValue("iprangeend", InetAddress.class); }
	/**
	 * Start of the IP range to which the evidence IP belongs.
	 *
	 * @return the value of the IpRangeStart property
	 */
	@Override
	public AspectPropertyValue<InetAddress> getIpRangeStart() { return getValue("iprangestart", InetAddress.class); }
	/**
	 * The IPv6 address of the request.
	 *
	 * @return the value of the IpV6 property
	 */
	@Override
	public AspectPropertyValue<InetAddress> getIpV6() { return getValue("ipv6", InetAddress.class); }
	/**
	 * Indicates whether the IP address is associated with a broadband connection. Includes DSL, Cable, Fibre, and Satellite connections.
	 *
	 * @return the value of the IsBroadband property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsBroadband() { return getValue("isbroadband", Boolean.class); }
	/**
	 * Indicates whether the IP address is associated with a cellular network.
	 *
	 * @return the value of the IsCellular property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsCellular() { return getValue("iscellular", Boolean.class); }
	/**
	 * Indicates whether the country of the supplied location is within the European Union.
	 *
	 * @return the value of the IsEu property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsEu() { return getValue("iseu", Boolean.class); }
	/**
	 * Indicates whether the IP address is associated with hosting. Includes both hosting and anonymised connections such as hosting networks, hosting ASNs, VPNs, proxies, TOR networks, and unreachable IP addresses.
	 *
	 * @return the value of the IsHosted property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsHosted() { return getValue("ishosted", Boolean.class); }
	/**
	 * The ISO 3166-2 code for the supplied location. This is using the 'ISO3166-2-lvl4' property from OpenStreetMap.
	 *
	 * @return the value of the Iso31662Lvl4 property
	 */
	@Override
	public AspectPropertyValue<String> getIso31662Lvl4() { return getValue("iso31662lvl4", String.class); }
	/**
	 * The alphanumeric code representing the subdivision from the ISO 3166-2 code of the supplied location. This is using the 'ISO3166-2-lvl4' property from OpenStreetMap.
	 *
	 * @return the value of the Iso31662Lvl4SubdivisionOnly property
	 */
	@Override
	public AspectPropertyValue<String> getIso31662Lvl4SubdivisionOnly() { return getValue("iso31662lvl4subdivisiononly", String.class); }
	/**
	 * The ISO 3166-2 code for the supplied location. This is using the 'ISO3166-2-lvl8' property from OpenStreetMap.
	 *
	 * @return the value of the Iso31662Lvl8 property
	 */
	@Override
	public AspectPropertyValue<String> getIso31662Lvl8() { return getValue("iso31662lvl8", String.class); }
	/**
	 * The alphanumeric code representing the subdivision from the ISO 3166-2 code of the supplied location. This is using the 'ISO3166-2-lvl8' property from OpenStreetMap.
	 *
	 * @return the value of the Iso31662Lvl8SubdivisionOnly property
	 */
	@Override
	public AspectPropertyValue<String> getIso31662Lvl8SubdivisionOnly() { return getValue("iso31662lvl8subdivisiononly", String.class); }
	/**
	 * Indicates whether the IP address is associated with a Proxy server.
	 *
	 * @return the value of the IsProxy property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsProxy() { return getValue("isproxy", Boolean.class); }
	/**
	 * Indicates whether the IP address is associated with a public router.
	 *
	 * @return the value of the IsPublicRouter property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsPublicRouter() { return getValue("ispublicrouter", Boolean.class); }
	/**
	 * Indicates whether the IP address is associated with a TOR server.
	 *
	 * @return the value of the IsTor property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsTor() { return getValue("istor", Boolean.class); }
	/**
	 * Indicates whether the IP address is associated with a VPN server.
	 *
	 * @return the value of the IsVPN property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsVPN() { return getValue("isvpn", Boolean.class); }
	/**
	 * The Alpha-2 ISO 639 Language code associated with the supplied location.
	 *
	 * @return the value of the LanguageCode property
	 */
	@Override
	public AspectPropertyValue<String> getLanguageCode() { return getValue("languagecode", String.class); }
	/**
	 * Average latitude of the IP. For privacy, this is randomized within around 1 kilometer of the result. Randomized result will change only once per day.
	 *
	 * @return the value of the Latitude property
	 */
	@Override
	public AspectPropertyValue<Float> getLatitude() { return getValue("latitude", Float.class); }
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
	 *
	 * @return the value of the LocationConfidence property
	 */
	@Override
	public AspectPropertyValue<String> getLocationConfidence() { return getValue("locationconfidence", String.class); }
	/**
	 * Average longitude of the IP. For privacy, this is randomized within around 1 kilometer of the result. Randomized result will change only once per day.
	 *
	 * @return the value of the Longitude property
	 */
	@Override
	public AspectPropertyValue<Float> getLongitude() { return getValue("longitude", Float.class); }
	/**
	 * The mobile country code of the network the device is connected to.
	 *
	 * @return the value of the Mcc property
	 */
	@Override
	public AspectPropertyValue<List<IWeightedValue<String>>> getMcc() { return getWeightedList("mcc", String.class); }
	/**
	 * Refers to the diversity of software observed from the IP range. An integer between 1-10, a lower value indicates a low number of software seen per IP address, while a higher value indicates a high number of software seen per IP address. A 0 value indicates that the probability is unknown.
	 *
	 * @return the value of the PlatformDiversity property
	 */
	@Override
	public AspectPropertyValue<Integer> getPlatformDiversity() { return getValue("platformdiversity", Integer.class); }
	/**
	 * The name of the geographical region that the supplied location is in.
	 *
	 * @return the value of the Region property
	 */
	@Override
	public AspectPropertyValue<String> getRegion() { return getValue("region", String.class); }
	/**
	 * Country code of the registered range.
	 *
	 * @return the value of the RegisteredCountry property
	 */
	@Override
	public AspectPropertyValue<String> getRegisteredCountry() { return getValue("registeredcountry", String.class); }
	/**
	 * The GeoNames identifier of the country in which the IP address range is registered.
	 *
	 * @return the value of the RegisteredCountryGeoNameId property
	 */
	@Override
	public AspectPropertyValue<String> getRegisteredCountryGeoNameId() { return getValue("registeredcountrygeonameid", String.class); }
	/**
	 * Name of the IP range. This is usually the owner.
	 *
	 * @return the value of the RegisteredName property
	 */
	@Override
	public AspectPropertyValue<String> getRegisteredName() { return getValue("registeredname", String.class); }
	/**
	 * Registered owner of the range.
	 *
	 * @return the value of the RegisteredOwner property
	 */
	@Override
	public AspectPropertyValue<String> getRegisteredOwner() { return getValue("registeredowner", String.class); }
	/**
	 * The name of the state that the supplied location is in.
	 *
	 * @return the value of the State property
	 */
	@Override
	public AspectPropertyValue<String> getState() { return getValue("state", String.class); }
	/**
	 * The GeoNames database identifier for the state (first-level administrative subdivision, admin1) that the supplied location is in. Unknown where the nearest subdivision lies in a different country from the location.
	 *
	 * @return the value of the StateGeoNameId property
	 */
	@Override
	public AspectPropertyValue<String> getStateGeoNameId() { return getValue("stategeonameid", String.class); }
	/**
	 * GeoNames identifiers of the first-level administrative subdivisions (admin1) of the location's country that overlap the area associated with the supplied evidence, weighted and ordered by each subdivision's share of that area. Subdivisions covering less than 1% of the area are omitted, at most the 20 largest are returned, and the weights of those returned are scaled to sum to 100%.
	 *
	 * @return the value of the StateGeoNameIdsGeographical property
	 */
	@Override
	public AspectPropertyValue<List<IWeightedValue<String>>> getStateGeoNameIdsGeographical() { return getWeightedList("stategeonameidsgeographical", String.class); }
	/**
	 * The name of the suburb that the supplied location is in.
	 *
	 * @return the value of the Suburb property
	 */
	@Override
	public AspectPropertyValue<String> getSuburb() { return getValue("suburb", String.class); }
	/**
	 * The time zone at the supplied location in the IANA Time Zone format.
	 *
	 * @return the value of the TimeZoneIana property
	 */
	@Override
	public AspectPropertyValue<String> getTimeZoneIana() { return getValue("timezoneiana", String.class); }
	/**
	 * The offset from UTC in minutes in the supplied location, at the time that the value is produced. A value of -1 means the offset is unknown and is only returned when no location can be determined for the IP address. Real offsets are whole multiples of 15 minutes, so -1 never collides with a genuine value.
	 *
	 * @return the value of the TimeZoneOffset property
	 */
	@Override
	public AspectPropertyValue<Integer> getTimeZoneOffset() { return getValue("timezoneoffset", Integer.class); }
	/**
	 * The name of the town that the supplied location is in.
	 *
	 * @return the value of the Town property
	 */
	@Override
	public AspectPropertyValue<String> getTown() { return getValue("town", String.class); }
	/**
	 * The GeoNames identifier of the town or populated place nearest to the supplied location. Unknown where the nearest populated place lies in a different country from the location.
	 *
	 * @return the value of the TownGeoNameId property
	 */
	@Override
	public AspectPropertyValue<String> getTownGeoNameId() { return getValue("towngeonameid", String.class); }
	/**
	 * The zip or postal code that the supplied location falls under.
	 *
	 * @return the value of the ZipCode property
	 */
	@Override
	public AspectPropertyValue<String> getZipCode() { return getValue("zipcode", String.class); }
}
