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
package fiftyone.devicedetection.shared;
import fiftyone.pipeline.core.data.types.JavaScript;
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
 * Abstract base class for properties relating to a device returned
 * by a device detection engine.
 */
public abstract class DeviceDataBase extends AspectDataBase implements DeviceData
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
	protected DeviceDataBase(
		Logger logger,
		FlowData flowData,
		AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine,
		MissingPropertyService missingPropertyService) {
		super(logger, flowData, engine, missingPropertyService);
	}
	/**
	 * Indicates what ajax request format should be used.
	 *
	 * @return the value of the AjaxRequestType property
	 */
	@Override
	public AspectPropertyValue<String> getAjaxRequestType() { return getValue("ajaxrequesttype", String.class); }
	/**
	 * Indicates if the browser supports 'window.requestAnimationFrame()' method.
	 *
	 * @return the value of the AnimationTiming property
	 * @see <a href="http://www.w3.org/TR/animation-timing/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getAnimationTiming() { return getValue("animationtiming", Boolean.class); }
	/**
	 * Indicates the resolution of the device's back camera in megapixels. For a device that has a rotating camera the same value is returned for front and back megapixels properties.
	 *
	 * @return the value of the BackCameraMegaPixels property
	 */
	@Override
	public AspectPropertyValue<Double> getBackCameraMegaPixels() { return getValue("backcameramegapixels", Double.class); }
	/**
	 * Indicates the capacity of the device's standard battery in mAh. This property is not applicable for a device that does not have a battery.
	 *
	 * @return the value of the BatteryCapacity property
	 */
	@Override
	public AspectPropertyValue<Integer> getBatteryCapacity() { return getValue("batterycapacity", Integer.class); }
	/**
	 * Indicates the number of bits used to describe the colour of each individual pixel, also known as bit depth or colour depth.
	 *
	 * @return the value of the BitsPerPixel property
	 */
	@Override
	public AspectPropertyValue<Integer> getBitsPerPixel() { return getValue("bitsperpixel", Integer.class); }
	/**
	 * Indicates if the browser fully supports BlobBuilder, containing a BlobBuilder interface, a FileSaver interface, a FileWriter interface, and a FileWriterSync interface.
	 *
	 * @return the value of the BlobBuilder property
	 * @see <a href="http://dev.w3.org/2009/dap/file-system/file-writer.html#the-blobbuilder-interface">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getBlobBuilder() { return getValue("blobbuilder", Boolean.class); }
	/**
	 * Refers to the list of audio codecs in specific formats supported for Decode by the Web Browser. This list of codecs is supported for playback on a basic browser installation.
	 *
	 * @return the value of the BrowserAudioCodecsDecode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getBrowserAudioCodecsDecode() { return getList("browseraudiocodecsdecode", String.class); }
	/**
	 * Refers to the list of audio codecs in specific formats supported for Encode by the Web Browser. This list of codecs is supported for capture on a basic browser installation.
	 *
	 * @return the value of the BrowserAudioCodecsEncode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getBrowserAudioCodecsEncode() { return getList("browseraudiocodecsencode", String.class); }
	/**
	 * Indicates the age in months of the browser since the BrowserDiscontinuedYear and BrowserDiscontinuedMonth.
	 *
	 * @return the value of the BrowserDiscontinuedAge property
	 */
	@Override
	public AspectPropertyValue<Integer> getBrowserDiscontinuedAge() { return getValue("browserdiscontinuedage", Integer.class); }
	/**
	 * The month in which further development of the browser version is stopped by the browser vendor. This occurs when a new stable version of the browser is released.
	 *
	 * @return the value of the BrowserDiscontinuedMonth property
	 */
	@Override
	public AspectPropertyValue<String> getBrowserDiscontinuedMonth() { return getValue("browserdiscontinuedmonth", String.class); }
	/**
	 * The year in which further development of the browser version is stopped by the browser vendor. This occurs when a new stable version of the browser is released.
	 *
	 * @return the value of the BrowserDiscontinuedYear property
	 */
	@Override
	public AspectPropertyValue<Integer> getBrowserDiscontinuedYear() { return getValue("browserdiscontinuedyear", Integer.class); }
	/**
	 * Indicates the name of the browser without the default OS or layout engine.
	 *
	 * @return the value of the BrowserFamily property
	 */
	@Override
	public AspectPropertyValue<String> getBrowserFamily() { return getValue("browserfamily", String.class); }
	/**
	 * A list of logos associated with the Browser. The string contains the caption, followed by the full image URL separated with a tab character.
	 *
	 * @return the value of the BrowserLogos property
	 */
	@Override
	public AspectPropertyValue<List<String>> getBrowserLogos() { return getList("browserlogos", String.class); }
	/**
	 * Indicates the name of the browser. Many mobile browsers, by default, come with an operating system (OS). Unless specifically named, these browsers are named after the accompanying OS and/or the layout engine. 
	 *
	 * @return the value of the BrowserName property
	 * @see <a href="http://en.wikipedia.org/wiki/Web_browser">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getBrowserName() { return getValue("browsername", String.class); }
	/**
	 * Indicates the age in months of the browser since the BrowserPreviewYear and BrowserPreviewMonth.
	 *
	 * @return the value of the BrowserPreviewAge property
	 */
	@Override
	public AspectPropertyValue<Integer> getBrowserPreviewAge() { return getValue("browserpreviewage", Integer.class); }
	/**
	 * The month in which the browser version is originally released as a Beta/Developer version by the browser vendor. This is before it is officially released as a stable version, to ensure wider testing by the community can take place.
	 *
	 * @return the value of the BrowserPreviewMonth property
	 */
	@Override
	public AspectPropertyValue<String> getBrowserPreviewMonth() { return getValue("browserpreviewmonth", String.class); }
	/**
	 * The year in which the browser version is originally released as a Beta/Developer version by the browser vendor. This is before it is officially released as a stable version, to ensure wider testing by the community can take place.
	 *
	 * @return the value of the BrowserPreviewYear property
	 */
	@Override
	public AspectPropertyValue<Integer> getBrowserPreviewYear() { return getValue("browserpreviewyear", Integer.class); }
	/**
	 * Indicates the source from which browser properties have been validated. Primary browser data are retrieved from the internal test and populated manually, then they might be validated against an external source such as Caniuse or RingMark. 
	 *
	 * @return the value of the BrowserPropertySource property
	 */
	@Override
	public AspectPropertyValue<String> getBrowserPropertySource() { return getValue("browserpropertysource", String.class); }
	/**
	 * A measure of the popularity of this browser version. All browsers are ordered by the number of events associated with that browser that occurred in the sampling period. The browser with the most events is ranked 1, the second 2 and so on.
	 *
	 * @return the value of the BrowserRank property
	 */
	@Override
	public AspectPropertyValue<Integer> getBrowserRank() { return getValue("browserrank", Integer.class); }
	/**
	 * Indicates the age in months of the browser since the BrowserReleaseYear and BrowserReleaseMonth.
	 *
	 * @return the value of the BrowserReleaseAge property
	 */
	@Override
	public AspectPropertyValue<Integer> getBrowserReleaseAge() { return getValue("browserreleaseage", Integer.class); }
	/**
	 * The month in which the browser version is officially released to users by the browser vendor. This version is called the stable version as any bugs or difficulties highlighted in the Beta/Developer Version will have been fixed for this release.
	 *
	 * @return the value of the BrowserReleaseMonth property
	 */
	@Override
	public AspectPropertyValue<String> getBrowserReleaseMonth() { return getValue("browserreleasemonth", String.class); }
	/**
	 * The year in which the browser version is officially released to users by the browser vendor. This version is called the stable version as any bugs or difficulties highlighted in the Beta/Developer Version will have been fixed for this release.
	 *
	 * @return the value of the BrowserReleaseYear property
	 */
	@Override
	public AspectPropertyValue<Integer> getBrowserReleaseYear() { return getValue("browserreleaseyear", Integer.class); }
	/**
	 * Name of the underlying browser source project.
	 *
	 * @return the value of the BrowserSourceProject property
	 */
	@Override
	public AspectPropertyValue<String> getBrowserSourceProject() { return getValue("browsersourceproject", String.class); }
	/**
	 * Indicates the version or subversion of the underlying browser source project.
	 *
	 * @return the value of the BrowserSourceProjectVersion property
	 */
	@Override
	public AspectPropertyValue<String> getBrowserSourceProjectVersion() { return getValue("browsersourceprojectversion", String.class); }
	/**
	 * Indicates the name of the company which created the browser.
	 *
	 * @return the value of the BrowserVendor property
	 * @see <a href="http://reference.sitepoint.com/css/vendorspecific">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getBrowserVendor() { return getValue("browservendor", String.class); }
	/**
	 * Indicates the version or subversion of the browser.
	 *
	 * @return the value of the BrowserVersion property
	 * @see <a href="http://www.webdevelopersnotes.com/how-do-i/check-browser-version.php">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getBrowserVersion() { return getValue("browserversion", String.class); }
	/**
	 * Refers to the list of video codecs in specific formats supported for Decode by the Web Browser. This list of codecs is supported for playback on a basic browser installation.
	 *
	 * @return the value of the BrowserVideoCodecsDecode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getBrowserVideoCodecsDecode() { return getList("browservideocodecsdecode", String.class); }
	/**
	 * Refers to the list of video codecs in specific formats supported for Encode by the Web Browser. This list of codecs is supported for capture on a basic browser installation.
	 *
	 * @return the value of the BrowserVideoCodecsEncode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getBrowserVideoCodecsEncode() { return getList("browservideocodecsencode", String.class); }
	/**
	 * Indicates the list of camera types the device has. If the device has a rotating camera, this property refers to both front and back facing cameras.
	 *
	 * @return the value of the CameraTypes property
	 */
	@Override
	public AspectPropertyValue<List<String>> getCameraTypes() { return getList("cameratypes", String.class); }
	/**
	 * Indicates if the browser supports the canvas element, useful for drawing graphics via scripting (usually JavaScript).
	 *
	 * @return the value of the Canvas property
	 * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/multipage/the-canvas-element.html">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCanvas() { return getValue("canvas", Boolean.class); }
	/**
	 * Stands for Composite Capability/Preference Profiles.  Refers to the list of MIME types supported by the operating system. The list does not include MIME types that are only enabled through the use of 3rd party applications.
	 *
	 * @return the value of the CcppAccept property
	 * @see <a href="http://en.wikipedia.org/wiki/Composite_Capability/Preference_Profiles">More information</a>
	 */
	@Override
	public AspectPropertyValue<List<String>> getCcppAccept() { return getList("ccppaccept", String.class); }
	/**
	 * Indicates which version of the Connected Limited Device Configuration the device supports for use with Java ME.
	 *
	 * @return the value of the CLDC property
	 * @see <a href="http://java.sun.com/products/cldc/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Double> getCLDC() { return getValue("cldc", Double.class); }
	/**
	 * Indicates the contrast ratio of the device.
	 *
	 * @return the value of the ContrastRatio property
	 */
	@Override
	public AspectPropertyValue<String> getContrastRatio() { return getValue("contrastratio", String.class); }
	/**
	 * Indicates if the browser supports http Cookies. However, the user may have disabled Cookies in their own configuration. Where data cannot be validated, it is assumed that the browser supports cookies.
	 *
	 * @return the value of the CookiesCapable property
	 * @see <a href="http://en.wikipedia.org/wiki/HTTP_cookie">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCookiesCapable() { return getValue("cookiescapable", Boolean.class); }
	/**
	 * Indicates the official name of the CPU within the SoC, e.g. ARM Cortex A9 or Krait (Qualcomm).
	 *
	 * @return the value of the CPU property
	 */
	@Override
	public AspectPropertyValue<String> getCPU() { return getValue("cpu", String.class); }
	/**
	 * Indicates the number of physical CPU cores the device has.
	 *
	 * @return the value of the CPUCores property
	 */
	@Override
	public AspectPropertyValue<Integer> getCPUCores() { return getValue("cpucores", Integer.class); }
	/**
	 * Indicates the Semiconductor Company that designed the CPU.
	 *
	 * @return the value of the CPUDesigner property
	 */
	@Override
	public AspectPropertyValue<String> getCPUDesigner() { return getValue("cpudesigner", String.class); }
	/**
	 * Indicates the maximum frequency of the CPU of the device in gigahertz (GHz).
	 *
	 * @return the value of the CPUMaximumFrequency property
	 */
	@Override
	public AspectPropertyValue<Double> getCPUMaximumFrequency() { return getValue("cpumaximumfrequency", Double.class); }
	/**
	 * Indicates the crawler name when applicable. Returns NotCrawler when the device is not a crawler.
	 *
	 * @return the value of the CrawlerName property
	 */
	@Override
	public AspectPropertyValue<String> getCrawlerName() { return getValue("crawlername", String.class); }
	/**
	 * Identification tokens, defined by the IETF, that a crawler includes in the User-Agent request header field to identify the crawler software.
	 *
	 * @return the value of the CrawlerProductTokens property
	 */
	@Override
	public AspectPropertyValue<List<String>> getCrawlerProductTokens() { return getList("crawlerproducttokens", String.class); }
	/**
	 * The url of the web page containing information from the crawler controller about the crawler. Where there is no publicly available information about the crawler, this instead links to their public website.
	 *
	 * @return the value of the CrawlerUrl property
	 */
	@Override
	public AspectPropertyValue<String> getCrawlerUrl() { return getValue("crawlerurl", String.class); }
	/**
	 * Indicates the purpose for which a crawler accesses content, including search, AI indexing, AI model training, and archiving data.
	 *
	 * @return the value of the CrawlerUsage property
	 */
	@Override
	public AspectPropertyValue<List<String>> getCrawlerUsage() { return getList("crawlerusage", String.class); }
	/**
	 * Indicates if the browser supports CSS3 background properties (such as background-image, background-color, etc.) that allow styling of the border and the background of an object, and create a shadow effect.
	 *
	 * @return the value of the CssBackground property
	 * @see <a href="http://www.w3.org/TR/css3-background/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssBackground() { return getValue("cssbackground", Boolean.class); }
	/**
	 * Indicates if the browser supports border images, allowing decoration of the border around an object.
	 *
	 * @return the value of the CssBorderImage property
	 * @see <a href="http://www.w3.org/TR/css3-background/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssBorderImage() { return getValue("cssborderimage", Boolean.class); }
	/**
	 * Indicates if the browser can draw CSS images into a Canvas.
	 *
	 * @return the value of the CssCanvas property
	 * @see <a href="http://www.webkit.org/blog/176/css-canvas-drawing/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssCanvas() { return getValue("csscanvas", Boolean.class); }
	/**
	 * Indicates if the browser supports CSS3 Color, allowing author control of the foreground colour and opacity of an element.
	 *
	 * @return the value of the CssColor property
	 * @see <a href="http://www.w3.org/TR/css3-color/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssColor() { return getValue("csscolor", Boolean.class); }
	/**
	 * Indicates if the browser supports CSS3 columns for setting column- width and column-count.
	 *
	 * @return the value of the CssColumn property
	 * @see <a href="http://www.w3schools.com/css3/css3_multiple_columns.asp">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssColumn() { return getValue("csscolumn", Boolean.class); }
	/**
	 * Indicates if the browser supports flexbox, allowing the automatic reordering of elements on the page when accessed from devices with different screen sizes.
	 *
	 * @return the value of the CssFlexbox property
	 * @see <a href="http://www.w3.org/TR/css3-flexbox/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssFlexbox() { return getValue("cssflexbox", Boolean.class); }
	/**
	 * Indicates if the browser supports CSS3 fonts, including non-standard fonts, e.g. @font-face.
	 *
	 * @return the value of the CssFont property
	 * @see <a href="http://www.w3.org/TR/css3-fonts/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssFont() { return getValue("cssfont", Boolean.class); }
	/**
	 * Indicates if the browser supports all CSS grid properties.
	 *
	 * @return the value of the CssGrid property
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssGrid() { return getValue("cssgrid", Boolean.class); }
	/**
	 * Indicates if the browser supports CSS3 images, allowing for fall-back images, gradients and other effects.
	 *
	 * @return the value of the CssImages property
	 * @see <a href="http://dev.w3.org/csswg/css3-images/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssImages() { return getValue("cssimages", Boolean.class); }
	/**
	 * Indicates if the browser supports MediaQueries for dynamic CSS that uses the @media rule.
	 *
	 * @return the value of the CssMediaQueries property
	 * @see <a href="http://www.w3.org/TR/css3-mediaqueries/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssMediaQueries() { return getValue("cssmediaqueries", Boolean.class); }
	/**
	 * Indicates if the browser supports the CSS 'min-width' and 'max-width' element.
	 *
	 * @return the value of the CssMinMax property
	 * @see <a href="http://www.w3.org/TR/CSS21/visudet.html#min-max-widths">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssMinMax() { return getValue("cssminmax", Boolean.class); }
	/**
	 * Indicates if the browser supports overflowing of clipped blocks.
	 *
	 * @return the value of the CssOverflow property
	 * @see <a href="http://www.w3.org/TR/CSS2/visufx.html">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssOverflow() { return getValue("cssoverflow", Boolean.class); }
	/**
	 * Indicates if the browser supports CSS position, allowing for different box placement algorithms, e.g. static, relative, absolute, fixed and initial.
	 *
	 * @return the value of the CssPosition property
	 * @see <a href="http://www.w3.org/TR/CSS21/visuren.html#choose-position">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssPosition() { return getValue("cssposition", Boolean.class); }
	/**
	 * Indicates if the browser supports all CSS3 text features including: text-overflow, word-wrap and word-break.
	 *
	 * @return the value of the CssText property
	 * @see <a href="http://www.w3.org/TR/css3-text/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssText() { return getValue("csstext", Boolean.class); }
	/**
	 * Indicates if the browser supports 2D transformations in CSS3 including rotating, scaling, etc. This property includes support for both transform and transform-origin properties.
	 *
	 * @return the value of the CssTransforms property
	 * @see <a href="http://www.w3.org/TR/css3-2d-transforms/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssTransforms() { return getValue("csstransforms", Boolean.class); }
	/**
	 * Indicates if the browser supports CSS3 transitions elements, used for animating changes to properties.
	 *
	 * @return the value of the CssTransitions property
	 * @see <a href="http://www.w3schools.com/css3/css3_transitions.asp">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssTransitions() { return getValue("csstransitions", Boolean.class); }
	/**
	 * Indicates if the browser supports CSS UI stylings, including text-overflow, css3-boxsizing and pointer properties.
	 *
	 * @return the value of the CssUI property
	 * @see <a href="http://www.w3.org/TR/css3-ui/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getCssUI() { return getValue("cssui", Boolean.class); }
	/**
	 * Indicates if the browser has the ability to embed custom data attributes on all HTML elements using the 'data-' prefix.
	 *
	 * @return the value of the DataSet property
	 */
	@Override
	public AspectPropertyValue<Boolean> getDataSet() { return getValue("dataset", Boolean.class); }
	/**
	 * Indicates if the browser allows encoded data to be contained in a URL.
	 *
	 * @return the value of the DataUrl property
	 * @see <a href="http://www.ietf.org/rfc/rfc2397">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getDataUrl() { return getValue("dataurl", Boolean.class); }
	/**
	 * Indicates which app stores are available on the device and come pre-installed by the manufacturer.
	 *
	 * @return the value of the DefaultAppStores property
	 */
	@Override
	public AspectPropertyValue<List<String>> getDefaultAppStores() { return getList("defaultappstores", String.class); }
	/**
	 * Indicates what certifications apply to this device.
	 *
	 * @return the value of the DeviceCertifications property
	 */
	@Override
	public AspectPropertyValue<List<String>> getDeviceCertifications() { return getList("devicecertifications", String.class); }
	/**
	 * Consists of four components separated by a hyphen symbol: Hardware-Platform-Browser-IsCrawler where each Component represents an ID of the corresponding Profile.
	 *
	 * @return the value of the DeviceId property
	 */
	@Override
	public AspectPropertyValue<String> getDeviceId() { return getValue("deviceid", String.class); }
	/**
	 * Indicates if the browser supports DOM events for device orientation, e.g. 'deviceorientation', 'devicemotion' and 'compassneedscalibration'.
	 *
	 * @return the value of the DeviceOrientation property
	 * @see <a href="http://www.w3.org/TR/orientation-event/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getDeviceOrientation() { return getValue("deviceorientation", Boolean.class); }
	/**
	 * Indicates the maximum volatile RAM capacity of the device in megabytes (MB). Where a device has different RAM capacity options, the largest option available is returned.
	 *
	 * @return the value of the DeviceRAM property
	 */
	@Override
	public AspectPropertyValue<Integer> getDeviceRAM() { return getValue("deviceram", Integer.class); }
	/**
	 * Indicates the volatile RAM capacity options for the device in megabytes (MB). If no variants are found, then the value returned will be the same as &quot;DeviceRAM&quot;.
	 *
	 * @return the value of the DeviceRAMVariants property
	 */
	@Override
	public AspectPropertyValue<List<String>> getDeviceRAMVariants() { return getList("deviceramvariants", String.class); }
	/**
	 * Indicates the type of the device based on values set in other properties, such as IsMobile, IsTablet, IsSmartphone, IsSmallScreen etc.
	 *
	 * @return the value of the DeviceType property
	 */
	@Override
	public AspectPropertyValue<String> getDeviceType() { return getValue("devicetype", String.class); }
	/**
	 * Used when detection method is not Exact or None. This is an integer value and the larger the value the less confident the detector is in this result.
	 *
	 * @return the value of the Difference property
	 */
	@Override
	public AspectPropertyValue<Integer> getDifference() { return getValue("difference", Integer.class); }
	/**
	 * Total difference in character positions where the substrings hashes were found away from where they were expected.
	 *
	 * @return the value of the Drift property
	 */
	@Override
	public AspectPropertyValue<Integer> getDrift() { return getValue("drift", Integer.class); }
	/**
	 * Indicates the device's Ingress Protection Rating against dust and water (http://en.wikipedia.org/wiki/IP_Code).
	 *
	 * @return the value of the Durability property
	 */
	@Override
	public AspectPropertyValue<List<String>> getDurability() { return getList("durability", String.class); }
	/**
	 * Indicates the dynamic contrast ratio of the device's screen.
	 *
	 * @return the value of the DynamicContrastRatio property
	 */
	@Override
	public AspectPropertyValue<String> getDynamicContrastRatio() { return getValue("dynamiccontrastratio", String.class); }
	/**
	 * Indicates the annual energy consumption of the device per year in kWh.
	 *
	 * @return the value of the EnergyConsumptionPerYear property
	 */
	@Override
	public AspectPropertyValue<Integer> getEnergyConsumptionPerYear() { return getValue("energyconsumptionperyear", Integer.class); }
	/**
	 * Indicates the maximum amount of memory in gigabytes (GB) the expansion slot of the device can support.
	 *
	 * @return the value of the ExpansionSlotMaxSize property
	 */
	@Override
	public AspectPropertyValue<Integer> getExpansionSlotMaxSize() { return getValue("expansionslotmaxsize", Integer.class); }
	/**
	 * Indicates the expansion slot type the device can support.
	 *
	 * @return the value of the ExpansionSlotType property
	 */
	@Override
	public AspectPropertyValue<List<String>> getExpansionSlotType() { return getList("expansionslottype", String.class); }
	/**
	 * Indicates if the browser supports the Fetch API.
	 *
	 * @return the value of the Fetch property
	 */
	@Override
	public AspectPropertyValue<Boolean> getFetch() { return getValue("fetch", Boolean.class); }
	/**
	 * Indicates if the browser supports file reading with events to show progress and errors.
	 *
	 * @return the value of the FileReader property
	 * @see <a href="http://www.w3.org/TR/FileAPI/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getFileReader() { return getValue("filereader", Boolean.class); }
	/**
	 * Indicates if the browser allows Blobs to be saved to client machines with events to show progress and errors. The End-User may opt to decline these files.
	 *
	 * @return the value of the FileSaver property
	 * @see <a href="http://www.w3.org/TR/file-writer-api/#idl-def-FileSaver">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getFileSaver() { return getValue("filesaver", Boolean.class); }
	/**
	 * Indicates if the browser allows files to be saved to client machines with events to show progress and errors. The End-User may opt to decline these files.
	 *
	 * @return the value of the FileWriter property
	 * @see <a href="http://www.w3.org/TR/file-writer-api/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getFileWriter() { return getValue("filewriter", Boolean.class); }
	/**
	 * Indicates if the browser supports the 'FormData' object. This property also refers to XMLHttpRequest. If the browser supports 'xhr2', the 'FormData' element will be also supported. 
	 *
	 * @return the value of the FormData property
	 * @see <a href="http://www.w3.org/TR/XMLHttpRequest/#interface-formdata">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getFormData() { return getValue("formdata", Boolean.class); }
	/**
	 * Indicates the list of frequency bands supported by the device.
	 *
	 * @return the value of the FrequencyBands property
	 */
	@Override
	public AspectPropertyValue<List<String>> getFrequencyBands() { return getList("frequencybands", String.class); }
	/**
	 * Indicates the resolution of the device's front camera in megapixels. For a device that has a rotating camera the same value is returned for front and back megapixels' properties.
	 *
	 * @return the value of the FrontCameraMegaPixels property
	 */
	@Override
	public AspectPropertyValue<Double> getFrontCameraMegaPixels() { return getValue("frontcameramegapixels", Double.class); }
	/**
	 * Indicates if the browser supports requests from a video or canvas element to be displayed in full-screen mode.
	 *
	 * @return the value of the Fullscreen property
	 * @see <a href="http://dvcs.w3.org/hg/fullscreen/raw-file/tip/Overview.html">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getFullscreen() { return getValue("fullscreen", Boolean.class); }
	/**
	 * Indicates if the browser supports a feature to acquire the geographical location. For information on which GeoLoc API the browser supports, refer to another property called JavaScriptPreferredGeoLocApi.
	 *
	 * @return the value of the GeoLocation property
	 * @see <a href="http://www.w3.org/TR/geolocation-API/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getGeoLocation() { return getValue("geolocation", Boolean.class); }
	/**
	 * Indicates the official name of the graphical chip within the SoC.
	 *
	 * @return the value of the GPU property
	 */
	@Override
	public AspectPropertyValue<String> getGPU() { return getValue("gpu", String.class); }
	/**
	 * Indicates the Semiconductor Company that designed the GPU.
	 *
	 * @return the value of the GPUDesigner property
	 */
	@Override
	public AspectPropertyValue<String> getGPUDesigner() { return getValue("gpudesigner", String.class); }
	/**
	 * Refers to the list of audio codecs supported for decoding by a Chipset. An audio codec is a program used to playback digital audio files. The values of this property are the codec's common name.
	 *
	 * @return the value of the HardwareAudioCodecsDecode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getHardwareAudioCodecsDecode() { return getList("hardwareaudiocodecsdecode", String.class); }
	/**
	 * Refers to the list of audio codecs supported for encoding by a Chipset. An audio codec is a program used to capture digital audio files. The values of this property are the codec's common name.
	 *
	 * @return the value of the HardwareAudioCodecsEncode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getHardwareAudioCodecsEncode() { return getList("hardwareaudiocodecsencode", String.class); }
	/**
	 * Indicates the carrier when the device is sold by the HardwareVendor on a single carrier or as indicated via device User-Agent.
	 *
	 * @return the value of the HardwareCarrier property
	 */
	@Override
	public AspectPropertyValue<String> getHardwareCarrier() { return getValue("hardwarecarrier", String.class); }
	/**
	 * Indicates the name of a group of devices that only differ by model or region but are marketed under the same name, e.g. Galaxy Tab S 10.5.
	 *
	 * @return the value of the HardwareFamily property
	 */
	@Override
	public AspectPropertyValue<String> getHardwareFamily() { return getValue("hardwarefamily", String.class); }
	/**
	 * A list of images associated with the device. The string contains the caption, followed by the full image URL separated with a tab character.
	 *
	 * @return the value of the HardwareImages property
	 */
	@Override
	public AspectPropertyValue<List<String>> getHardwareImages() { return getList("hardwareimages", String.class); }
	/**
	 * Indicates the model name or number used primarily by the hardware vendor to identify the device, e.g.SM-T805S. When a model identifier is not available the HardwareName will be used.
	 *
	 * @return the value of the HardwareModel property
	 */
	@Override
	public AspectPropertyValue<String> getHardwareModel() { return getValue("hardwaremodel", String.class); }
	/**
	 * Indicates all model numbers used by the hardware vendor to identify the device. This property compliments 'HardwareModel', e.g. Hardware Model Variants A1660 and A1778 correlate to the Hardware Model - iPhone 7.
	 *
	 * @return the value of the HardwareModelVariants property
	 */
	@Override
	public AspectPropertyValue<List<String>> getHardwareModelVariants() { return getList("hardwaremodelvariants", String.class); }
	/**
	 * Indicates the common marketing names associated with the device, e.g. Xperia Z5.
	 *
	 * @return the value of the HardwareName property
	 */
	@Override
	public AspectPropertyValue<List<String>> getHardwareName() { return getList("hardwarename", String.class); }
	/**
	 * Marketed name of the device or series (e.g., iPhone). If a device does not have an established naming system, this field will be an empty string.
	 *
	 * @return the value of the HardwareNamePrefix property
	 */
	@Override
	public AspectPropertyValue<String> getHardwareNamePrefix() { return getValue("hardwarenameprefix", String.class); }
	/**
	 * Marketed version of the device or series (e.g., 17 Pro Max). If a device does not have an established naming system, this field will be an empty string.
	 *
	 * @return the value of the HardwareNameVersion property
	 */
	@Override
	public AspectPropertyValue<String> getHardwareNameVersion() { return getValue("hardwarenameversion", String.class); }
	/**
	 * Indicates the source of the profile's specifications. This property will return 'Manufacturer' value if the profile data was obtained from the manufacturer of the device or the device itself. This property will return 'Authoritative' value if the profile data was not obtained from the manufacturer or the device itself but other third party sources (this may include retailers, social media, carriers, etc). This property will return 'Legacy' value if the profile data was obtained prior to 51degrees differentiating between Manufacturer and Authoritative. This property will return 'N/A' value if the profile data was not obtained due to unidentifiable User-Agent. The example profiles are: Generic Android Unknown, Unknown Tablet, etc.
	 *
	 * @return the value of the HardwareProfileSource property
	 */
	@Override
	public AspectPropertyValue<String> getHardwareProfileSource() { return getValue("hardwareprofilesource", String.class); }
	/**
	 * A measure of the popularity of this device model. All models are ordered by the number of events associated with that model that occurred in the sampling period. The device with the most events is ranked 1, the second 2 and so on. 
	 *
	 * @return the value of the HardwareRank property
	 */
	@Override
	public AspectPropertyValue<Integer> getHardwareRank() { return getValue("hardwarerank", Integer.class); }
	/**
	 * Indicates the name of the company that manufactures the device or primarily sells it, e.g. Samsung.
	 *
	 * @return the value of the HardwareVendor property
	 */
	@Override
	public AspectPropertyValue<String> getHardwareVendor() { return getValue("hardwarevendor", String.class); }
	/**
	 * Refers to the list of video codecs supported for decoding by a Chipset. A video codec is a program used to playback digital video files. The values of this property are the codec's common name. 
	 *
	 * @return the value of the HardwareVideoCodecsDecode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getHardwareVideoCodecsDecode() { return getList("hardwarevideocodecsdecode", String.class); }
	/**
	 * Refers to the list of video codecs supported for encoding by a Chipset. A video codec is a program used to capture digital video files. The values of this property are the codec's common name. 
	 *
	 * @return the value of the HardwareVideoCodecsEncode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getHardwareVideoCodecsEncode() { return getList("hardwarevideocodecsencode", String.class); }
	/**
	 * Indicates if the device has a camera capable of taking 3D images.
	 *
	 * @return the value of the Has3DCamera property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHas3DCamera() { return getValue("has3dcamera", Boolean.class); }
	/**
	 * Indicates if the device has a screen capable of displaying 3D images.
	 *
	 * @return the value of the Has3DScreen property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHas3DScreen() { return getValue("has3dscreen", Boolean.class); }
	/**
	 * Indicates if the device has a camera.
	 *
	 * @return the value of the HasCamera property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHasCamera() { return getValue("hascamera", Boolean.class); }
	/**
	 * Indicates if the device has a click wheel such as found on Apple iPod devices.
	 *
	 * @return the value of the HasClickWheel property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHasClickWheel() { return getValue("hasclickwheel", Boolean.class); }
	/**
	 * Indicates if the device has a physical numeric keypad.
	 *
	 * @return the value of the HasKeypad property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHasKeypad() { return getValue("haskeypad", Boolean.class); }
	/**
	 * Indicates if the device has embedded NFC (Near Field Communication) wireless technology.
	 *
	 * @return the value of the HasNFC property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHasNFC() { return getValue("hasnfc", Boolean.class); }
	/**
	 * Indicates if the device has a physical qwerty keyboard.
	 *
	 * @return the value of the HasQwertyPad property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHasQwertyPad() { return getValue("hasqwertypad", Boolean.class); }
	/**
	 * Indicates if the device has a removable battery. This property is not applicable for devices that do not have batteries. Unless otherwise stated this property will return a 'False' value for tablets.
	 *
	 * @return the value of the HasRemovableBattery property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHasRemovableBattery() { return getValue("hasremovablebattery", Boolean.class); }
	/**
	 * Indicates if the device has a touch screen. This property will return 'False' for a device that does not have an integrated screen.
	 *
	 * @return the value of the HasTouchScreen property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHasTouchScreen() { return getValue("hastouchscreen", Boolean.class); }
	/**
	 * Indicates if the device has a trackpad or trackball. Examples of devices that support this property are the Nexus One and Blackberry Curve.
	 *
	 * @return the value of the HasTrackPad property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHasTrackPad() { return getValue("hastrackpad", Boolean.class); }
	/**
	 * Indicates if the device has a virtual qwerty keyboard capability.
	 *
	 * @return the value of the HasVirtualQwerty property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHasVirtualQwerty() { return getValue("hasvirtualqwerty", Boolean.class); }
	/**
	 * Indicates whether the browser advertises a web driver, as a browser under automation is required to do. Returns True where the browser advertises a web driver, and False where it advertises none. The value reports what the browser advertises and not what an automated client may be concealing.
	 *
	 * @return the value of the HasWebDriver property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHasWebDriver() { return getValue("haswebdriver", Boolean.class); }
	/**
	 * JavaScript which overrides the HasWebDriver property to report whether the browser advertised a web driver when the 51Degrees JavaScript ran.
	 *
	 * @return the value of the HasWebDriverJavaScript property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getHasWebDriverJavaScript() { return getValue("haswebdriverjavascript", JavaScript.class); }
	/**
	 * Indicates if the browser stores the session history for a web page that contains the URLs visited by the browser's user.
	 *
	 * @return the value of the History property
	 * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getHistory() { return getValue("history", Boolean.class); }
	/**
	 * Indicates if the browser is able to use media inputs, e.g. webcam and microphone, in a script and as an input for forms, e.g. '&lt;input type=&quot;file&quot; accept=&quot;image/*&quot; id=&quot;capture&quot;&gt;' would prompt image- capturing software to open.
	 *
	 * @return the value of the Html-Media-Capture property
	 * @see <a href="http://www.w3.org/TR/html-media-capture/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getHtmlMediaCapture() { return getValue("html-media-capture", Boolean.class); }
	/**
	 * Indicates if the browser supports the new markup in HTML 5 that also refers to 'New Semantic Elements' such as &lt;header&gt;, &lt;nav&gt;, &lt;section&gt;, &lt;aside&gt;,&lt;footer&gt; etc.
	 *
	 * @return the value of the Html5 property
	 * @see <a href="http://www.w3.org/TR/html5/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getHtml5() { return getValue("html5", Boolean.class); }
	/**
	 * Lists what audio formats, if any, the browser supports using the HTML5 &lt;audio&gt; tag.
	 *
	 * @return the value of the Html5Audio property
	 * @see <a href="http://www.w3schools.com/html5/html5_audio.asp">More information</a>
	 */
	@Override
	public AspectPropertyValue<List<String>> getHtml5Audio() { return getList("html5audio", String.class); }
	/**
	 * Lists what video formats, if any, the browser supports using the HTLM5 &lt;video&gt; tag.
	 *
	 * @return the value of the Html5Video property
	 * @see <a href="http://www.w3schools.com/html5/html5_video.asp">More information</a>
	 */
	@Override
	public AspectPropertyValue<List<String>> getHtml5Video() { return getList("html5video", String.class); }
	/**
	 * Refers to the latest version of HyperText Markup Language (HTML) supported by the browser.
	 *
	 * @return the value of the HtmlVersion property
	 * @see <a href="http://www.w3schools.com/w3c/w3c_html.asp">More information</a>
	 */
	@Override
	public AspectPropertyValue<Double> getHtmlVersion() { return getValue("htmlversion", Double.class); }
	/**
	 * Indicates if the browser supports HTTP version 2.
	 *
	 * @return the value of the Http2 property
	 */
	@Override
	public AspectPropertyValue<Boolean> getHttp2() { return getValue("http2", Boolean.class); }
	/**
	 * Indicates if the browser supports HTTP Live Streaming, also known as HLS.
	 *
	 * @return the value of the HttpLiveStreaming property
	 */
	@Override
	public AspectPropertyValue<String> getHttpLiveStreaming() { return getValue("httplivestreaming", String.class); }
	/**
	 * Indicates if the browser supports the 'Iframe' element, used to embed another document within a current HTML document.
	 *
	 * @return the value of the Iframe property
	 * @see <a href="http://dev.w3.org/html5/spec/the-iframe-element.html#the-iframe-element">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getIframe() { return getValue("iframe", Boolean.class); }
	/**
	 * Indicates if the browser supports an indexed local database.
	 *
	 * @return the value of the IndexedDB property
	 * @see <a href="http://www.w3.org/TR/IndexedDB/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getIndexedDB() { return getValue("indexeddb", Boolean.class); }
	/**
	 * Indicates the internal persistent storage (ROM capacity) options the device can be supplied with in gigabytes (GB), including the device's Operating System and bundled applications. This could also be referred to as &quot;Electrically Erasable Programmable Read-Only Memory (EEPROM)&quot; or &quot;Non Volatile Random Access Memory (NVRAM)&quot;. If no variants are found, then the value returned will be the same as &quot;MaxInternalStorage&quot;.
	 *
	 * @return the value of the InternalStorageVariants property
	 */
	@Override
	public AspectPropertyValue<List<String>> getInternalStorageVariants() { return getList("internalstoragevariants", String.class); }
	/**
	 * Indicates if a web page is accessed through a VR headset.
	 *
	 * @return the value of the InVRMode property
	 */
	@Override
	public AspectPropertyValue<Boolean> getInVRMode() { return getValue("invrmode", Boolean.class); }
	/**
	 * Indicates whether the crawler operates for an AI related purpose, as defined by the values in the CrawlerUsage property. This is based on our judgement of information available from the crawler controller or other trusted research.
	 *
	 * @return the value of the IsArtificialIntelligence property
	 */
	@Override
	public AspectPropertyValue<String> getIsArtificialIntelligence() { return getValue("isartificialintelligence", String.class); }
	/**
	 * Indicates if the device is primarily a game console, such as an Xbox or Playstation.
	 *
	 * @return the value of the IsConsole property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsConsole() { return getValue("isconsole", Boolean.class); }
	/**
	 * Indicates if the source of the web traffic identifies itself as operating without human interaction for the purpose of monitoring the availability or performance of a web site, retrieving a response for inclusion in a search engine or is requesting structured data such as via an API. Such sources are often referred to as crawlers, bots, robots, spiders, probes, monitors or HTTP services among other terms. Where the source pretends to be a device operating with human interaction, such as a smartphone or tablet, this property will return, 'False'.
	 *
	 * @return the value of the IsCrawler property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsCrawler() { return getValue("iscrawler", Boolean.class); }
	/**
	 * Indicates if the browser may be optimised for low bandwidth. A true value indicates the browser supports a feature that can improve performance on low bandwidth connections, either via the removal of elements, features, a proxy or other methods.
	 *
	 * @return the value of the IsDataMinimising property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsDataMinimising() { return getValue("isdataminimising", Boolean.class); }
	/**
	 * Indicates if the application is an email browser (Outlook, Gmail, YahooMail, etc.) that is primarily used to access and manage emails (usually from mobile devices).
	 *
	 * @return the value of the IsEmailBrowser property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsEmailBrowser() { return getValue("isemailbrowser", Boolean.class); }
	/**
	 * Indicates if the mobile device accessing a web page emulates a desktop computer. This property is not applicable for desktops, media hubs, TVs and consoles.
	 *
	 * @return the value of the IsEmulatingDesktop property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsEmulatingDesktop() { return getValue("isemulatingdesktop", Boolean.class); }
	/**
	 * Indicates a browser that does not correctly identify the physical hardware device and instead reports an emulated device.
	 *
	 * @return the value of the IsEmulatingDevice property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsEmulatingDevice() { return getValue("isemulatingdevice", Boolean.class); }
	/**
	 * Indicates if the device is primarily advertised as an e-reader. If the device type is EReader then the device is not classified as a tablet.
	 *
	 * @return the value of the IsEReader property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsEReader() { return getValue("isereader", Boolean.class); }
	/**
	 * Indicates a profile which contains more than a single hardware device. When this is true all returned properties represent the default value or lowest given specification of all grouped devices. E.g. the profile representing unknown Windows 10 tablets will return true. Apple devices detected through JavascriptHardwareProfile that do not uniquely identify a device will also return true, and HardwareModelVariants will return a list of model numbers associated with that device group.
	 *
	 * @return the value of the IsHardwareGroup property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsHardwareGroup() { return getValue("ishardwaregroup", Boolean.class); }
	/**
	 * Indicates whether the browser identifies itself as running without a visible window, for example through the HeadlessChrome token in the User-Agent. The value reflects what the browser advertises about itself and does not indicate whether the session is automated or controlled by a web driver.
	 *
	 * @return the value of the IsHeadless property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsHeadless() { return getValue("isheadless", Boolean.class); }
	/**
	 * Indicates if the device is a media hub or set top box that requires an external display(s).
	 *
	 * @return the value of the IsMediaHub property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsMediaHub() { return getValue("ismediahub", Boolean.class); }
	/**
	 * Indicates if the device's primary data connection is wireless and the device is designed to operate mostly by battery power (e.g. mobile phone, smartphone or tablet). This property does not indicate if the device is a mobile phone or not. Laptops are not classified as mobile devices under this definition and so 'IsMobile' will be 'False'.
	 *
	 * @return the value of the IsMobile property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsMobile() { return getValue("ismobile", Boolean.class); }
	/**
	 * Indicates whether the PlatformVendor is providing a frozen or incorrect platform version in the User-Agent evidence. When True, the PlatformVersion will be the most likely version and may be different to the value provided in the evidence.
	 *
	 * @return the value of the IsPlatformVersionFrozen property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsPlatformVersionFrozen() { return getValue("isplatformversionfrozen", Boolean.class); }
	/**
	 * Indicates whether the device screen is foldable or not. If the device does not have a screen or the screen is not foldable, 'False' is returned.
	 *
	 * @return the value of the IsScreenFoldable property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsScreenFoldable() { return getValue("isscreenfoldable", Boolean.class); }
	/**
	 * Indicates if the device is a mobile with a screen size less than 2.5 inches even where the device is marketed as a Smartphone.
	 *
	 * @return the value of the IsSmallScreen property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsSmallScreen() { return getValue("issmallscreen", Boolean.class); }
	/**
	 * Indicates whether the device can make and receive phone calls, has a screen size greater than or equal to 2.5 inches, runs a modern operating system (Android, iOS, Windows Phone, BlackBerry etc.), is not designed to be a wearable technology and is marketed by the vendor as a Smartphone.
	 *
	 * @return the value of the IsSmartPhone property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsSmartPhone() { return getValue("issmartphone", Boolean.class); }
	/**
	 * Indicates if the device is a web enabled computerised wristwatch with other capabilities beyond timekeeping, such as push notifications. It runs on a Smart Operating System i.e. Android, WatchOS, Tizen, Ubuntu Touch and is designed to be wearable technology.
	 *
	 * @return the value of the IsSmartWatch property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsSmartWatch() { return getValue("issmartwatch", Boolean.class); }
	/**
	 * Indicates if the device is primarily marketed as a tablet or phablet and has a screen size equal to or greater than 7 inches.
	 *
	 * @return the value of the IsTablet property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsTablet() { return getValue("istablet", Boolean.class); }
	/**
	 * Indicates if the device is a TV running on a smart operating system e.g. Android.
	 *
	 * @return the value of the IsTv property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsTv() { return getValue("istv", Boolean.class); }
	/**
	 * Indicates whether the device is likely to be a genuine iPhone based on interrogation by JavaScript contained in the JavaScriptHardwareProfile property.
	 *
	 * @return the value of the IsVerifiediPhone property
	 */
	@Override
	public AspectPropertyValue<String> getIsVerifiediPhone() { return getValue("isverifiediphone", String.class); }
	/**
	 * JavaScript which overrides the IsVerifiediPhone property to report whether an iPhone is genuine. This is only used where the device is presenting as an iPhone with the evidence provided. Requires JavascriptHardwareProfile property.
	 *
	 * @return the value of the IsVerifiediPhoneJavaScript property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getIsVerifiediPhoneJavaScript() { return getValue("isverifiediphonejavascript", JavaScript.class); }
	/**
	 * Indicates whether the browser window was in view when the 51Degrees JavaScript ran on the page. A false value means the page was in a background tab, in a minimised window, entirely obscured by another window, or on a device whose screen was off at that moment. The value describes that moment only and is not updated if the window later comes into view.
	 *
	 * @return the value of the IsVisible property
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsVisible() { return getValue("isvisible", Boolean.class); }
	/**
	 * JavaScript which overrides the IsVisible property to report whether the browser window was in view when the 51Degrees JavaScript ran on the page.
	 *
	 * @return the value of the IsVisibleJavaScript property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getIsVisibleJavaScript() { return getValue("isvisiblejavascript", JavaScript.class); }
	/**
	 * Indicates if the browser or app is being used to access a web page through a WebView.
	 *
	 * @return the value of the IsWebApp property
	 * @see <a href="https://en.wikipedia.org/wiki/WebView">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getIsWebApp() { return getValue("iswebapp", Boolean.class); }
	/**
	 * The number of iterations carried out in order to find a match. This is the number of nodes in the graph which have been visited.
	 *
	 * @return the value of the Iterations property
	 */
	@Override
	public AspectPropertyValue<Integer> getIterations() { return getValue("iterations", Integer.class); }
	/**
	 * Indicates if the browser supports JavaScript.
	 *
	 * @return the value of the Javascript property
	 * @see <a href="http://www.w3schools.com/js/js_intro.asp">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getJavascript() { return getValue("javascript", Boolean.class); }
	/**
	 * JavaScript that checks for browser specific features and overrides the ProfileID.
	 *
	 * @return the value of the JavaScriptBrowserOverride property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getJavaScriptBrowserOverride() { return getValue("javascriptbrowseroverride", JavaScript.class); }
	/**
	 * Indicates if the browser supports the JavaScript that can manipulate CSS on the browser's web page.
	 *
	 * @return the value of the JavascriptCanManipulateCSS property
	 */
	@Override
	public AspectPropertyValue<Boolean> getJavascriptCanManipulateCSS() { return getValue("javascriptcanmanipulatecss", Boolean.class); }
	/**
	 * Indicates if the browser supports the JavaScript that can manipulate the Document Object Model on the browser's web page.
	 *
	 * @return the value of the JavascriptCanManipulateDOM property
	 */
	@Override
	public AspectPropertyValue<Boolean> getJavascriptCanManipulateDOM() { return getValue("javascriptcanmanipulatedom", Boolean.class); }
	/**
	 * Indicates if the browser supports JavaScript that is able to access HTML elements from their ID using the getElementById method.
	 *
	 * @return the value of the JavascriptGetElementById property
	 */
	@Override
	public AspectPropertyValue<Boolean> getJavascriptGetElementById() { return getValue("javascriptgetelementbyid", Boolean.class); }
	/**
	 * Contains Javascript to get high entropy values.
	 *
	 * @return the value of the JavascriptGetHighEntropyValues property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getJavascriptGetHighEntropyValues() { return getValue("javascriptgethighentropyvalues", JavaScript.class); }
	/**
	 * JavaScript that can override the profile found by the server using information on the client device. This property is applicable for Apple devices which do not provide information about the model in the User-Agent string.
	 *
	 * @return the value of the JavascriptHardwareProfile property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getJavascriptHardwareProfile() { return getValue("javascripthardwareprofile", JavaScript.class); }
	/**
	 * Refers to the JavaScript snippet used to optimise images.
	 *
	 * @return the value of the JavascriptImageOptimiser property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getJavascriptImageOptimiser() { return getValue("javascriptimageoptimiser", JavaScript.class); }
	/**
	 * Indicates which GeoLoc API JavaScript the browser supports. If a browser supports a feature to acquire the user's geographical location, another property called 'GeoLocation' will be set to True.
	 *
	 * @return the value of the JavascriptPreferredGeoLocApi property
	 */
	@Override
	public AspectPropertyValue<String> getJavascriptPreferredGeoLocApi() { return getValue("javascriptpreferredgeolocapi", String.class); }
	/**
	 * Indicates if the browser allows registration of event listeners on event targets by using the addEventListener() method.
	 *
	 * @return the value of the JavascriptSupportsEventListener property
	 */
	@Override
	public AspectPropertyValue<Boolean> getJavascriptSupportsEventListener() { return getValue("javascriptsupportseventlistener", Boolean.class); }
	/**
	 * Indicates if the browser supports the JavaScript events 'onload', 'onclick' and 'onselect'. 
	 *
	 * @return the value of the JavascriptSupportsEvents property
	 */
	@Override
	public AspectPropertyValue<Boolean> getJavascriptSupportsEvents() { return getValue("javascriptsupportsevents", Boolean.class); }
	/**
	 * Indicates if the browser supports the JavaScript that is able to insert HTML into a DIV tag.
	 *
	 * @return the value of the JavascriptSupportsInnerHtml property
	 */
	@Override
	public AspectPropertyValue<Boolean> getJavascriptSupportsInnerHtml() { return getValue("javascriptsupportsinnerhtml", Boolean.class); }
	/**
	 * Indicates which JavaScript version the browser uses. The number refers to JavaScript versioning, not ECMAscript or Jscript. If the browser doesn't support JavaScript then 'NotSupported' value is returned.
	 *
	 * @return the value of the JavascriptVersion property
	 */
	@Override
	public AspectPropertyValue<String> getJavascriptVersion() { return getValue("javascriptversion", String.class); }
	/**
	 * Indicates the browser supports JPEG 2000 image format.
	 *
	 * @return the value of the Jpeg2000 property
	 */
	@Override
	public AspectPropertyValue<Boolean> getJpeg2000() { return getValue("jpeg2000", Boolean.class); }
	/**
	 * Refers to the grade of the level the device has with the jQuery Mobile Framework, as posted by jQuery.
	 *
	 * @return the value of the jQueryMobileSupport property
	 * @see <a href="http://jquerymobile.com/gbs/">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getjQueryMobileSupport() { return getValue("jquerymobilesupport", String.class); }
	/**
	 * Indicates if the browser supports the 'JSON' object. This property may need a vendor prefix, e.g. webkit, moz, etc.
	 *
	 * @return the value of the Json property
	 * @see <a href="http://es5.github.com/x15.12.html">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getJson() { return getValue("json", Boolean.class); }
	/**
	 * Refers to the name of the embedded technology the browser uses to display formatted content on the screen.
	 *
	 * @return the value of the LayoutEngine property
	 * @see <a href="http://en.wikipedia.org/wiki/Web_browser_engine">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getLayoutEngine() { return getValue("layoutengine", String.class); }
	/**
	 * Indicates if the browser supports the CSS-mask element that allows users to alter the visibility of an item by either partially or fully hiding the item.
	 *
	 * @return the value of the Masking property
	 * @see <a href="http://www.webkit.org/blog/181/css-masks/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getMasking() { return getValue("masking", Boolean.class); }
	/**
	 * Indicates the number of hash nodes matched within the evidence.
	 *
	 * @return the value of the MatchedNodes property
	 */
	@Override
	public AspectPropertyValue<Integer> getMatchedNodes() { return getValue("matchednodes", Integer.class); }
	/**
	 * Indicates the maximum amount of internal persistent storage (ROM capacity) with which the device is supplied in gigabytes (GB), including the space used by the device's Operating System and bundled applications. This could also be referred to as &quot;Electrically Erasable Programmable Read-Only Memory (EEPROM)&quot; or &quot;Non Volatile Random Access Memory (NVRAM)&quot;. Where a device has different internal storage options, the largest option available is returned.
	 *
	 * @return the value of the MaxInternalStorage property
	 */
	@Override
	public AspectPropertyValue<Double> getMaxInternalStorage() { return getValue("maxinternalstorage", Double.class); }
	/**
	 * Indicates the maximum number of &quot;Universal Integrated Circuit Cards (UICC - more commonly known as, SIM)&quot; the device can support including both removable and embedded. If the device doesn't support any UICC then a value of '0' is returned.
	 *
	 * @return the value of the MaxNumberOfSIMCards property
	 */
	@Override
	public AspectPropertyValue<Integer> getMaxNumberOfSIMCards() { return getValue("maxnumberofsimcards", Integer.class); }
	/**
	 * Indicates the maximum standby time of the device in hours. This property is not applicable for a device without a battery.
	 *
	 * @return the value of the MaxStandbyTime property
	 */
	@Override
	public AspectPropertyValue<Integer> getMaxStandbyTime() { return getValue("maxstandbytime", Integer.class); }
	/**
	 * Indicates the maximum talk time of the device in minutes. This property is not applicable for a device that does not have a battery or support phone calls.
	 *
	 * @return the value of the MaxTalkTime property
	 */
	@Override
	public AspectPropertyValue<Integer> getMaxTalkTime() { return getValue("maxtalktime", Integer.class); }
	/**
	 * Indicates the maximum general usage time of the device in minutes. This property is not applicable for a device without a battery.
	 *
	 * @return the value of the MaxUsageTime property
	 */
	@Override
	public AspectPropertyValue<Integer> getMaxUsageTime() { return getValue("maxusagetime", Integer.class); }
	/**
	 * Indicates if the browser supports a meter element that represents a scalar measurement within a known range or fractional value. This property does not indicate whether the browser supports the progress bar indication. For this purpose, the progress property should be used.
	 *
	 * @return the value of the Meter property
	 * @see <a href="http://www.w3.org/TR/html-markup/meter.html">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getMeter() { return getValue("meter", Boolean.class); }
	/**
	 * The method used to determine the match result.
	 *
	 * @return the value of the Method property
	 */
	@Override
	public AspectPropertyValue<String> getMethod() { return getValue("method", String.class); }
	/**
	 * Indicates which version of Mobile Information Device Profile the device supports, used with Java ME and CLDC.
	 *
	 * @return the value of the MIDP property
	 */
	@Override
	public AspectPropertyValue<Double> getMIDP() { return getValue("midp", Double.class); }
	/**
	 * Refers to the 'Retail Branding' value returned for Android Google Play native applications, when the android.os.Build.BRAND javascript is used to display the class. This property is not applicable for hardware running on operating systems other than Android.
	 *
	 * @return the value of the NativeBrand property
	 */
	@Override
	public AspectPropertyValue<List<String>> getNativeBrand() { return getList("nativebrand", String.class); }
	/**
	 * Refers to the 'Device' value returned for Android Google Play native applications, when the android.os.Build.DEVICE javascript is used to display the class. This property is not applicable for hardware running on operating systems other than Android.
	 *
	 * @return the value of the NativeDevice property
	 */
	@Override
	public AspectPropertyValue<List<String>> getNativeDevice() { return getList("nativedevice", String.class); }
	/**
	 * Refers to the 'Model' value returned for Android Google Play or Amazon native applications when the android.os.Build.MODEL javascript is used to display the class. For Apple devices, this property refers to the device identifier, for example 'iPad5,4'. This may be obtained by calling sysctlbyname and passing hw.machine as a name parameter or alternatively by obtaining &quot;machine&quot; member from a populated utsname structure using uname API.
	 *
	 * @return the value of the NativeModel property
	 */
	@Override
	public AspectPropertyValue<List<String>> getNativeModel() { return getList("nativemodel", String.class); }
	/**
	 * NativeName Refers to the 'Marketing Name' value that a device is registered with on the Google Play service. This property is not applicable for hardware running on operating systems other than Android.
	 *
	 * @return the value of the NativeName property
	 */
	@Override
	public AspectPropertyValue<List<String>> getNativeName() { return getList("nativename", String.class); }
	/**
	 * Indicates the name of the mobile operating system (iOS, Android) for which an application program has been developed to be used by a device.
	 *
	 * @return the value of the NativePlatform property
	 */
	@Override
	public AspectPropertyValue<String> getNativePlatform() { return getValue("nativeplatform", String.class); }
	/**
	 * Indicates the number of screens the device has. This property is not applicable for a device that does not have a screen. A display is only considered a screen if it could be used to display a web page.
	 *
	 * @return the value of the NumberOfScreens property
	 */
	@Override
	public AspectPropertyValue<Integer> getNumberOfScreens() { return getValue("numberofscreens", Integer.class); }
	/**
	 * Indicates the name of the company that manufactures the device.
	 *
	 * @return the value of the OEM property
	 */
	@Override
	public AspectPropertyValue<String> getOEM() { return getValue("oem", String.class); }
	/**
	 * Indicates the power consumption of the device while switched on.
	 *
	 * @return the value of the OnPowerConsumption property
	 */
	@Override
	public AspectPropertyValue<Integer> getOnPowerConsumption() { return getValue("onpowerconsumption", Integer.class); }
	/**
	 * The ratio of the resolution in physical pixels to the resolution in CSS pixels. This is approximated by screen resolution and screen size when the value is not known.
	 *
	 * @return the value of the PixelRatio property
	 */
	@Override
	public AspectPropertyValue<Double> getPixelRatio() { return getValue("pixelratio", Double.class); }
	/**
	 * JavaScript that can override the property value found by the server using information on the client device. This property is applicable for browsers that support pixel ratio cookie.
	 *
	 * @return the value of the PixelRatioJavascript property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getPixelRatioJavascript() { return getValue("pixelratiojavascript", JavaScript.class); }
	/**
	 * Indicates the age in months of the operating system since the PlatformReleaseYear and PlatformReleaseMonth.
	 *
	 * @return the value of the PlatformDiscontinuedAge property
	 */
	@Override
	public AspectPropertyValue<Integer> getPlatformDiscontinuedAge() { return getValue("platformdiscontinuedage", Integer.class); }
	/**
	 * The month in which further development for the platform version is stopped by the platform vendor. This occurs when a new stable version of the platform is released.
	 *
	 * @return the value of the PlatformDiscontinuedMonth property
	 */
	@Override
	public AspectPropertyValue<String> getPlatformDiscontinuedMonth() { return getValue("platformdiscontinuedmonth", String.class); }
	/**
	 * The year in which further development for the platform version is stopped by the platform vendor. This occurs when a new stable version of the platform is released.
	 *
	 * @return the value of the PlatformDiscontinuedYear property
	 */
	@Override
	public AspectPropertyValue<Integer> getPlatformDiscontinuedYear() { return getValue("platformdiscontinuedyear", Integer.class); }
	/**
	 * A list of logos associated with the Software. The string contains the caption, followed by the full image URL separated with a tab character.
	 *
	 * @return the value of the PlatformLogos property
	 */
	@Override
	public AspectPropertyValue<List<String>> getPlatformLogos() { return getList("platformlogos", String.class); }
	/**
	 * Indicates the name of the operating system the device is using.
	 *
	 * @return the value of the PlatformName property
	 * @see <a href="http://en.wikipedia.org/wiki/List_of_operating_systems">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getPlatformName() { return getValue("platformname", String.class); }
	/**
	 * Indicates the age in months of the operating system since the PlatformPreviewYear and PlatformPreviewMonth.
	 *
	 * @return the value of the PlatformPreviewAge property
	 */
	@Override
	public AspectPropertyValue<Integer> getPlatformPreviewAge() { return getValue("platformpreviewage", Integer.class); }
	/**
	 * The month in which the platform version was originally released as a Beta/Developer version by the platform vendor. This is before it is officially released as a stable version, to ensure wider testing by the community can take place.
	 *
	 * @return the value of the PlatformPreviewMonth property
	 */
	@Override
	public AspectPropertyValue<String> getPlatformPreviewMonth() { return getValue("platformpreviewmonth", String.class); }
	/**
	 * The year in which the platform version was originally released as a Beta/Developer version by the platform vendor. This is before it is officially released as a stable version, to ensure wider testing by the community can take place.
	 *
	 * @return the value of the PlatformPreviewYear property
	 */
	@Override
	public AspectPropertyValue<Integer> getPlatformPreviewYear() { return getValue("platformpreviewyear", Integer.class); }
	/**
	 * A measure of the popularity of this software platform (i.e. OS and version). All platforms are ordered by the number of events associated with that platform that occurred in the sampling period. The platform with the most events is ranked 1, the second 2 and so on.
	 *
	 * @return the value of the PlatformRank property
	 */
	@Override
	public AspectPropertyValue<Integer> getPlatformRank() { return getValue("platformrank", Integer.class); }
	/**
	 * Indicates the age in months of the operating system since the PlatformReleaseYear and PlatformReleaseMonth.
	 *
	 * @return the value of the PlatformReleaseAge property
	 */
	@Override
	public AspectPropertyValue<Integer> getPlatformReleaseAge() { return getValue("platformreleaseage", Integer.class); }
	/**
	 * The month in which the platform version was officially released to users by the platform vendor. This version is called the stable version as any bugs or difficulties highlighted in the Beta/Developer version will have been fixed for this release.
	 *
	 * @return the value of the PlatformReleaseMonth property
	 */
	@Override
	public AspectPropertyValue<String> getPlatformReleaseMonth() { return getValue("platformreleasemonth", String.class); }
	/**
	 * The year in which the platform version was officially released to users by the platform vendor. This version is called the stable version as any bugs or difficulties highlighted in the Beta/Developer Version will have been fixed for this release.
	 *
	 * @return the value of the PlatformReleaseYear property
	 */
	@Override
	public AspectPropertyValue<Integer> getPlatformReleaseYear() { return getValue("platformreleaseyear", Integer.class); }
	/**
	 * Indicates the name of the company that developed the operating system.
	 *
	 * @return the value of the PlatformVendor property
	 * @see <a href="http://en.wikipedia.org/wiki/Mobile_operating_system">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getPlatformVendor() { return getValue("platformvendor", String.class); }
	/**
	 * Indicates the version or subversion of the software platform. Where the PlatformVendor is Apple, the PlatformVersion returned will be the most likely version. It may be different to the value provided in the evidence.
	 *
	 * @return the value of the PlatformVersion property
	 * @see <a href="http://en.wikipedia.org/wiki/Mobile_operating_system">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getPlatformVersion() { return getValue("platformversion", String.class); }
	/**
	 * Refers to the number of unique client IPs from which this device has been seen.
	 *
	 * @return the value of the Popularity property
	 */
	@Override
	public AspectPropertyValue<String> getPopularity() { return getValue("popularity", String.class); }
	/**
	 * Indicates if the browser supports messages between different documents.
	 *
	 * @return the value of the PostMessage property
	 * @see <a href="http://www.w3.org/TR/webmessaging/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getPostMessage() { return getValue("postmessage", Boolean.class); }
	/**
	 * Indicates if the browser can prefetch resources without executing them.
	 *
	 * @return the value of the Preload property
	 */
	@Override
	public AspectPropertyValue<Boolean> getPreload() { return getValue("preload", Boolean.class); }
	/**
	 * Indicates a price range describing the recommended retail price of the device at the date of release, inclusive of tax (where applicable).  Prices are in United States Dollars (USD); if the price is not originally in USD it will be converted to USD using the relevant exchange rate at the time of launch. Prices are for the SIM-free version of the device (if applicable). In cases where there are several versions of the same model of the device, the price will reflect the device that was used to populate the specifications.
	 *
	 * @return the value of the PriceBand property
	 */
	@Override
	public AspectPropertyValue<String> getPriceBand() { return getValue("priceband", String.class); }
	/**
	 * Gets the Profiles property.
	 *
	 * @return the value of the Profiles property
	 */
	@Override
	public AspectPropertyValue<String> getProfiles() { return getValue("profiles", String.class); }
	/**
	 * Indicates if the browser supports progress reports, such as with HTTP requests. The progress element can be used to display the progress of the task. This property doesn't represent a scalar measurement. If the browser supports a gauge, the meter property should be used.
	 *
	 * @return the value of the Progress property
	 * @see <a href="http://www.w3.org/TR/progress-events/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getProgress() { return getValue("progress", Boolean.class); }
	/**
	 * Indicates the level of support for the Promise object. The Promise object represents the eventual completion (or failure) of an asynchronous operation, and its resulting value.
	 *
	 * @return the value of the Promise property
	 */
	@Override
	public AspectPropertyValue<String> getPromise() { return getValue("promise", String.class); }
	/**
	 * Indicates if the browser supports simple dialogues (window.alert, window.confirm and window.prompt).
	 *
	 * @return the value of the Prompts property
	 * @see <a href="http://www.w3.org/TR/html5/timers.html#user-prompts">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getPrompts() { return getValue("prompts", Boolean.class); }
	/**
	 * Indicates the maximum number of frames per second of the output image of the device in Hertz.
	 *
	 * @return the value of the RefreshRate property
	 */
	@Override
	public AspectPropertyValue<Integer> getRefreshRate() { return getValue("refreshrate", Integer.class); }
	/**
	 * Indicates the age in months of the device since the ReleaseYear and ReleaseMonth.
	 *
	 * @return the value of the ReleaseAge property
	 */
	@Override
	public AspectPropertyValue<Integer> getReleaseAge() { return getValue("releaseage", Integer.class); }
	/**
	 * Indicates the month in which the device was released or the month in which the device was first seen by 51Degrees (if the release date cannot be identified).
	 *
	 * @return the value of the ReleaseMonth property
	 */
	@Override
	public AspectPropertyValue<String> getReleaseMonth() { return getValue("releasemonth", String.class); }
	/**
	 * Indicates the year in which the device was released or the year in which the device was first seen by 51Degrees (if the release date cannot be identified).
	 *
	 * @return the value of the ReleaseYear property
	 */
	@Override
	public AspectPropertyValue<Integer> getReleaseYear() { return getValue("releaseyear", Integer.class); }
	/**
	 * Indicates  the device's supported satellite navigation types.
	 *
	 * @return the value of the SatelliteNavigationTypes property
	 */
	@Override
	public AspectPropertyValue<List<String>> getSatelliteNavigationTypes() { return getList("satellitenavigationtypes", String.class); }
	/**
	 * Indicates the diagonal size of the device's screen in inches, to a maximum of two decimal points. Where screens have curved corners, the actual viewable area may be less.
	 *
	 * @return the value of the ScreenInchesDiagonal property
	 */
	@Override
	public AspectPropertyValue<Double> getScreenInchesDiagonal() { return getValue("screeninchesdiagonal", Double.class); }
	/**
	 * Indicates the diagonal size of the device's screen in inches rounded to the nearest whole number. This property will return the value 'Unknown' for desktop or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenInchesDiagonalRounded property
	 */
	@Override
	public AspectPropertyValue<Integer> getScreenInchesDiagonalRounded() { return getValue("screeninchesdiagonalrounded", Integer.class); }
	/**
	 * Refers to the height of the device's screen in inches. This property will return 'Unknown' for desktops or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenInchesHeight property
	 */
	@Override
	public AspectPropertyValue<Double> getScreenInchesHeight() { return getValue("screeninchesheight", Double.class); }
	/**
	 * Indicates the area of the device's screen in square inches rounded to the nearest whole number. This property will return the value 'Unknown' for desktop or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenInchesSquare property
	 */
	@Override
	public AspectPropertyValue<Integer> getScreenInchesSquare() { return getValue("screeninchessquare", Integer.class); }
	/**
	 * Refers to the width of the device's screen in inches. This property will return the value 'Unknown' for desktop or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenInchesWidth property
	 */
	@Override
	public AspectPropertyValue<Double> getScreenInchesWidth() { return getValue("screenincheswidth", Double.class); }
	/**
	 * Refers to the diagonal size of the screen of the device in millimetres. This property will return 'Unknown' for desktops or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenMMDiagonal property
	 */
	@Override
	public AspectPropertyValue<Double> getScreenMMDiagonal() { return getValue("screenmmdiagonal", Double.class); }
	/**
	 * Indicate the diagonal size of the device's screen in millimetres rounded to the nearest whole number. This property will return the value 'Unknown' for desktop or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenMMDiagonalRounded property
	 */
	@Override
	public AspectPropertyValue<Integer> getScreenMMDiagonalRounded() { return getValue("screenmmdiagonalrounded", Integer.class); }
	/**
	 * Refers to the screen height of the device in millimetres. This property will return 'Unknown' for desktops or for devices which do not have an integrated screen. For devices such as tablets or TV which are predominantly used in landscape mode, the screen height will be the smaller value compared to the screen width.
	 *
	 * @return the value of the ScreenMMHeight property
	 */
	@Override
	public AspectPropertyValue<Double> getScreenMMHeight() { return getValue("screenmmheight", Double.class); }
	/**
	 * Indicates the area of the device's screen in square millimetres rounded to the nearest whole number. This property will return the value  'Unknown' for desktop or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenMMSquare property
	 */
	@Override
	public AspectPropertyValue<Integer> getScreenMMSquare() { return getValue("screenmmsquare", Integer.class); }
	/**
	 * Refers to the screen width of the device in millimetres. This property will return 'Unknown' for desktops or for devices which do not have an integrated screen. For devices such as tablets or TV which are predominantly used in landscape mode, the screen height will be the smaller value compared to the screen width.
	 *
	 * @return the value of the ScreenMMWidth property
	 */
	@Override
	public AspectPropertyValue<Double> getScreenMMWidth() { return getValue("screenmmwidth", Double.class); }
	/**
	 * Indicates the height of the device's screen in pixels.This property is not applicable for a device that does not have a screen. For devices such as tablets or TV which are predominantly used in landscape mode, the pixel height will be the smaller value compared to the pixel width.
	 *
	 * @return the value of the ScreenPixelsHeight property
	 */
	@Override
	public AspectPropertyValue<Integer> getScreenPixelsHeight() { return getValue("screenpixelsheight", Integer.class); }
	/**
	 * JavaScript that can override the property value found by the server using information on the client device. This property is applicable for browsers that support screen pixels height cookie.
	 *
	 * @return the value of the ScreenPixelsHeightJavaScript property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getScreenPixelsHeightJavaScript() { return getValue("screenpixelsheightjavascript", JavaScript.class); }
	/**
	 * Indicates the height of the device's screen in physical pixels. This property is not applicable for a device that does not have a screen. For devices such as tablets or TV which are predominantly used in landscape mode, the pixel height will be the smaller value compared to the pixel width. 
	 *
	 * @return the value of the ScreenPixelsPhysicalHeight property
	 */
	@Override
	public AspectPropertyValue<Integer> getScreenPixelsPhysicalHeight() { return getValue("screenpixelsphysicalheight", Integer.class); }
	/**
	 * Indicates the width of the device's screen in physical pixels. This property is not applicable for a device that does not have a screen. For devices such as tablets or TV which are predominantly used in landscape mode, the pixel width will be the larger value compared to the pixel height.
	 *
	 * @return the value of the ScreenPixelsPhysicalWidth property
	 */
	@Override
	public AspectPropertyValue<Integer> getScreenPixelsPhysicalWidth() { return getValue("screenpixelsphysicalwidth", Integer.class); }
	/**
	 * Indicates the width of the device's screen in pixels. This property is not applicable for a device that does not have a screen. For devices such as tablets or TV which are predominantly used in landscape mode, the pixel width will be the larger value compared to the pixel height.
	 *
	 * @return the value of the ScreenPixelsWidth property
	 */
	@Override
	public AspectPropertyValue<Integer> getScreenPixelsWidth() { return getValue("screenpixelswidth", Integer.class); }
	/**
	 * JavaScript that can override the property value found by the server using information on the client device. This property is applicable for browsers that support screen pixels width cookie. 
	 *
	 * @return the value of the ScreenPixelsWidthJavaScript property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getScreenPixelsWidthJavaScript() { return getValue("screenpixelswidthjavascript", JavaScript.class); }
	/**
	 * Indicates the screen type of the device. This property is not applicable for a device that does not have an integrated screen, e.g. a media hub. If the device manufacturer or vendor does not specify what the screen type of the device is then it is assumed the device has an LCD screen.
	 *
	 * @return the value of the ScreenType property
	 */
	@Override
	public AspectPropertyValue<String> getScreenType() { return getValue("screentype", String.class); }
	/**
	 * Indicates the resolution of the device's second back camera in megapixels.
	 *
	 * @return the value of the SecondBackCameraMegaPixels property
	 */
	@Override
	public AspectPropertyValue<Double> getSecondBackCameraMegaPixels() { return getValue("secondbackcameramegapixels", Double.class); }
	/**
	 * Indicates the resolution of the device's second front camera in megapixels.
	 *
	 * @return the value of the SecondFrontCameraMegaPixels property
	 */
	@Override
	public AspectPropertyValue<Double> getSecondFrontCameraMegaPixels() { return getValue("secondfrontcameramegapixels", Double.class); }
	/**
	 * Indicates the diagonal size of the device's second screen in inches. This property is not applicable for a device that does not have a second screen.
	 *
	 * @return the value of the SecondScreenInchesDiagonal property
	 */
	@Override
	public AspectPropertyValue<Double> getSecondScreenInchesDiagonal() { return getValue("secondscreeninchesdiagonal", Double.class); }
	/**
	 * Indicates the diagonal size of the device's second screen in inches rounded to the nearest whole number. This property will return the value 'N/A' for desktop or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenInchesDiagonalRounded property
	 */
	@Override
	public AspectPropertyValue<Integer> getSecondScreenInchesDiagonalRounded() { return getValue("secondscreeninchesdiagonalrounded", Integer.class); }
	/**
	 * Refers to the height of the device's second screen in inches. This property will return 'N/A' for desktops or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenInchesHeight property
	 */
	@Override
	public AspectPropertyValue<Double> getSecondScreenInchesHeight() { return getValue("secondscreeninchesheight", Double.class); }
	/**
	 * Indicates the area of the device's second screen in square inches rounded to the nearest whole number. This property will return the value 'N/A' for desktop or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenInchesSquare property
	 */
	@Override
	public AspectPropertyValue<Integer> getSecondScreenInchesSquare() { return getValue("secondscreeninchessquare", Integer.class); }
	/**
	 * Refers to the width of the device's second screen in inches. This property will return the value 'N/A' for desktop or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenInchesWidth property
	 */
	@Override
	public AspectPropertyValue<Double> getSecondScreenInchesWidth() { return getValue("secondscreenincheswidth", Double.class); }
	/**
	 * Refers to the diagonal size of the second screen of the device in millimetres. This property will return 'N/A' for desktops or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenMMDiagonal property
	 */
	@Override
	public AspectPropertyValue<Double> getSecondScreenMMDiagonal() { return getValue("secondscreenmmdiagonal", Double.class); }
	/**
	 * Indicate the diagonal size of the device's second screen in millimetres rounded to the nearest whole number. This property will return the value 'N/A' for desktop or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenMMDiagonalRounded property
	 */
	@Override
	public AspectPropertyValue<Integer> getSecondScreenMMDiagonalRounded() { return getValue("secondscreenmmdiagonalrounded", Integer.class); }
	/**
	 * Refers to the second screen height of the device in millimetres. This property will return 'N/A' for desktops or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenMMHeight property
	 */
	@Override
	public AspectPropertyValue<Double> getSecondScreenMMHeight() { return getValue("secondscreenmmheight", Double.class); }
	/**
	 * Indicates the area of the device's second screen in square millimetres rounded to the nearest whole number. This property will return the value  'N/A' for desktop or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenMMSquare property
	 */
	@Override
	public AspectPropertyValue<Integer> getSecondScreenMMSquare() { return getValue("secondscreenmmsquare", Integer.class); }
	/**
	 * Refers to the second screen width of the device in millimetres. This property will return 'N/A' for desktops or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenMMWidth property
	 */
	@Override
	public AspectPropertyValue<Double> getSecondScreenMMWidth() { return getValue("secondscreenmmwidth", Double.class); }
	/**
	 * Indicates the height of the device's second screen in pixels. This property is not applicable for a device that does not have a second screen.
	 *
	 * @return the value of the SecondScreenPixelsHeight property
	 */
	@Override
	public AspectPropertyValue<Integer> getSecondScreenPixelsHeight() { return getValue("secondscreenpixelsheight", Integer.class); }
	/**
	 * Indicates the width of the device's second screen in pixels. This property is not applicable for a device that does not have a second screen.
	 *
	 * @return the value of the SecondScreenPixelsWidth property
	 */
	@Override
	public AspectPropertyValue<Integer> getSecondScreenPixelsWidth() { return getValue("secondscreenpixelswidth", Integer.class); }
	/**
	 * Indicates if the browser supports the querySelector() method that returns the first element matching a specified CSS selector(s) in the document.
	 *
	 * @return the value of the Selector property
	 * @see <a href="http://www.w3.org/TR/selectors-api2/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getSelector() { return getValue("selector", Boolean.class); }
	/**
	 * Contains the Accept-CH HTTP header values to add to the HTTP response for the browser component. UACH values Sec-CH-UA, and Sec-CH-UA-Full-Version are relevant. The default value is Unknown if the browser does not fully support UACH.
	 *
	 * @return the value of the SetHeaderBrowserAccept-CH property
	 * @see <a href="https://learnclienthints.com">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getSetHeaderBrowserAcceptCH() { return getValue("setheaderbrowseraccept-ch", String.class); }
	/**
	 * Contains the Accept-CH HTTP header values to add to the HTTP response for the hardware component. UACH values Sec-CH-UA-Model, and Sec-CH-UA-Mobile are relevant. The default value is Unknown if the browser does not fully support UACH.
	 *
	 * @return the value of the SetHeaderHardwareAccept-CH property
	 * @see <a href="https://learnclienthints.com">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getSetHeaderHardwareAcceptCH() { return getValue("setheaderhardwareaccept-ch", String.class); }
	/**
	 * Contains the Accept-CH HTTP header values to add to the HTTP response for the platform component. UACH values Sec-CH-UA-Platform, and Sec-CH-UA-Platform-Version are relevant. The default value is Unknown if the browser does not fully support UACH.
	 *
	 * @return the value of the SetHeaderPlatformAccept-CH property
	 * @see <a href="https://learnclienthints.com">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getSetHeaderPlatformAcceptCH() { return getValue("setheaderplatformaccept-ch", String.class); }
	/**
	 * Indicates the primary marketing name of the System on Chip (chipset) which includes the CPU, GPU and modem. e.g. Snapdragon S4
	 *
	 * @return the value of the SoC property
	 */
	@Override
	public AspectPropertyValue<String> getSoC() { return getValue("soc", String.class); }
	/**
	 * Indicates the Semiconductor Company that designed the System on Chip (chipset) e.g. Qualcomm, Intel or Mediatek.
	 *
	 * @return the value of the SoCDesigner property
	 */
	@Override
	public AspectPropertyValue<String> getSoCDesigner() { return getValue("socdesigner", String.class); }
	/**
	 * Indicates the official model of the System on Chip (chipset) e.g. MSM8625, MT8312.
	 *
	 * @return the value of the SoCModel property
	 */
	@Override
	public AspectPropertyValue<String> getSoCModel() { return getValue("socmodel", String.class); }
	/**
	 * Refers to the list of audio codecs supported by an operating system. This list of codecs is supported for playback on a  basic software installation. The values of this property are the codec's common name.
	 *
	 * @return the value of the SoftwareAudioCodecsDecode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getSoftwareAudioCodecsDecode() { return getList("softwareaudiocodecsdecode", String.class); }
	/**
	 * Refers to the list of audio codecs supported by an operating system. This list of codecs is supported for capture on a basic software installation. The values of this property are the codec's common name.
	 *
	 * @return the value of the SoftwareAudioCodecsEncode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getSoftwareAudioCodecsEncode() { return getList("softwareaudiocodecsencode", String.class); }
	/**
	 * Refers to the list of video codecs supported by an operating system. This list of codecs is supported for playback on a  basic software installation. The values of this property are the codec's common name.
	 *
	 * @return the value of the SoftwareVideoCodecsDecode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getSoftwareVideoCodecsDecode() { return getList("softwarevideocodecsdecode", String.class); }
	/**
	 * Refers to the list of video codecs supported by an operating system. This list of codecs is supported for capture on a basic software installation. The values of this property are the codec's common name.
	 *
	 * @return the value of the SoftwareVideoCodecsEncode property
	 */
	@Override
	public AspectPropertyValue<List<String>> getSoftwareVideoCodecsEncode() { return getList("softwarevideocodecsencode", String.class); }
	/**
	 * The Specific Absorbtion Rate (SAR) is a measure of the rate at which energy is absorbed by the human body when exposed by a radio frequency electromagnetic field. This property contains values in Watts per Kilogram (W/kg) in accordance with the European Committee for Electrotechnical Standardization (CENELEC).
	 *
	 * @return the value of the SpecificAbsorbtionRateEU property
	 */
	@Override
	public AspectPropertyValue<Double> getSpecificAbsorbtionRateEU() { return getValue("specificabsorbtionrateeu", Double.class); }
	/**
	 * The Specific Absorbtion Rate (SAR) is a measure of the rate at which energy is absorbed by the human body when exposed by a radio frequency electromagnetic field. This property contains values in Watts per Kilogram (W/kg) in accordance with the Federal Communications Commission (FCC).
	 *
	 * @return the value of the SpecificAbsorbtionRateUS property
	 */
	@Override
	public AspectPropertyValue<Double> getSpecificAbsorbtionRateUS() { return getValue("specificabsorbtionrateus", Double.class); }
	/**
	 * A list of MIME types the device can stream. The list does not include MIME types that are only supported through the use of 3rd party applications.
	 *
	 * @return the value of the StreamingAccept property
	 */
	@Override
	public AspectPropertyValue<List<String>> getStreamingAccept() { return getList("streamingaccept", String.class); }
	/**
	 * Refers to the suggested optimum height of a button in millimetres. Ensures the button is touchable on a touch screen and not too large on a non-touch screen. Assumes the actual device DPI (Dots Per Inch) is being used. 
	 *
	 * @return the value of the SuggestedImageButtonHeightMms property
	 */
	@Override
	public AspectPropertyValue<Double> getSuggestedImageButtonHeightMms() { return getValue("suggestedimagebuttonheightmms", Double.class); }
	/**
	 * Refers to the suggested optimum height of a button in millimetres. Ensures the button is touchable on a touch screen and not too large on a non-touch screen. 
	 *
	 * @return the value of the SuggestedImageButtonHeightPixels property
	 */
	@Override
	public AspectPropertyValue<Double> getSuggestedImageButtonHeightPixels() { return getValue("suggestedimagebuttonheightpixels", Double.class); }
	/**
	 * Refers to the suggested optimum height of a hyperlink in pixels. Ensures the link is touchable on a touch screen and not too large on a non-touch screen. Assumes the actual device DPI is being used.
	 *
	 * @return the value of the SuggestedLinkSizePixels property
	 */
	@Override
	public AspectPropertyValue<Double> getSuggestedLinkSizePixels() { return getValue("suggestedlinksizepixels", Double.class); }
	/**
	 * Refers to the suggested optimum height of a hyperlink in points. Ensures the link is touchable on a touch screen and not too large on a non-touch screen. 
	 *
	 * @return the value of the SuggestedLinkSizePoints property
	 */
	@Override
	public AspectPropertyValue<Double> getSuggestedLinkSizePoints() { return getValue("suggestedlinksizepoints", Double.class); }
	/**
	 * Indicates the list of wireless data technologies supported by the device, including Bluetooth and Wi-Fi. For example, 4G cellular network technologies includes 'LTE' (Long Term Evolution), and 5G technologies includes 'NR' (New Radio). If the device supports phone calls, the SMS value is also returned.
	 *
	 * @return the value of the SupportedBearers property
	 * @see <a href="http://en.wikipedia.org/wiki/Wireless_Application_Protocol">More information</a>
	 */
	@Override
	public AspectPropertyValue<List<String>> getSupportedBearers() { return getList("supportedbearers", String.class); }
	/**
	 * Indicates the highest version of Bluetooth the device supports.
	 *
	 * @return the value of the SupportedBluetooth property
	 */
	@Override
	public AspectPropertyValue<Double> getSupportedBluetooth() { return getValue("supportedbluetooth", Double.class); }
	/**
	 * Indicates the Bluetooth profiles the device supports.
	 *
	 * @return the value of the SupportedBluetoothProfiles property
	 * @see <a href="https://en.wikipedia.org/wiki/List_of_Bluetooth_profiles">More information</a>
	 */
	@Override
	public AspectPropertyValue<List<String>> getSupportedBluetoothProfiles() { return getList("supportedbluetoothprofiles", String.class); }
	/**
	 * This Property is no longer being supported. Please see Properties, SupportedBluetooth and SupportedBluetoothProfiles for the relevant data.
	 *
	 * @return the value of the SupportedBluetoothVersion property
	 * @see <a href="http://en.wikipedia.org/wiki/Bluetooth">More information</a>
	 */
	@Override
	public AspectPropertyValue<String> getSupportedBluetoothVersion() { return getValue("supportedbluetoothversion", String.class); }
	/**
	 * Indicates the list of features the device's camera supports.
	 *
	 * @return the value of the SupportedCameraFeatures property
	 */
	@Override
	public AspectPropertyValue<List<String>> getSupportedCameraFeatures() { return getList("supportedcamerafeatures", String.class); }
	/**
	 * Indicates the list of charger types supported by the device. For devices that operate via mains power only, e.g. TVs, MediaHubs (which technically aren't being charged) this property is not applicable.
	 *
	 * @return the value of the SupportedChargerTypes property
	 */
	@Override
	public AspectPropertyValue<List<String>> getSupportedChargerTypes() { return getList("supportedchargertypes", String.class); }
	/**
	 * Indicates the list of input and output communications the device can support, for example 3.5mm jack, micro-USB etc.
	 *
	 * @return the value of the SupportedI/O property
	 */
	@Override
	public AspectPropertyValue<List<String>> getSupportedIO() { return getList("supportedi/o", String.class); }
	/**
	 * Indicates the list of sensors supported by the device. This property may be not applicable for devices without sensors, such as most feature phones and media hubs.
	 *
	 * @return the value of the SupportedSensorTypes property
	 */
	@Override
	public AspectPropertyValue<List<String>> getSupportedSensorTypes() { return getList("supportedsensortypes", String.class); }
	/**
	 * Indicates whether the &quot;Universal Integrated Circuit Card (UICC - more commonly known as, SIM)&quot; is removable or embedded. If removable, the form factor of the UICC is returned.
	 *
	 * @return the value of the SupportedSIMCardTypes property
	 */
	@Override
	public AspectPropertyValue<List<String>> getSupportedSIMCardTypes() { return getList("supportedsimcardtypes", String.class); }
	/**
	 * Indicates if the device supports 24p; a video format that operates at 24 frames per second.
	 *
	 * @return the value of the Supports24p property
	 */
	@Override
	public AspectPropertyValue<Boolean> getSupports24p() { return getValue("supports24p", Boolean.class); }
	/**
	 * Indicates if the device can receive and make telephone calls using available bearers without any additional software such as VoIP. Devices that support voice calls do not necessarily support phone calls.
	 *
	 * @return the value of the SupportsPhoneCalls property
	 */
	@Override
	public AspectPropertyValue<Boolean> getSupportsPhoneCalls() { return getValue("supportsphonecalls", Boolean.class); }
	/**
	 * Indicates if the browser supports TLS or SSL, essential for secure protocols such as HTTPS.
	 *
	 * @return the value of the SupportsTls/Ssl property
	 */
	@Override
	public AspectPropertyValue<Boolean> getSupportsTlsSsl() { return getValue("supportstls/ssl", Boolean.class); }
	/**
	 * Indicates if the browser supports WebGL technology to generate hardware-accelerated 3D graphics.
	 *
	 * @return the value of the SupportsWebGL property
	 */
	@Override
	public AspectPropertyValue<Boolean> getSupportsWebGL() { return getValue("supportswebgl", Boolean.class); }
	/**
	 * Indicates if the device supports Wireless Display Technology.
	 *
	 * @return the value of the SupportsWiDi property
	 */
	@Override
	public AspectPropertyValue<Boolean> getSupportsWiDi() { return getValue("supportswidi", Boolean.class); }
	/**
	 * Indicates if the browser supports SVG (scalable vector graphics), useful for 2D animations and applications where all objects within the SVG can be accessed via the DOM and can have assigned event listener elements.
	 *
	 * @return the value of the Svg property
	 * @see <a href="http://www.w3.org/TR/SVG/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getSvg() { return getValue("svg", Boolean.class); }
	/**
	 * Indicates the Type Allocation Code (TAC) for devices supporting GSM/3GPP networks which come from multiple sources. This property will return 'N/A' if we cannot determine the device TAC authenticy.
	 *
	 * @return the value of the TAC property
	 */
	@Override
	public AspectPropertyValue<List<String>> getTAC() { return getList("tac", String.class); }
	/**
	 * Indicates whether third party cookies are enabled. Where the JavaScript contained in ThirdPartyCookiesEnabledJavaScript has not run this will indicate the likely status.
	 *
	 * @return the value of the ThirdPartyCookiesEnabled property
	 */
	@Override
	public AspectPropertyValue<String> getThirdPartyCookiesEnabled() { return getValue("thirdpartycookiesenabled", String.class); }
	/**
	 * Contains JavaScript that can override the property ThirdPartyCookiesEnabled. This property returns NotSupported for browsers which cannot support third party cookies.
	 *
	 * @return the value of the ThirdPartyCookiesEnabledJavaScript property
	 */
	@Override
	public AspectPropertyValue<JavaScript> getThirdPartyCookiesEnabledJavaScript() { return getValue("thirdpartycookiesenabledjavascript", JavaScript.class); }
	/**
	 * Indicates if the browser supports the method of registering and interpreting finder (or stylus) activity on touch screens or trackpads.
	 *
	 * @return the value of the TouchEvents property
	 * @see <a href="http://www.w3.org/TR/touch-events/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getTouchEvents() { return getValue("touchevents", Boolean.class); }
	/**
	 * Indicates if the browser supports a method of tracking text being played with media, e.g. subtitles and captions.
	 *
	 * @return the value of the Track property
	 * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html#the-track-element">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getTrack() { return getValue("track", Boolean.class); }
	/**
	 * The matched User-Agents.
	 *
	 * @return the value of the UserAgents property
	 */
	@Override
	public AspectPropertyValue<List<String>> getUserAgents() { return getList("useragents", String.class); }
	/**
	 * Indicates if the browser supports the 'Video' element for playing videos on web pages without requiring a plug-in.
	 *
	 * @return the value of the Video property
	 * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getVideo() { return getValue("video", Boolean.class); }
	/**
	 * Indicates if the browser supports Viewport, to give control over view for different screen sizes and resolutions of devices accessing a website.
	 *
	 * @return the value of the Viewport property
	 * @see <a href="http://www.w3.org/TR/mwabp/#bp-viewport">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getViewport() { return getValue("viewport", Boolean.class); }
	/**
	 * Indicates if the browser supports the WebP image format.
	 *
	 * @return the value of the WebP property
	 */
	@Override
	public AspectPropertyValue<Boolean> getWebP() { return getValue("webp", Boolean.class); }
	/**
	 * Indicates if the browser supports background workers in JavaScript.
	 *
	 * @return the value of the WebWorkers property
	 * @see <a href="http://www.w3.org/TR/workers/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getWebWorkers() { return getValue("webworkers", Boolean.class); }
	/**
	 * Indicates the weight of the device with battery in grams.
	 *
	 * @return the value of the WeightWithBattery property
	 */
	@Override
	public AspectPropertyValue<Double> getWeightWithBattery() { return getValue("weightwithbattery", Double.class); }
	/**
	 * Indicates the weight of the device without battery in grams.
	 *
	 * @return the value of the WeightWithoutBattery property
	 */
	@Override
	public AspectPropertyValue<Double> getWeightWithoutBattery() { return getValue("weightwithoutbattery", Double.class); }
	/**
	 * Indicates if the browser supports client-to-server communication with XmlHttpRequests. If the browser supports 'Xhr2' will also support 'DataForm' element. This property may need a vendor prefix, e.g. webkit, moz, etc.
	 *
	 * @return the value of the Xhr2 property
	 * @see <a href="http://www.w3.org/TR/XMLHttpRequest/">More information</a>
	 */
	@Override
	public AspectPropertyValue<Boolean> getXhr2() { return getValue("xhr2", Boolean.class); }
}
