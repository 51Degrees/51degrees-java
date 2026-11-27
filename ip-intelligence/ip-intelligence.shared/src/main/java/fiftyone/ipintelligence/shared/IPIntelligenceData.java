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
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import java.util.List;
// This interface sits at the top of the name space in order to make
// life easier for consumers.
/**
 * Interface exposing typed accessors for properties related to an IP.
 * This includes the network, and location.
 */
public interface IPIntelligenceData extends AspectData
{
	/**
	 * Radius in kilometers of the circle centred around the most probable location that encompasses the entire area, measured against the trimmed area. Where multiple areas are returned, this will only cover the area the most probable location is in. See Areas property. This will likely be a very large distance. It is recommended to use the AccuracyRadiusMin property.
	 *
	 * @return the value of the AccuracyRadiusMax property
	 */
	AspectPropertyValue<Integer> getAccuracyRadiusMax();
	/**
	 * Radius in kilometers of the largest circle centred around the most probable location that fits within the area, measured against the trimmed area. Where multiple areas are returned, only the area that the most probable location falls within is considered. See Areas property.
	 *
	 * @return the value of the AccuracyRadiusMin property
	 */
	AspectPropertyValue<Integer> getAccuracyRadiusMin();
	/**
	 * The area the IP range is likely to be in, trimmed to the land of the country the location is in, so it does not reach into neighbouring countries or over the sea. Returned as a WKT string stored as a reduced format of WKB.
	 *
	 * @return the value of the Areas property
	 * @see <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">More information</a>
	 */
	AspectPropertyValue<WktString> getAreas();
	/**
	 * Autonomous System Number associated with the IP address.
	 *
	 * @return the value of the Asn property
	 * @see <a href="https://en.wikipedia.org/wiki/Autonomous_system_(Internet)">More information</a>
	 */
	AspectPropertyValue<String> getAsn();
	/**
	 * The name registered to the Asn associated with the IP address.
	 *
	 * @return the value of the AsnName property
	 * @see <a href="https://en.wikipedia.org/wiki/Autonomous_system_(Internet)">More information</a>
	 */
	AspectPropertyValue<String> getAsnName();
	/**
	 * Refers to the diversity of browsers observed from the IP range. An integer between 1-10, a lower value indicates a low number of browsers seen per IP address, while a higher value indicates a high number of browsers seen per IP address. A 0 value indicates that the probability is unknown.
	 *
	 * @return the value of the BrowserDiversity property
	 */
	AspectPropertyValue<Integer> getBrowserDiversity();
	/**
	 * Indicates the type of connection being used. Returns either Broadband, Cellular, or Hosting and Anonymous.
	 *
	 * @return the value of the ConnectionType property
	 */
	AspectPropertyValue<String> getConnectionType();
	/**
	 * The 2-character ISO 3166-1 continent code for the supplied location.
	 *
	 * @return the value of the ContinentCode2 property
	 */
	AspectPropertyValue<String> getContinentCode2();
	/**
	 * The GeoNames identifier of the continent associated with the supplied location.
	 *
	 * @return the value of the ContinentGeoNameId property
	 */
	AspectPropertyValue<String> getContinentGeoNameId();
	/**
	 * The name of the continent the supplied location is in.
	 *
	 * @return the value of the ContinentName property
	 */
	AspectPropertyValue<String> getContinentName();
	/**
	 * The name of the country that the supplied location is in.
	 *
	 * @return the value of the Country property
	 */
	AspectPropertyValue<String> getCountry();
	/**
	 * The 2-character ISO 3166-1 code of the country that the supplied location is in.
	 *
	 * @return the value of the CountryCode property
	 */
	AspectPropertyValue<String> getCountryCode();
	/**
	 * The 3-character ISO 3166-1 alpha-3 code of the country that the supplied location is in.
	 *
	 * @return the value of the CountryCode3 property
	 */
	AspectPropertyValue<String> getCountryCode3();
	/**
	 * A list of countries in ISO 3166-1 alpha-2 country code format that overlap with the area likely associated with the provided evidence, weighted and ordered by each country's proportion of the area. Administrative areas that cannot be cleanly resolved to the country level will not contribute to the resulting data.
	 *
	 * @return the value of the CountryCodesGeographical property
	 */
	AspectPropertyValue<List<IWeightedValue<String>>> getCountryCodesGeographical();
	/**
	 * A list of countries in ISO 3166-1 alpha-2 country code format that overlap with the area likely associated with the provided evidence, weighted and ordered by each country's proportion of the total population within the area. Administrative areas that cannot be cleanly resolved to the country level will not contribute to the resulting data.
	 *
	 * @return the value of the CountryCodesPopulation property
	 */
	AspectPropertyValue<List<IWeightedValue<String>>> getCountryCodesPopulation();
	/**
	 * The GeoNames identifier of the country associated with the supplied location.
	 *
	 * @return the value of the CountryGeoNameId property
	 */
	AspectPropertyValue<String> getCountryGeoNameId();
	/**
	 * The name of the county that the supplied location is in. In this case, a county is defined as an administrative sub-section of a country or state.
	 *
	 * @return the value of the County property
	 */
	AspectPropertyValue<String> getCounty();
	/**
	 * The GeoNames identifier of the county, a second-level administrative subdivision (admin2) associated with the supplied location. Unknown where the nearest subdivision lies in a different country from the location.
	 *
	 * @return the value of the CountyGeoNameId property
	 */
	AspectPropertyValue<String> getCountyGeoNameId();
	/**
	 * The Alpha-3 ISO 4217 code of the currency associated with the supplied location.
	 *
	 * @return the value of the CurrencyCode property
	 */
	AspectPropertyValue<String> getCurrencyCode();
	/**
	 * ITU international telephone numbering plan code for the country.
	 *
	 * @return the value of the DialCode property
	 */
	AspectPropertyValue<String> getDialCode();
	/**
	 * Refers to the diversity of hardware devices observed from the IP range. An integer between 1-10, a lower value indicates a low number of devices seen per IP address, while a higher value indicates a high number of devices seen per IP address. A 0 value indicates that the probability is unknown.
	 *
	 * @return the value of the HardwareDiversity property
	 */
	AspectPropertyValue<Integer> getHardwareDiversity();
	/**
	 * The confidence that the IP address is a human user versus associated with hosting. A 0-10 value where; 0-3: Low confidence the user is human, 4-6: Medium confidence, 7-10: High confidence. A -1 value indicates that the probability is unknown.
	 *
	 * @return the value of the HumanProbability property
	 */
	AspectPropertyValue<Integer> getHumanProbability();
	/**
	 * The IPv4 address of the request.
	 *
	 * @return the value of the Ip property
	 */
	AspectPropertyValue<InetAddress> getIp();
	/**
	 * End of the IP range to which the evidence IP belongs.
	 *
	 * @return the value of the IpRangeEnd property
	 */
	AspectPropertyValue<InetAddress> getIpRangeEnd();
	/**
	 * Start of the IP range to which the evidence IP belongs.
	 *
	 * @return the value of the IpRangeStart property
	 */
	AspectPropertyValue<InetAddress> getIpRangeStart();
	/**
	 * The IPv6 address of the request.
	 *
	 * @return the value of the IpV6 property
	 */
	AspectPropertyValue<InetAddress> getIpV6();
	/**
	 * Indicates whether the IP address is associated with a broadband connection. Includes DSL, Cable, Fibre, and Satellite connections.
	 *
	 * @return the value of the IsBroadband property
	 */
	AspectPropertyValue<Boolean> getIsBroadband();
	/**
	 * Indicates whether the IP address is associated with a cellular network.
	 *
	 * @return the value of the IsCellular property
	 */
	AspectPropertyValue<Boolean> getIsCellular();
	/**
	 * Indicates whether the country of the supplied location is within the European Union.
	 *
	 * @return the value of the IsEu property
	 */
	AspectPropertyValue<Boolean> getIsEu();
	/**
	 * Indicates whether the IP address is associated with hosting. Includes both hosting and anonymised connections such as hosting networks, hosting ASNs, VPNs, proxies, TOR networks, and unreachable IP addresses.
	 *
	 * @return the value of the IsHosted property
	 */
	AspectPropertyValue<Boolean> getIsHosted();
	/**
	 * The ISO 3166-2 code for the supplied location. This is using the 'ISO3166-2-lvl4' property from OpenStreetMap.
	 *
	 * @return the value of the Iso31662Lvl4 property
	 */
	AspectPropertyValue<String> getIso31662Lvl4();
	/**
	 * The alphanumeric code representing the subdivision from the ISO 3166-2 code of the supplied location. This is using the 'ISO3166-2-lvl4' property from OpenStreetMap.
	 *
	 * @return the value of the Iso31662Lvl4SubdivisionOnly property
	 */
	AspectPropertyValue<String> getIso31662Lvl4SubdivisionOnly();
	/**
	 * The ISO 3166-2 code for the supplied location. This is using the 'ISO3166-2-lvl8' property from OpenStreetMap.
	 *
	 * @return the value of the Iso31662Lvl8 property
	 */
	AspectPropertyValue<String> getIso31662Lvl8();
	/**
	 * The alphanumeric code representing the subdivision from the ISO 3166-2 code of the supplied location. This is using the 'ISO3166-2-lvl8' property from OpenStreetMap.
	 *
	 * @return the value of the Iso31662Lvl8SubdivisionOnly property
	 */
	AspectPropertyValue<String> getIso31662Lvl8SubdivisionOnly();
	/**
	 * Indicates whether the IP address is associated with a Proxy server.
	 *
	 * @return the value of the IsProxy property
	 */
	AspectPropertyValue<Boolean> getIsProxy();
	/**
	 * Indicates whether the IP address is associated with a public router.
	 *
	 * @return the value of the IsPublicRouter property
	 */
	AspectPropertyValue<Boolean> getIsPublicRouter();
	/**
	 * Indicates whether the IP address is associated with a TOR server.
	 *
	 * @return the value of the IsTor property
	 */
	AspectPropertyValue<Boolean> getIsTor();
	/**
	 * Indicates whether the IP address is associated with a VPN server.
	 *
	 * @return the value of the IsVPN property
	 */
	AspectPropertyValue<Boolean> getIsVPN();
	/**
	 * The Alpha-2 ISO 639 Language code associated with the supplied location.
	 *
	 * @return the value of the LanguageCode property
	 */
	AspectPropertyValue<String> getLanguageCode();
	/**
	 * Average latitude of the IP. For privacy, this is randomized within around 1 kilometer of the result. Randomized result will change only once per day.
	 *
	 * @return the value of the Latitude property
	 */
	AspectPropertyValue<Float> getLatitude();
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
	AspectPropertyValue<String> getLocationConfidence();
	/**
	 * Average longitude of the IP. For privacy, this is randomized within around 1 kilometer of the result. Randomized result will change only once per day.
	 *
	 * @return the value of the Longitude property
	 */
	AspectPropertyValue<Float> getLongitude();
	/**
	 * The mobile country code of the network the device is connected to.
	 *
	 * @return the value of the Mcc property
	 */
	AspectPropertyValue<List<IWeightedValue<String>>> getMcc();
	/**
	 * Refers to the diversity of software observed from the IP range. An integer between 1-10, a lower value indicates a low number of software seen per IP address, while a higher value indicates a high number of software seen per IP address. A 0 value indicates that the probability is unknown.
	 *
	 * @return the value of the PlatformDiversity property
	 */
	AspectPropertyValue<Integer> getPlatformDiversity();
	/**
	 * The name of the geographical region that the supplied location is in.
	 *
	 * @return the value of the Region property
	 */
	AspectPropertyValue<String> getRegion();
	/**
	 * Country code of the registered range.
	 *
	 * @return the value of the RegisteredCountry property
	 */
	AspectPropertyValue<String> getRegisteredCountry();
	/**
	 * The GeoNames identifier of the country in which the IP address range is registered.
	 *
	 * @return the value of the RegisteredCountryGeoNameId property
	 */
	AspectPropertyValue<String> getRegisteredCountryGeoNameId();
	/**
	 * Name of the IP range. This is usually the owner.
	 *
	 * @return the value of the RegisteredName property
	 */
	AspectPropertyValue<String> getRegisteredName();
	/**
	 * Registered owner of the range.
	 *
	 * @return the value of the RegisteredOwner property
	 */
	AspectPropertyValue<String> getRegisteredOwner();
	/**
	 * The name of the state that the supplied location is in.
	 *
	 * @return the value of the State property
	 */
	AspectPropertyValue<String> getState();
	/**
	 * The GeoNames database identifier for the state (first-level administrative subdivision, admin1) that the supplied location is in. Unknown where the nearest subdivision lies in a different country from the location.
	 *
	 * @return the value of the StateGeoNameId property
	 */
	AspectPropertyValue<String> getStateGeoNameId();
	/**
	 * GeoNames identifiers of the first-level administrative subdivisions (admin1) of the location's country that overlap the area associated with the supplied evidence, weighted and ordered by each subdivision's share of that area. Subdivisions covering less than 1% of the area are omitted, at most the 20 largest are returned, and the weights of those returned are scaled to sum to 100%.
	 *
	 * @return the value of the StateGeoNameIdsGeographical property
	 */
	AspectPropertyValue<List<IWeightedValue<String>>> getStateGeoNameIdsGeographical();
	/**
	 * The name of the suburb that the supplied location is in.
	 *
	 * @return the value of the Suburb property
	 */
	AspectPropertyValue<String> getSuburb();
	/**
	 * The time zone at the supplied location in the IANA Time Zone format.
	 *
	 * @return the value of the TimeZoneIana property
	 */
	AspectPropertyValue<String> getTimeZoneIana();
	/**
	 * The offset from UTC in minutes in the supplied location, at the time that the value is produced. A value of -1 means the offset is unknown and is only returned when no location can be determined for the IP address. Real offsets are whole multiples of 15 minutes, so -1 never collides with a genuine value.
	 *
	 * @return the value of the TimeZoneOffset property
	 */
	AspectPropertyValue<Integer> getTimeZoneOffset();
	/**
	 * The name of the town that the supplied location is in.
	 *
	 * @return the value of the Town property
	 */
	AspectPropertyValue<String> getTown();
	/**
	 * The GeoNames identifier of the town or populated place nearest to the supplied location. Unknown where the nearest populated place lies in a different country from the location.
	 *
	 * @return the value of the TownGeoNameId property
	 */
	AspectPropertyValue<String> getTownGeoNameId();
	/**
	 * The zip or postal code that the supplied location falls under.
	 *
	 * @return the value of the ZipCode property
	 */
	AspectPropertyValue<String> getZipCode();
}
