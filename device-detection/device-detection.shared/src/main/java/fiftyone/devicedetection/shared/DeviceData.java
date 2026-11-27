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
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import java.util.List;
// This interface sits at the top of the name space in order to make
// life easier for consumers.
/**
 * Interface exposing typed accessors for properties related to a device
 * returned by a device detection engine.
 */
public interface DeviceData extends AspectData
{
	/**
	 * Indicates what ajax request format should be used.
	 *
	 * @return the value of the AjaxRequestType property
	 */
	AspectPropertyValue<String> getAjaxRequestType();
	/**
	 * Indicates if the browser supports 'window.requestAnimationFrame()' method.
	 *
	 * @return the value of the AnimationTiming property
	 * @see <a href="http://www.w3.org/TR/animation-timing/">More information</a>
	 */
	AspectPropertyValue<Boolean> getAnimationTiming();
	/**
	 * Indicates the resolution of the device's back camera in megapixels. For a device that has a rotating camera the same value is returned for front and back megapixels properties.
	 *
	 * @return the value of the BackCameraMegaPixels property
	 */
	AspectPropertyValue<Double> getBackCameraMegaPixels();
	/**
	 * Indicates the capacity of the device's standard battery in mAh. This property is not applicable for a device that does not have a battery.
	 *
	 * @return the value of the BatteryCapacity property
	 */
	AspectPropertyValue<Integer> getBatteryCapacity();
	/**
	 * Indicates the number of bits used to describe the colour of each individual pixel, also known as bit depth or colour depth.
	 *
	 * @return the value of the BitsPerPixel property
	 */
	AspectPropertyValue<Integer> getBitsPerPixel();
	/**
	 * Indicates if the browser fully supports BlobBuilder, containing a BlobBuilder interface, a FileSaver interface, a FileWriter interface, and a FileWriterSync interface.
	 *
	 * @return the value of the BlobBuilder property
	 * @see <a href="http://dev.w3.org/2009/dap/file-system/file-writer.html#the-blobbuilder-interface">More information</a>
	 */
	AspectPropertyValue<Boolean> getBlobBuilder();
	/**
	 * Refers to the list of audio codecs in specific formats supported for Decode by the Web Browser. This list of codecs is supported for playback on a basic browser installation.
	 *
	 * @return the value of the BrowserAudioCodecsDecode property
	 */
	AspectPropertyValue<List<String>> getBrowserAudioCodecsDecode();
	/**
	 * Refers to the list of audio codecs in specific formats supported for Encode by the Web Browser. This list of codecs is supported for capture on a basic browser installation.
	 *
	 * @return the value of the BrowserAudioCodecsEncode property
	 */
	AspectPropertyValue<List<String>> getBrowserAudioCodecsEncode();
	/**
	 * Indicates the age in months of the browser since the BrowserDiscontinuedYear and BrowserDiscontinuedMonth.
	 *
	 * @return the value of the BrowserDiscontinuedAge property
	 */
	AspectPropertyValue<Integer> getBrowserDiscontinuedAge();
	/**
	 * The month in which further development of the browser version is stopped by the browser vendor. This occurs when a new stable version of the browser is released.
	 *
	 * @return the value of the BrowserDiscontinuedMonth property
	 */
	AspectPropertyValue<String> getBrowserDiscontinuedMonth();
	/**
	 * The year in which further development of the browser version is stopped by the browser vendor. This occurs when a new stable version of the browser is released.
	 *
	 * @return the value of the BrowserDiscontinuedYear property
	 */
	AspectPropertyValue<Integer> getBrowserDiscontinuedYear();
	/**
	 * Indicates the name of the browser without the default OS or layout engine.
	 *
	 * @return the value of the BrowserFamily property
	 */
	AspectPropertyValue<String> getBrowserFamily();
	/**
	 * A list of logos associated with the Browser. The string contains the caption, followed by the full image URL separated with a tab character.
	 *
	 * @return the value of the BrowserLogos property
	 */
	AspectPropertyValue<List<String>> getBrowserLogos();
	/**
	 * Indicates the name of the browser. Many mobile browsers, by default, come with an operating system (OS). Unless specifically named, these browsers are named after the accompanying OS and/or the layout engine. 
	 *
	 * @return the value of the BrowserName property
	 * @see <a href="http://en.wikipedia.org/wiki/Web_browser">More information</a>
	 */
	AspectPropertyValue<String> getBrowserName();
	/**
	 * Indicates the age in months of the browser since the BrowserPreviewYear and BrowserPreviewMonth.
	 *
	 * @return the value of the BrowserPreviewAge property
	 */
	AspectPropertyValue<Integer> getBrowserPreviewAge();
	/**
	 * The month in which the browser version is originally released as a Beta/Developer version by the browser vendor. This is before it is officially released as a stable version, to ensure wider testing by the community can take place.
	 *
	 * @return the value of the BrowserPreviewMonth property
	 */
	AspectPropertyValue<String> getBrowserPreviewMonth();
	/**
	 * The year in which the browser version is originally released as a Beta/Developer version by the browser vendor. This is before it is officially released as a stable version, to ensure wider testing by the community can take place.
	 *
	 * @return the value of the BrowserPreviewYear property
	 */
	AspectPropertyValue<Integer> getBrowserPreviewYear();
	/**
	 * Indicates the source from which browser properties have been validated. Primary browser data are retrieved from the internal test and populated manually, then they might be validated against an external source such as Caniuse or RingMark. 
	 *
	 * @return the value of the BrowserPropertySource property
	 */
	AspectPropertyValue<String> getBrowserPropertySource();
	/**
	 * A measure of the popularity of this browser version. All browsers are ordered by the number of events associated with that browser that occurred in the sampling period. The browser with the most events is ranked 1, the second 2 and so on.
	 *
	 * @return the value of the BrowserRank property
	 */
	AspectPropertyValue<Integer> getBrowserRank();
	/**
	 * Indicates the age in months of the browser since the BrowserReleaseYear and BrowserReleaseMonth.
	 *
	 * @return the value of the BrowserReleaseAge property
	 */
	AspectPropertyValue<Integer> getBrowserReleaseAge();
	/**
	 * The month in which the browser version is officially released to users by the browser vendor. This version is called the stable version as any bugs or difficulties highlighted in the Beta/Developer Version will have been fixed for this release.
	 *
	 * @return the value of the BrowserReleaseMonth property
	 */
	AspectPropertyValue<String> getBrowserReleaseMonth();
	/**
	 * The year in which the browser version is officially released to users by the browser vendor. This version is called the stable version as any bugs or difficulties highlighted in the Beta/Developer Version will have been fixed for this release.
	 *
	 * @return the value of the BrowserReleaseYear property
	 */
	AspectPropertyValue<Integer> getBrowserReleaseYear();
	/**
	 * Name of the underlying browser source project.
	 *
	 * @return the value of the BrowserSourceProject property
	 */
	AspectPropertyValue<String> getBrowserSourceProject();
	/**
	 * Indicates the version or subversion of the underlying browser source project.
	 *
	 * @return the value of the BrowserSourceProjectVersion property
	 */
	AspectPropertyValue<String> getBrowserSourceProjectVersion();
	/**
	 * Indicates the name of the company which created the browser.
	 *
	 * @return the value of the BrowserVendor property
	 * @see <a href="http://reference.sitepoint.com/css/vendorspecific">More information</a>
	 */
	AspectPropertyValue<String> getBrowserVendor();
	/**
	 * Indicates the version or subversion of the browser.
	 *
	 * @return the value of the BrowserVersion property
	 * @see <a href="http://www.webdevelopersnotes.com/how-do-i/check-browser-version.php">More information</a>
	 */
	AspectPropertyValue<String> getBrowserVersion();
	/**
	 * Refers to the list of video codecs in specific formats supported for Decode by the Web Browser. This list of codecs is supported for playback on a basic browser installation.
	 *
	 * @return the value of the BrowserVideoCodecsDecode property
	 */
	AspectPropertyValue<List<String>> getBrowserVideoCodecsDecode();
	/**
	 * Refers to the list of video codecs in specific formats supported for Encode by the Web Browser. This list of codecs is supported for capture on a basic browser installation.
	 *
	 * @return the value of the BrowserVideoCodecsEncode property
	 */
	AspectPropertyValue<List<String>> getBrowserVideoCodecsEncode();
	/**
	 * Indicates the list of camera types the device has. If the device has a rotating camera, this property refers to both front and back facing cameras.
	 *
	 * @return the value of the CameraTypes property
	 */
	AspectPropertyValue<List<String>> getCameraTypes();
	/**
	 * Indicates if the browser supports the canvas element, useful for drawing graphics via scripting (usually JavaScript).
	 *
	 * @return the value of the Canvas property
	 * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/multipage/the-canvas-element.html">More information</a>
	 */
	AspectPropertyValue<Boolean> getCanvas();
	/**
	 * Stands for Composite Capability/Preference Profiles.  Refers to the list of MIME types supported by the operating system. The list does not include MIME types that are only enabled through the use of 3rd party applications.
	 *
	 * @return the value of the CcppAccept property
	 * @see <a href="http://en.wikipedia.org/wiki/Composite_Capability/Preference_Profiles">More information</a>
	 */
	AspectPropertyValue<List<String>> getCcppAccept();
	/**
	 * Indicates which version of the Connected Limited Device Configuration the device supports for use with Java ME.
	 *
	 * @return the value of the CLDC property
	 * @see <a href="http://java.sun.com/products/cldc/">More information</a>
	 */
	AspectPropertyValue<Double> getCLDC();
	/**
	 * Indicates the contrast ratio of the device.
	 *
	 * @return the value of the ContrastRatio property
	 */
	AspectPropertyValue<String> getContrastRatio();
	/**
	 * Indicates if the browser supports http Cookies. However, the user may have disabled Cookies in their own configuration. Where data cannot be validated, it is assumed that the browser supports cookies.
	 *
	 * @return the value of the CookiesCapable property
	 * @see <a href="http://en.wikipedia.org/wiki/HTTP_cookie">More information</a>
	 */
	AspectPropertyValue<Boolean> getCookiesCapable();
	/**
	 * Indicates the official name of the CPU within the SoC, e.g. ARM Cortex A9 or Krait (Qualcomm).
	 *
	 * @return the value of the CPU property
	 */
	AspectPropertyValue<String> getCPU();
	/**
	 * Indicates the number of physical CPU cores the device has.
	 *
	 * @return the value of the CPUCores property
	 */
	AspectPropertyValue<Integer> getCPUCores();
	/**
	 * Indicates the Semiconductor Company that designed the CPU.
	 *
	 * @return the value of the CPUDesigner property
	 */
	AspectPropertyValue<String> getCPUDesigner();
	/**
	 * Indicates the maximum frequency of the CPU of the device in gigahertz (GHz).
	 *
	 * @return the value of the CPUMaximumFrequency property
	 */
	AspectPropertyValue<Double> getCPUMaximumFrequency();
	/**
	 * Indicates the crawler name when applicable. Returns NotCrawler when the device is not a crawler.
	 *
	 * @return the value of the CrawlerName property
	 */
	AspectPropertyValue<String> getCrawlerName();
	/**
	 * Identification tokens, defined by the IETF, that a crawler includes in the User-Agent request header field to identify the crawler software.
	 *
	 * @return the value of the CrawlerProductTokens property
	 */
	AspectPropertyValue<List<String>> getCrawlerProductTokens();
	/**
	 * The url of the web page containing information from the crawler controller about the crawler. Where there is no publicly available information about the crawler, this instead links to their public website.
	 *
	 * @return the value of the CrawlerUrl property
	 */
	AspectPropertyValue<String> getCrawlerUrl();
	/**
	 * Indicates the purpose for which a crawler accesses content, including search, AI indexing, AI model training, and archiving data.
	 *
	 * @return the value of the CrawlerUsage property
	 */
	AspectPropertyValue<List<String>> getCrawlerUsage();
	/**
	 * Indicates if the browser supports CSS3 background properties (such as background-image, background-color, etc.) that allow styling of the border and the background of an object, and create a shadow effect.
	 *
	 * @return the value of the CssBackground property
	 * @see <a href="http://www.w3.org/TR/css3-background/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssBackground();
	/**
	 * Indicates if the browser supports border images, allowing decoration of the border around an object.
	 *
	 * @return the value of the CssBorderImage property
	 * @see <a href="http://www.w3.org/TR/css3-background/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssBorderImage();
	/**
	 * Indicates if the browser can draw CSS images into a Canvas.
	 *
	 * @return the value of the CssCanvas property
	 * @see <a href="http://www.webkit.org/blog/176/css-canvas-drawing/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssCanvas();
	/**
	 * Indicates if the browser supports CSS3 Color, allowing author control of the foreground colour and opacity of an element.
	 *
	 * @return the value of the CssColor property
	 * @see <a href="http://www.w3.org/TR/css3-color/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssColor();
	/**
	 * Indicates if the browser supports CSS3 columns for setting column- width and column-count.
	 *
	 * @return the value of the CssColumn property
	 * @see <a href="http://www.w3schools.com/css3/css3_multiple_columns.asp">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssColumn();
	/**
	 * Indicates if the browser supports flexbox, allowing the automatic reordering of elements on the page when accessed from devices with different screen sizes.
	 *
	 * @return the value of the CssFlexbox property
	 * @see <a href="http://www.w3.org/TR/css3-flexbox/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssFlexbox();
	/**
	 * Indicates if the browser supports CSS3 fonts, including non-standard fonts, e.g. @font-face.
	 *
	 * @return the value of the CssFont property
	 * @see <a href="http://www.w3.org/TR/css3-fonts/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssFont();
	/**
	 * Indicates if the browser supports all CSS grid properties.
	 *
	 * @return the value of the CssGrid property
	 */
	AspectPropertyValue<Boolean> getCssGrid();
	/**
	 * Indicates if the browser supports CSS3 images, allowing for fall-back images, gradients and other effects.
	 *
	 * @return the value of the CssImages property
	 * @see <a href="http://dev.w3.org/csswg/css3-images/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssImages();
	/**
	 * Indicates if the browser supports MediaQueries for dynamic CSS that uses the @media rule.
	 *
	 * @return the value of the CssMediaQueries property
	 * @see <a href="http://www.w3.org/TR/css3-mediaqueries/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssMediaQueries();
	/**
	 * Indicates if the browser supports the CSS 'min-width' and 'max-width' element.
	 *
	 * @return the value of the CssMinMax property
	 * @see <a href="http://www.w3.org/TR/CSS21/visudet.html#min-max-widths">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssMinMax();
	/**
	 * Indicates if the browser supports overflowing of clipped blocks.
	 *
	 * @return the value of the CssOverflow property
	 * @see <a href="http://www.w3.org/TR/CSS2/visufx.html">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssOverflow();
	/**
	 * Indicates if the browser supports CSS position, allowing for different box placement algorithms, e.g. static, relative, absolute, fixed and initial.
	 *
	 * @return the value of the CssPosition property
	 * @see <a href="http://www.w3.org/TR/CSS21/visuren.html#choose-position">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssPosition();
	/**
	 * Indicates if the browser supports all CSS3 text features including: text-overflow, word-wrap and word-break.
	 *
	 * @return the value of the CssText property
	 * @see <a href="http://www.w3.org/TR/css3-text/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssText();
	/**
	 * Indicates if the browser supports 2D transformations in CSS3 including rotating, scaling, etc. This property includes support for both transform and transform-origin properties.
	 *
	 * @return the value of the CssTransforms property
	 * @see <a href="http://www.w3.org/TR/css3-2d-transforms/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssTransforms();
	/**
	 * Indicates if the browser supports CSS3 transitions elements, used for animating changes to properties.
	 *
	 * @return the value of the CssTransitions property
	 * @see <a href="http://www.w3schools.com/css3/css3_transitions.asp">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssTransitions();
	/**
	 * Indicates if the browser supports CSS UI stylings, including text-overflow, css3-boxsizing and pointer properties.
	 *
	 * @return the value of the CssUI property
	 * @see <a href="http://www.w3.org/TR/css3-ui/">More information</a>
	 */
	AspectPropertyValue<Boolean> getCssUI();
	/**
	 * Indicates if the browser has the ability to embed custom data attributes on all HTML elements using the 'data-' prefix.
	 *
	 * @return the value of the DataSet property
	 */
	AspectPropertyValue<Boolean> getDataSet();
	/**
	 * Indicates if the browser allows encoded data to be contained in a URL.
	 *
	 * @return the value of the DataUrl property
	 * @see <a href="http://www.ietf.org/rfc/rfc2397">More information</a>
	 */
	AspectPropertyValue<Boolean> getDataUrl();
	/**
	 * Indicates which app stores are available on the device and come pre-installed by the manufacturer.
	 *
	 * @return the value of the DefaultAppStores property
	 */
	AspectPropertyValue<List<String>> getDefaultAppStores();
	/**
	 * Indicates what certifications apply to this device.
	 *
	 * @return the value of the DeviceCertifications property
	 */
	AspectPropertyValue<List<String>> getDeviceCertifications();
	/**
	 * Consists of four components separated by a hyphen symbol: Hardware-Platform-Browser-IsCrawler where each Component represents an ID of the corresponding Profile.
	 *
	 * @return the value of the DeviceId property
	 */
	AspectPropertyValue<String> getDeviceId();
	/**
	 * Indicates if the browser supports DOM events for device orientation, e.g. 'deviceorientation', 'devicemotion' and 'compassneedscalibration'.
	 *
	 * @return the value of the DeviceOrientation property
	 * @see <a href="http://www.w3.org/TR/orientation-event/">More information</a>
	 */
	AspectPropertyValue<Boolean> getDeviceOrientation();
	/**
	 * Indicates the maximum volatile RAM capacity of the device in megabytes (MB). Where a device has different RAM capacity options, the largest option available is returned.
	 *
	 * @return the value of the DeviceRAM property
	 */
	AspectPropertyValue<Integer> getDeviceRAM();
	/**
	 * Indicates the volatile RAM capacity options for the device in megabytes (MB). If no variants are found, then the value returned will be the same as &quot;DeviceRAM&quot;.
	 *
	 * @return the value of the DeviceRAMVariants property
	 */
	AspectPropertyValue<List<String>> getDeviceRAMVariants();
	/**
	 * Indicates the type of the device based on values set in other properties, such as IsMobile, IsTablet, IsSmartphone, IsSmallScreen etc.
	 *
	 * @return the value of the DeviceType property
	 */
	AspectPropertyValue<String> getDeviceType();
	/**
	 * Used when detection method is not Exact or None. This is an integer value and the larger the value the less confident the detector is in this result.
	 *
	 * @return the value of the Difference property
	 */
	AspectPropertyValue<Integer> getDifference();
	/**
	 * Total difference in character positions where the substrings hashes were found away from where they were expected.
	 *
	 * @return the value of the Drift property
	 */
	AspectPropertyValue<Integer> getDrift();
	/**
	 * Indicates the device's Ingress Protection Rating against dust and water (http://en.wikipedia.org/wiki/IP_Code).
	 *
	 * @return the value of the Durability property
	 */
	AspectPropertyValue<List<String>> getDurability();
	/**
	 * Indicates the dynamic contrast ratio of the device's screen.
	 *
	 * @return the value of the DynamicContrastRatio property
	 */
	AspectPropertyValue<String> getDynamicContrastRatio();
	/**
	 * Indicates the annual energy consumption of the device per year in kWh.
	 *
	 * @return the value of the EnergyConsumptionPerYear property
	 */
	AspectPropertyValue<Integer> getEnergyConsumptionPerYear();
	/**
	 * Indicates the maximum amount of memory in gigabytes (GB) the expansion slot of the device can support.
	 *
	 * @return the value of the ExpansionSlotMaxSize property
	 */
	AspectPropertyValue<Integer> getExpansionSlotMaxSize();
	/**
	 * Indicates the expansion slot type the device can support.
	 *
	 * @return the value of the ExpansionSlotType property
	 */
	AspectPropertyValue<List<String>> getExpansionSlotType();
	/**
	 * Indicates if the browser supports the Fetch API.
	 *
	 * @return the value of the Fetch property
	 */
	AspectPropertyValue<Boolean> getFetch();
	/**
	 * Indicates if the browser supports file reading with events to show progress and errors.
	 *
	 * @return the value of the FileReader property
	 * @see <a href="http://www.w3.org/TR/FileAPI/">More information</a>
	 */
	AspectPropertyValue<Boolean> getFileReader();
	/**
	 * Indicates if the browser allows Blobs to be saved to client machines with events to show progress and errors. The End-User may opt to decline these files.
	 *
	 * @return the value of the FileSaver property
	 * @see <a href="http://www.w3.org/TR/file-writer-api/#idl-def-FileSaver">More information</a>
	 */
	AspectPropertyValue<Boolean> getFileSaver();
	/**
	 * Indicates if the browser allows files to be saved to client machines with events to show progress and errors. The End-User may opt to decline these files.
	 *
	 * @return the value of the FileWriter property
	 * @see <a href="http://www.w3.org/TR/file-writer-api/">More information</a>
	 */
	AspectPropertyValue<Boolean> getFileWriter();
	/**
	 * Indicates if the browser supports the 'FormData' object. This property also refers to XMLHttpRequest. If the browser supports 'xhr2', the 'FormData' element will be also supported. 
	 *
	 * @return the value of the FormData property
	 * @see <a href="http://www.w3.org/TR/XMLHttpRequest/#interface-formdata">More information</a>
	 */
	AspectPropertyValue<Boolean> getFormData();
	/**
	 * Indicates the list of frequency bands supported by the device.
	 *
	 * @return the value of the FrequencyBands property
	 */
	AspectPropertyValue<List<String>> getFrequencyBands();
	/**
	 * Indicates the resolution of the device's front camera in megapixels. For a device that has a rotating camera the same value is returned for front and back megapixels' properties.
	 *
	 * @return the value of the FrontCameraMegaPixels property
	 */
	AspectPropertyValue<Double> getFrontCameraMegaPixels();
	/**
	 * Indicates if the browser supports requests from a video or canvas element to be displayed in full-screen mode.
	 *
	 * @return the value of the Fullscreen property
	 * @see <a href="http://dvcs.w3.org/hg/fullscreen/raw-file/tip/Overview.html">More information</a>
	 */
	AspectPropertyValue<Boolean> getFullscreen();
	/**
	 * Indicates if the browser supports a feature to acquire the geographical location. For information on which GeoLoc API the browser supports, refer to another property called JavaScriptPreferredGeoLocApi.
	 *
	 * @return the value of the GeoLocation property
	 * @see <a href="http://www.w3.org/TR/geolocation-API/">More information</a>
	 */
	AspectPropertyValue<Boolean> getGeoLocation();
	/**
	 * Indicates the official name of the graphical chip within the SoC.
	 *
	 * @return the value of the GPU property
	 */
	AspectPropertyValue<String> getGPU();
	/**
	 * Indicates the Semiconductor Company that designed the GPU.
	 *
	 * @return the value of the GPUDesigner property
	 */
	AspectPropertyValue<String> getGPUDesigner();
	/**
	 * Refers to the list of audio codecs supported for decoding by a Chipset. An audio codec is a program used to playback digital audio files. The values of this property are the codec's common name.
	 *
	 * @return the value of the HardwareAudioCodecsDecode property
	 */
	AspectPropertyValue<List<String>> getHardwareAudioCodecsDecode();
	/**
	 * Refers to the list of audio codecs supported for encoding by a Chipset. An audio codec is a program used to capture digital audio files. The values of this property are the codec's common name.
	 *
	 * @return the value of the HardwareAudioCodecsEncode property
	 */
	AspectPropertyValue<List<String>> getHardwareAudioCodecsEncode();
	/**
	 * Indicates the carrier when the device is sold by the HardwareVendor on a single carrier or as indicated via device User-Agent.
	 *
	 * @return the value of the HardwareCarrier property
	 */
	AspectPropertyValue<String> getHardwareCarrier();
	/**
	 * Indicates the name of a group of devices that only differ by model or region but are marketed under the same name, e.g. Galaxy Tab S 10.5.
	 *
	 * @return the value of the HardwareFamily property
	 */
	AspectPropertyValue<String> getHardwareFamily();
	/**
	 * A list of images associated with the device. The string contains the caption, followed by the full image URL separated with a tab character.
	 *
	 * @return the value of the HardwareImages property
	 */
	AspectPropertyValue<List<String>> getHardwareImages();
	/**
	 * Indicates the model name or number used primarily by the hardware vendor to identify the device, e.g.SM-T805S. When a model identifier is not available the HardwareName will be used.
	 *
	 * @return the value of the HardwareModel property
	 */
	AspectPropertyValue<String> getHardwareModel();
	/**
	 * Indicates all model numbers used by the hardware vendor to identify the device. This property compliments 'HardwareModel', e.g. Hardware Model Variants A1660 and A1778 correlate to the Hardware Model - iPhone 7.
	 *
	 * @return the value of the HardwareModelVariants property
	 */
	AspectPropertyValue<List<String>> getHardwareModelVariants();
	/**
	 * Indicates the common marketing names associated with the device, e.g. Xperia Z5.
	 *
	 * @return the value of the HardwareName property
	 */
	AspectPropertyValue<List<String>> getHardwareName();
	/**
	 * Marketed name of the device or series (e.g., iPhone). If a device does not have an established naming system, this field will be an empty string.
	 *
	 * @return the value of the HardwareNamePrefix property
	 */
	AspectPropertyValue<String> getHardwareNamePrefix();
	/**
	 * Marketed version of the device or series (e.g., 17 Pro Max). If a device does not have an established naming system, this field will be an empty string.
	 *
	 * @return the value of the HardwareNameVersion property
	 */
	AspectPropertyValue<String> getHardwareNameVersion();
	/**
	 * Indicates the source of the profile's specifications. This property will return 'Manufacturer' value if the profile data was obtained from the manufacturer of the device or the device itself. This property will return 'Authoritative' value if the profile data was not obtained from the manufacturer or the device itself but other third party sources (this may include retailers, social media, carriers, etc). This property will return 'Legacy' value if the profile data was obtained prior to 51degrees differentiating between Manufacturer and Authoritative. This property will return 'N/A' value if the profile data was not obtained due to unidentifiable User-Agent. The example profiles are: Generic Android Unknown, Unknown Tablet, etc.
	 *
	 * @return the value of the HardwareProfileSource property
	 */
	AspectPropertyValue<String> getHardwareProfileSource();
	/**
	 * A measure of the popularity of this device model. All models are ordered by the number of events associated with that model that occurred in the sampling period. The device with the most events is ranked 1, the second 2 and so on. 
	 *
	 * @return the value of the HardwareRank property
	 */
	AspectPropertyValue<Integer> getHardwareRank();
	/**
	 * Indicates the name of the company that manufactures the device or primarily sells it, e.g. Samsung.
	 *
	 * @return the value of the HardwareVendor property
	 */
	AspectPropertyValue<String> getHardwareVendor();
	/**
	 * Refers to the list of video codecs supported for decoding by a Chipset. A video codec is a program used to playback digital video files. The values of this property are the codec's common name. 
	 *
	 * @return the value of the HardwareVideoCodecsDecode property
	 */
	AspectPropertyValue<List<String>> getHardwareVideoCodecsDecode();
	/**
	 * Refers to the list of video codecs supported for encoding by a Chipset. A video codec is a program used to capture digital video files. The values of this property are the codec's common name. 
	 *
	 * @return the value of the HardwareVideoCodecsEncode property
	 */
	AspectPropertyValue<List<String>> getHardwareVideoCodecsEncode();
	/**
	 * Indicates if the device has a camera capable of taking 3D images.
	 *
	 * @return the value of the Has3DCamera property
	 */
	AspectPropertyValue<Boolean> getHas3DCamera();
	/**
	 * Indicates if the device has a screen capable of displaying 3D images.
	 *
	 * @return the value of the Has3DScreen property
	 */
	AspectPropertyValue<Boolean> getHas3DScreen();
	/**
	 * Indicates if the device has a camera.
	 *
	 * @return the value of the HasCamera property
	 */
	AspectPropertyValue<Boolean> getHasCamera();
	/**
	 * Indicates if the device has a click wheel such as found on Apple iPod devices.
	 *
	 * @return the value of the HasClickWheel property
	 */
	AspectPropertyValue<Boolean> getHasClickWheel();
	/**
	 * Indicates if the device has a physical numeric keypad.
	 *
	 * @return the value of the HasKeypad property
	 */
	AspectPropertyValue<Boolean> getHasKeypad();
	/**
	 * Indicates if the device has embedded NFC (Near Field Communication) wireless technology.
	 *
	 * @return the value of the HasNFC property
	 */
	AspectPropertyValue<Boolean> getHasNFC();
	/**
	 * Indicates if the device has a physical qwerty keyboard.
	 *
	 * @return the value of the HasQwertyPad property
	 */
	AspectPropertyValue<Boolean> getHasQwertyPad();
	/**
	 * Indicates if the device has a removable battery. This property is not applicable for devices that do not have batteries. Unless otherwise stated this property will return a 'False' value for tablets.
	 *
	 * @return the value of the HasRemovableBattery property
	 */
	AspectPropertyValue<Boolean> getHasRemovableBattery();
	/**
	 * Indicates if the device has a touch screen. This property will return 'False' for a device that does not have an integrated screen.
	 *
	 * @return the value of the HasTouchScreen property
	 */
	AspectPropertyValue<Boolean> getHasTouchScreen();
	/**
	 * Indicates if the device has a trackpad or trackball. Examples of devices that support this property are the Nexus One and Blackberry Curve.
	 *
	 * @return the value of the HasTrackPad property
	 */
	AspectPropertyValue<Boolean> getHasTrackPad();
	/**
	 * Indicates if the device has a virtual qwerty keyboard capability.
	 *
	 * @return the value of the HasVirtualQwerty property
	 */
	AspectPropertyValue<Boolean> getHasVirtualQwerty();
	/**
	 * Indicates whether the browser advertises a web driver, as a browser under automation is required to do. Returns True where the browser advertises a web driver, and False where it advertises none. The value reports what the browser advertises and not what an automated client may be concealing.
	 *
	 * @return the value of the HasWebDriver property
	 */
	AspectPropertyValue<Boolean> getHasWebDriver();
	/**
	 * JavaScript which overrides the HasWebDriver property to report whether the browser advertised a web driver when the 51Degrees JavaScript ran.
	 *
	 * @return the value of the HasWebDriverJavaScript property
	 */
	AspectPropertyValue<JavaScript> getHasWebDriverJavaScript();
	/**
	 * Indicates if the browser stores the session history for a web page that contains the URLs visited by the browser's user.
	 *
	 * @return the value of the History property
	 * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html">More information</a>
	 */
	AspectPropertyValue<Boolean> getHistory();
	/**
	 * Indicates if the browser is able to use media inputs, e.g. webcam and microphone, in a script and as an input for forms, e.g. '&lt;input type=&quot;file&quot; accept=&quot;image/*&quot; id=&quot;capture&quot;&gt;' would prompt image- capturing software to open.
	 *
	 * @return the value of the Html-Media-Capture property
	 * @see <a href="http://www.w3.org/TR/html-media-capture/">More information</a>
	 */
	AspectPropertyValue<Boolean> getHtmlMediaCapture();
	/**
	 * Indicates if the browser supports the new markup in HTML 5 that also refers to 'New Semantic Elements' such as &lt;header&gt;, &lt;nav&gt;, &lt;section&gt;, &lt;aside&gt;,&lt;footer&gt; etc.
	 *
	 * @return the value of the Html5 property
	 * @see <a href="http://www.w3.org/TR/html5/">More information</a>
	 */
	AspectPropertyValue<Boolean> getHtml5();
	/**
	 * Lists what audio formats, if any, the browser supports using the HTML5 &lt;audio&gt; tag.
	 *
	 * @return the value of the Html5Audio property
	 * @see <a href="http://www.w3schools.com/html5/html5_audio.asp">More information</a>
	 */
	AspectPropertyValue<List<String>> getHtml5Audio();
	/**
	 * Lists what video formats, if any, the browser supports using the HTLM5 &lt;video&gt; tag.
	 *
	 * @return the value of the Html5Video property
	 * @see <a href="http://www.w3schools.com/html5/html5_video.asp">More information</a>
	 */
	AspectPropertyValue<List<String>> getHtml5Video();
	/**
	 * Refers to the latest version of HyperText Markup Language (HTML) supported by the browser.
	 *
	 * @return the value of the HtmlVersion property
	 * @see <a href="http://www.w3schools.com/w3c/w3c_html.asp">More information</a>
	 */
	AspectPropertyValue<Double> getHtmlVersion();
	/**
	 * Indicates if the browser supports HTTP version 2.
	 *
	 * @return the value of the Http2 property
	 */
	AspectPropertyValue<Boolean> getHttp2();
	/**
	 * Indicates if the browser supports HTTP Live Streaming, also known as HLS.
	 *
	 * @return the value of the HttpLiveStreaming property
	 */
	AspectPropertyValue<String> getHttpLiveStreaming();
	/**
	 * Indicates if the browser supports the 'Iframe' element, used to embed another document within a current HTML document.
	 *
	 * @return the value of the Iframe property
	 * @see <a href="http://dev.w3.org/html5/spec/the-iframe-element.html#the-iframe-element">More information</a>
	 */
	AspectPropertyValue<Boolean> getIframe();
	/**
	 * Indicates if the browser supports an indexed local database.
	 *
	 * @return the value of the IndexedDB property
	 * @see <a href="http://www.w3.org/TR/IndexedDB/">More information</a>
	 */
	AspectPropertyValue<Boolean> getIndexedDB();
	/**
	 * Indicates the internal persistent storage (ROM capacity) options the device can be supplied with in gigabytes (GB), including the device's Operating System and bundled applications. This could also be referred to as &quot;Electrically Erasable Programmable Read-Only Memory (EEPROM)&quot; or &quot;Non Volatile Random Access Memory (NVRAM)&quot;. If no variants are found, then the value returned will be the same as &quot;MaxInternalStorage&quot;.
	 *
	 * @return the value of the InternalStorageVariants property
	 */
	AspectPropertyValue<List<String>> getInternalStorageVariants();
	/**
	 * Indicates if a web page is accessed through a VR headset.
	 *
	 * @return the value of the InVRMode property
	 */
	AspectPropertyValue<Boolean> getInVRMode();
	/**
	 * Indicates whether the crawler operates for an AI related purpose, as defined by the values in the CrawlerUsage property. This is based on our judgement of information available from the crawler controller or other trusted research.
	 *
	 * @return the value of the IsArtificialIntelligence property
	 */
	AspectPropertyValue<String> getIsArtificialIntelligence();
	/**
	 * Indicates if the device is primarily a game console, such as an Xbox or Playstation.
	 *
	 * @return the value of the IsConsole property
	 */
	AspectPropertyValue<Boolean> getIsConsole();
	/**
	 * Indicates if the source of the web traffic identifies itself as operating without human interaction for the purpose of monitoring the availability or performance of a web site, retrieving a response for inclusion in a search engine or is requesting structured data such as via an API. Such sources are often referred to as crawlers, bots, robots, spiders, probes, monitors or HTTP services among other terms. Where the source pretends to be a device operating with human interaction, such as a smartphone or tablet, this property will return, 'False'.
	 *
	 * @return the value of the IsCrawler property
	 */
	AspectPropertyValue<Boolean> getIsCrawler();
	/**
	 * Indicates if the browser may be optimised for low bandwidth. A true value indicates the browser supports a feature that can improve performance on low bandwidth connections, either via the removal of elements, features, a proxy or other methods.
	 *
	 * @return the value of the IsDataMinimising property
	 */
	AspectPropertyValue<Boolean> getIsDataMinimising();
	/**
	 * Indicates if the application is an email browser (Outlook, Gmail, YahooMail, etc.) that is primarily used to access and manage emails (usually from mobile devices).
	 *
	 * @return the value of the IsEmailBrowser property
	 */
	AspectPropertyValue<Boolean> getIsEmailBrowser();
	/**
	 * Indicates if the mobile device accessing a web page emulates a desktop computer. This property is not applicable for desktops, media hubs, TVs and consoles.
	 *
	 * @return the value of the IsEmulatingDesktop property
	 */
	AspectPropertyValue<Boolean> getIsEmulatingDesktop();
	/**
	 * Indicates a browser that does not correctly identify the physical hardware device and instead reports an emulated device.
	 *
	 * @return the value of the IsEmulatingDevice property
	 */
	AspectPropertyValue<Boolean> getIsEmulatingDevice();
	/**
	 * Indicates if the device is primarily advertised as an e-reader. If the device type is EReader then the device is not classified as a tablet.
	 *
	 * @return the value of the IsEReader property
	 */
	AspectPropertyValue<Boolean> getIsEReader();
	/**
	 * Indicates a profile which contains more than a single hardware device. When this is true all returned properties represent the default value or lowest given specification of all grouped devices. E.g. the profile representing unknown Windows 10 tablets will return true. Apple devices detected through JavascriptHardwareProfile that do not uniquely identify a device will also return true, and HardwareModelVariants will return a list of model numbers associated with that device group.
	 *
	 * @return the value of the IsHardwareGroup property
	 */
	AspectPropertyValue<Boolean> getIsHardwareGroup();
	/**
	 * Indicates whether the browser identifies itself as running without a visible window, for example through the HeadlessChrome token in the User-Agent. The value reflects what the browser advertises about itself and does not indicate whether the session is automated or controlled by a web driver.
	 *
	 * @return the value of the IsHeadless property
	 */
	AspectPropertyValue<Boolean> getIsHeadless();
	/**
	 * Indicates if the device is a media hub or set top box that requires an external display(s).
	 *
	 * @return the value of the IsMediaHub property
	 */
	AspectPropertyValue<Boolean> getIsMediaHub();
	/**
	 * Indicates if the device's primary data connection is wireless and the device is designed to operate mostly by battery power (e.g. mobile phone, smartphone or tablet). This property does not indicate if the device is a mobile phone or not. Laptops are not classified as mobile devices under this definition and so 'IsMobile' will be 'False'.
	 *
	 * @return the value of the IsMobile property
	 */
	AspectPropertyValue<Boolean> getIsMobile();
	/**
	 * Indicates whether the PlatformVendor is providing a frozen or incorrect platform version in the User-Agent evidence. When True, the PlatformVersion will be the most likely version and may be different to the value provided in the evidence.
	 *
	 * @return the value of the IsPlatformVersionFrozen property
	 */
	AspectPropertyValue<Boolean> getIsPlatformVersionFrozen();
	/**
	 * Indicates whether the device screen is foldable or not. If the device does not have a screen or the screen is not foldable, 'False' is returned.
	 *
	 * @return the value of the IsScreenFoldable property
	 */
	AspectPropertyValue<Boolean> getIsScreenFoldable();
	/**
	 * Indicates if the device is a mobile with a screen size less than 2.5 inches even where the device is marketed as a Smartphone.
	 *
	 * @return the value of the IsSmallScreen property
	 */
	AspectPropertyValue<Boolean> getIsSmallScreen();
	/**
	 * Indicates whether the device can make and receive phone calls, has a screen size greater than or equal to 2.5 inches, runs a modern operating system (Android, iOS, Windows Phone, BlackBerry etc.), is not designed to be a wearable technology and is marketed by the vendor as a Smartphone.
	 *
	 * @return the value of the IsSmartPhone property
	 */
	AspectPropertyValue<Boolean> getIsSmartPhone();
	/**
	 * Indicates if the device is a web enabled computerised wristwatch with other capabilities beyond timekeeping, such as push notifications. It runs on a Smart Operating System i.e. Android, WatchOS, Tizen, Ubuntu Touch and is designed to be wearable technology.
	 *
	 * @return the value of the IsSmartWatch property
	 */
	AspectPropertyValue<Boolean> getIsSmartWatch();
	/**
	 * Indicates if the device is primarily marketed as a tablet or phablet and has a screen size equal to or greater than 7 inches.
	 *
	 * @return the value of the IsTablet property
	 */
	AspectPropertyValue<Boolean> getIsTablet();
	/**
	 * Indicates if the device is a TV running on a smart operating system e.g. Android.
	 *
	 * @return the value of the IsTv property
	 */
	AspectPropertyValue<Boolean> getIsTv();
	/**
	 * Indicates whether the device is likely to be a genuine iPhone based on interrogation by JavaScript contained in the JavaScriptHardwareProfile property.
	 *
	 * @return the value of the IsVerifiediPhone property
	 */
	AspectPropertyValue<String> getIsVerifiediPhone();
	/**
	 * JavaScript which overrides the IsVerifiediPhone property to report whether an iPhone is genuine. This is only used where the device is presenting as an iPhone with the evidence provided. Requires JavascriptHardwareProfile property.
	 *
	 * @return the value of the IsVerifiediPhoneJavaScript property
	 */
	AspectPropertyValue<JavaScript> getIsVerifiediPhoneJavaScript();
	/**
	 * Indicates whether the browser window was in view when the 51Degrees JavaScript ran on the page. A false value means the page was in a background tab, in a minimised window, entirely obscured by another window, or on a device whose screen was off at that moment. The value describes that moment only and is not updated if the window later comes into view.
	 *
	 * @return the value of the IsVisible property
	 */
	AspectPropertyValue<Boolean> getIsVisible();
	/**
	 * JavaScript which overrides the IsVisible property to report whether the browser window was in view when the 51Degrees JavaScript ran on the page.
	 *
	 * @return the value of the IsVisibleJavaScript property
	 */
	AspectPropertyValue<JavaScript> getIsVisibleJavaScript();
	/**
	 * Indicates if the browser or app is being used to access a web page through a WebView.
	 *
	 * @return the value of the IsWebApp property
	 * @see <a href="https://en.wikipedia.org/wiki/WebView">More information</a>
	 */
	AspectPropertyValue<Boolean> getIsWebApp();
	/**
	 * The number of iterations carried out in order to find a match. This is the number of nodes in the graph which have been visited.
	 *
	 * @return the value of the Iterations property
	 */
	AspectPropertyValue<Integer> getIterations();
	/**
	 * Indicates if the browser supports JavaScript.
	 *
	 * @return the value of the Javascript property
	 * @see <a href="http://www.w3schools.com/js/js_intro.asp">More information</a>
	 */
	AspectPropertyValue<Boolean> getJavascript();
	/**
	 * JavaScript that checks for browser specific features and overrides the ProfileID.
	 *
	 * @return the value of the JavaScriptBrowserOverride property
	 */
	AspectPropertyValue<JavaScript> getJavaScriptBrowserOverride();
	/**
	 * Indicates if the browser supports the JavaScript that can manipulate CSS on the browser's web page.
	 *
	 * @return the value of the JavascriptCanManipulateCSS property
	 */
	AspectPropertyValue<Boolean> getJavascriptCanManipulateCSS();
	/**
	 * Indicates if the browser supports the JavaScript that can manipulate the Document Object Model on the browser's web page.
	 *
	 * @return the value of the JavascriptCanManipulateDOM property
	 */
	AspectPropertyValue<Boolean> getJavascriptCanManipulateDOM();
	/**
	 * Indicates if the browser supports JavaScript that is able to access HTML elements from their ID using the getElementById method.
	 *
	 * @return the value of the JavascriptGetElementById property
	 */
	AspectPropertyValue<Boolean> getJavascriptGetElementById();
	/**
	 * Contains Javascript to get high entropy values.
	 *
	 * @return the value of the JavascriptGetHighEntropyValues property
	 */
	AspectPropertyValue<JavaScript> getJavascriptGetHighEntropyValues();
	/**
	 * JavaScript that can override the profile found by the server using information on the client device. This property is applicable for Apple devices which do not provide information about the model in the User-Agent string.
	 *
	 * @return the value of the JavascriptHardwareProfile property
	 */
	AspectPropertyValue<JavaScript> getJavascriptHardwareProfile();
	/**
	 * Refers to the JavaScript snippet used to optimise images.
	 *
	 * @return the value of the JavascriptImageOptimiser property
	 */
	AspectPropertyValue<JavaScript> getJavascriptImageOptimiser();
	/**
	 * Indicates which GeoLoc API JavaScript the browser supports. If a browser supports a feature to acquire the user's geographical location, another property called 'GeoLocation' will be set to True.
	 *
	 * @return the value of the JavascriptPreferredGeoLocApi property
	 */
	AspectPropertyValue<String> getJavascriptPreferredGeoLocApi();
	/**
	 * Indicates if the browser allows registration of event listeners on event targets by using the addEventListener() method.
	 *
	 * @return the value of the JavascriptSupportsEventListener property
	 */
	AspectPropertyValue<Boolean> getJavascriptSupportsEventListener();
	/**
	 * Indicates if the browser supports the JavaScript events 'onload', 'onclick' and 'onselect'. 
	 *
	 * @return the value of the JavascriptSupportsEvents property
	 */
	AspectPropertyValue<Boolean> getJavascriptSupportsEvents();
	/**
	 * Indicates if the browser supports the JavaScript that is able to insert HTML into a DIV tag.
	 *
	 * @return the value of the JavascriptSupportsInnerHtml property
	 */
	AspectPropertyValue<Boolean> getJavascriptSupportsInnerHtml();
	/**
	 * Indicates which JavaScript version the browser uses. The number refers to JavaScript versioning, not ECMAscript or Jscript. If the browser doesn't support JavaScript then 'NotSupported' value is returned.
	 *
	 * @return the value of the JavascriptVersion property
	 */
	AspectPropertyValue<String> getJavascriptVersion();
	/**
	 * Indicates the browser supports JPEG 2000 image format.
	 *
	 * @return the value of the Jpeg2000 property
	 */
	AspectPropertyValue<Boolean> getJpeg2000();
	/**
	 * Refers to the grade of the level the device has with the jQuery Mobile Framework, as posted by jQuery.
	 *
	 * @return the value of the jQueryMobileSupport property
	 * @see <a href="http://jquerymobile.com/gbs/">More information</a>
	 */
	AspectPropertyValue<String> getjQueryMobileSupport();
	/**
	 * Indicates if the browser supports the 'JSON' object. This property may need a vendor prefix, e.g. webkit, moz, etc.
	 *
	 * @return the value of the Json property
	 * @see <a href="http://es5.github.com/x15.12.html">More information</a>
	 */
	AspectPropertyValue<Boolean> getJson();
	/**
	 * Refers to the name of the embedded technology the browser uses to display formatted content on the screen.
	 *
	 * @return the value of the LayoutEngine property
	 * @see <a href="http://en.wikipedia.org/wiki/Web_browser_engine">More information</a>
	 */
	AspectPropertyValue<String> getLayoutEngine();
	/**
	 * Indicates if the browser supports the CSS-mask element that allows users to alter the visibility of an item by either partially or fully hiding the item.
	 *
	 * @return the value of the Masking property
	 * @see <a href="http://www.webkit.org/blog/181/css-masks/">More information</a>
	 */
	AspectPropertyValue<Boolean> getMasking();
	/**
	 * Indicates the number of hash nodes matched within the evidence.
	 *
	 * @return the value of the MatchedNodes property
	 */
	AspectPropertyValue<Integer> getMatchedNodes();
	/**
	 * Indicates the maximum amount of internal persistent storage (ROM capacity) with which the device is supplied in gigabytes (GB), including the space used by the device's Operating System and bundled applications. This could also be referred to as &quot;Electrically Erasable Programmable Read-Only Memory (EEPROM)&quot; or &quot;Non Volatile Random Access Memory (NVRAM)&quot;. Where a device has different internal storage options, the largest option available is returned.
	 *
	 * @return the value of the MaxInternalStorage property
	 */
	AspectPropertyValue<Double> getMaxInternalStorage();
	/**
	 * Indicates the maximum number of &quot;Universal Integrated Circuit Cards (UICC - more commonly known as, SIM)&quot; the device can support including both removable and embedded. If the device doesn't support any UICC then a value of '0' is returned.
	 *
	 * @return the value of the MaxNumberOfSIMCards property
	 */
	AspectPropertyValue<Integer> getMaxNumberOfSIMCards();
	/**
	 * Indicates the maximum standby time of the device in hours. This property is not applicable for a device without a battery.
	 *
	 * @return the value of the MaxStandbyTime property
	 */
	AspectPropertyValue<Integer> getMaxStandbyTime();
	/**
	 * Indicates the maximum talk time of the device in minutes. This property is not applicable for a device that does not have a battery or support phone calls.
	 *
	 * @return the value of the MaxTalkTime property
	 */
	AspectPropertyValue<Integer> getMaxTalkTime();
	/**
	 * Indicates the maximum general usage time of the device in minutes. This property is not applicable for a device without a battery.
	 *
	 * @return the value of the MaxUsageTime property
	 */
	AspectPropertyValue<Integer> getMaxUsageTime();
	/**
	 * Indicates if the browser supports a meter element that represents a scalar measurement within a known range or fractional value. This property does not indicate whether the browser supports the progress bar indication. For this purpose, the progress property should be used.
	 *
	 * @return the value of the Meter property
	 * @see <a href="http://www.w3.org/TR/html-markup/meter.html">More information</a>
	 */
	AspectPropertyValue<Boolean> getMeter();
	/**
	 * The method used to determine the match result.
	 *
	 * @return the value of the Method property
	 */
	AspectPropertyValue<String> getMethod();
	/**
	 * Indicates which version of Mobile Information Device Profile the device supports, used with Java ME and CLDC.
	 *
	 * @return the value of the MIDP property
	 */
	AspectPropertyValue<Double> getMIDP();
	/**
	 * Refers to the 'Retail Branding' value returned for Android Google Play native applications, when the android.os.Build.BRAND javascript is used to display the class. This property is not applicable for hardware running on operating systems other than Android.
	 *
	 * @return the value of the NativeBrand property
	 */
	AspectPropertyValue<List<String>> getNativeBrand();
	/**
	 * Refers to the 'Device' value returned for Android Google Play native applications, when the android.os.Build.DEVICE javascript is used to display the class. This property is not applicable for hardware running on operating systems other than Android.
	 *
	 * @return the value of the NativeDevice property
	 */
	AspectPropertyValue<List<String>> getNativeDevice();
	/**
	 * Refers to the 'Model' value returned for Android Google Play or Amazon native applications when the android.os.Build.MODEL javascript is used to display the class. For Apple devices, this property refers to the device identifier, for example 'iPad5,4'. This may be obtained by calling sysctlbyname and passing hw.machine as a name parameter or alternatively by obtaining &quot;machine&quot; member from a populated utsname structure using uname API.
	 *
	 * @return the value of the NativeModel property
	 */
	AspectPropertyValue<List<String>> getNativeModel();
	/**
	 * NativeName Refers to the 'Marketing Name' value that a device is registered with on the Google Play service. This property is not applicable for hardware running on operating systems other than Android.
	 *
	 * @return the value of the NativeName property
	 */
	AspectPropertyValue<List<String>> getNativeName();
	/**
	 * Indicates the name of the mobile operating system (iOS, Android) for which an application program has been developed to be used by a device.
	 *
	 * @return the value of the NativePlatform property
	 */
	AspectPropertyValue<String> getNativePlatform();
	/**
	 * Indicates the number of screens the device has. This property is not applicable for a device that does not have a screen. A display is only considered a screen if it could be used to display a web page.
	 *
	 * @return the value of the NumberOfScreens property
	 */
	AspectPropertyValue<Integer> getNumberOfScreens();
	/**
	 * Indicates the name of the company that manufactures the device.
	 *
	 * @return the value of the OEM property
	 */
	AspectPropertyValue<String> getOEM();
	/**
	 * Indicates the power consumption of the device while switched on.
	 *
	 * @return the value of the OnPowerConsumption property
	 */
	AspectPropertyValue<Integer> getOnPowerConsumption();
	/**
	 * The ratio of the resolution in physical pixels to the resolution in CSS pixels. This is approximated by screen resolution and screen size when the value is not known.
	 *
	 * @return the value of the PixelRatio property
	 */
	AspectPropertyValue<Double> getPixelRatio();
	/**
	 * JavaScript that can override the property value found by the server using information on the client device. This property is applicable for browsers that support pixel ratio cookie.
	 *
	 * @return the value of the PixelRatioJavascript property
	 */
	AspectPropertyValue<JavaScript> getPixelRatioJavascript();
	/**
	 * Indicates the age in months of the operating system since the PlatformReleaseYear and PlatformReleaseMonth.
	 *
	 * @return the value of the PlatformDiscontinuedAge property
	 */
	AspectPropertyValue<Integer> getPlatformDiscontinuedAge();
	/**
	 * The month in which further development for the platform version is stopped by the platform vendor. This occurs when a new stable version of the platform is released.
	 *
	 * @return the value of the PlatformDiscontinuedMonth property
	 */
	AspectPropertyValue<String> getPlatformDiscontinuedMonth();
	/**
	 * The year in which further development for the platform version is stopped by the platform vendor. This occurs when a new stable version of the platform is released.
	 *
	 * @return the value of the PlatformDiscontinuedYear property
	 */
	AspectPropertyValue<Integer> getPlatformDiscontinuedYear();
	/**
	 * A list of logos associated with the Software. The string contains the caption, followed by the full image URL separated with a tab character.
	 *
	 * @return the value of the PlatformLogos property
	 */
	AspectPropertyValue<List<String>> getPlatformLogos();
	/**
	 * Indicates the name of the operating system the device is using.
	 *
	 * @return the value of the PlatformName property
	 * @see <a href="http://en.wikipedia.org/wiki/List_of_operating_systems">More information</a>
	 */
	AspectPropertyValue<String> getPlatformName();
	/**
	 * Indicates the age in months of the operating system since the PlatformPreviewYear and PlatformPreviewMonth.
	 *
	 * @return the value of the PlatformPreviewAge property
	 */
	AspectPropertyValue<Integer> getPlatformPreviewAge();
	/**
	 * The month in which the platform version was originally released as a Beta/Developer version by the platform vendor. This is before it is officially released as a stable version, to ensure wider testing by the community can take place.
	 *
	 * @return the value of the PlatformPreviewMonth property
	 */
	AspectPropertyValue<String> getPlatformPreviewMonth();
	/**
	 * The year in which the platform version was originally released as a Beta/Developer version by the platform vendor. This is before it is officially released as a stable version, to ensure wider testing by the community can take place.
	 *
	 * @return the value of the PlatformPreviewYear property
	 */
	AspectPropertyValue<Integer> getPlatformPreviewYear();
	/**
	 * A measure of the popularity of this software platform (i.e. OS and version). All platforms are ordered by the number of events associated with that platform that occurred in the sampling period. The platform with the most events is ranked 1, the second 2 and so on.
	 *
	 * @return the value of the PlatformRank property
	 */
	AspectPropertyValue<Integer> getPlatformRank();
	/**
	 * Indicates the age in months of the operating system since the PlatformReleaseYear and PlatformReleaseMonth.
	 *
	 * @return the value of the PlatformReleaseAge property
	 */
	AspectPropertyValue<Integer> getPlatformReleaseAge();
	/**
	 * The month in which the platform version was officially released to users by the platform vendor. This version is called the stable version as any bugs or difficulties highlighted in the Beta/Developer version will have been fixed for this release.
	 *
	 * @return the value of the PlatformReleaseMonth property
	 */
	AspectPropertyValue<String> getPlatformReleaseMonth();
	/**
	 * The year in which the platform version was officially released to users by the platform vendor. This version is called the stable version as any bugs or difficulties highlighted in the Beta/Developer Version will have been fixed for this release.
	 *
	 * @return the value of the PlatformReleaseYear property
	 */
	AspectPropertyValue<Integer> getPlatformReleaseYear();
	/**
	 * Indicates the name of the company that developed the operating system.
	 *
	 * @return the value of the PlatformVendor property
	 * @see <a href="http://en.wikipedia.org/wiki/Mobile_operating_system">More information</a>
	 */
	AspectPropertyValue<String> getPlatformVendor();
	/**
	 * Indicates the version or subversion of the software platform. Where the PlatformVendor is Apple, the PlatformVersion returned will be the most likely version. It may be different to the value provided in the evidence.
	 *
	 * @return the value of the PlatformVersion property
	 * @see <a href="http://en.wikipedia.org/wiki/Mobile_operating_system">More information</a>
	 */
	AspectPropertyValue<String> getPlatformVersion();
	/**
	 * Refers to the number of unique client IPs from which this device has been seen.
	 *
	 * @return the value of the Popularity property
	 */
	AspectPropertyValue<String> getPopularity();
	/**
	 * Indicates if the browser supports messages between different documents.
	 *
	 * @return the value of the PostMessage property
	 * @see <a href="http://www.w3.org/TR/webmessaging/">More information</a>
	 */
	AspectPropertyValue<Boolean> getPostMessage();
	/**
	 * Indicates if the browser can prefetch resources without executing them.
	 *
	 * @return the value of the Preload property
	 */
	AspectPropertyValue<Boolean> getPreload();
	/**
	 * Indicates a price range describing the recommended retail price of the device at the date of release, inclusive of tax (where applicable).  Prices are in United States Dollars (USD); if the price is not originally in USD it will be converted to USD using the relevant exchange rate at the time of launch. Prices are for the SIM-free version of the device (if applicable). In cases where there are several versions of the same model of the device, the price will reflect the device that was used to populate the specifications.
	 *
	 * @return the value of the PriceBand property
	 */
	AspectPropertyValue<String> getPriceBand();
	/**
	 * Gets the Profiles property.
	 *
	 * @return the value of the Profiles property
	 */
	AspectPropertyValue<String> getProfiles();
	/**
	 * Indicates if the browser supports progress reports, such as with HTTP requests. The progress element can be used to display the progress of the task. This property doesn't represent a scalar measurement. If the browser supports a gauge, the meter property should be used.
	 *
	 * @return the value of the Progress property
	 * @see <a href="http://www.w3.org/TR/progress-events/">More information</a>
	 */
	AspectPropertyValue<Boolean> getProgress();
	/**
	 * Indicates the level of support for the Promise object. The Promise object represents the eventual completion (or failure) of an asynchronous operation, and its resulting value.
	 *
	 * @return the value of the Promise property
	 */
	AspectPropertyValue<String> getPromise();
	/**
	 * Indicates if the browser supports simple dialogues (window.alert, window.confirm and window.prompt).
	 *
	 * @return the value of the Prompts property
	 * @see <a href="http://www.w3.org/TR/html5/timers.html#user-prompts">More information</a>
	 */
	AspectPropertyValue<Boolean> getPrompts();
	/**
	 * Indicates the maximum number of frames per second of the output image of the device in Hertz.
	 *
	 * @return the value of the RefreshRate property
	 */
	AspectPropertyValue<Integer> getRefreshRate();
	/**
	 * Indicates the age in months of the device since the ReleaseYear and ReleaseMonth.
	 *
	 * @return the value of the ReleaseAge property
	 */
	AspectPropertyValue<Integer> getReleaseAge();
	/**
	 * Indicates the month in which the device was released or the month in which the device was first seen by 51Degrees (if the release date cannot be identified).
	 *
	 * @return the value of the ReleaseMonth property
	 */
	AspectPropertyValue<String> getReleaseMonth();
	/**
	 * Indicates the year in which the device was released or the year in which the device was first seen by 51Degrees (if the release date cannot be identified).
	 *
	 * @return the value of the ReleaseYear property
	 */
	AspectPropertyValue<Integer> getReleaseYear();
	/**
	 * Indicates  the device's supported satellite navigation types.
	 *
	 * @return the value of the SatelliteNavigationTypes property
	 */
	AspectPropertyValue<List<String>> getSatelliteNavigationTypes();
	/**
	 * Indicates the diagonal size of the device's screen in inches, to a maximum of two decimal points. Where screens have curved corners, the actual viewable area may be less.
	 *
	 * @return the value of the ScreenInchesDiagonal property
	 */
	AspectPropertyValue<Double> getScreenInchesDiagonal();
	/**
	 * Indicates the diagonal size of the device's screen in inches rounded to the nearest whole number. This property will return the value 'Unknown' for desktop or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenInchesDiagonalRounded property
	 */
	AspectPropertyValue<Integer> getScreenInchesDiagonalRounded();
	/**
	 * Refers to the height of the device's screen in inches. This property will return 'Unknown' for desktops or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenInchesHeight property
	 */
	AspectPropertyValue<Double> getScreenInchesHeight();
	/**
	 * Indicates the area of the device's screen in square inches rounded to the nearest whole number. This property will return the value 'Unknown' for desktop or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenInchesSquare property
	 */
	AspectPropertyValue<Integer> getScreenInchesSquare();
	/**
	 * Refers to the width of the device's screen in inches. This property will return the value 'Unknown' for desktop or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenInchesWidth property
	 */
	AspectPropertyValue<Double> getScreenInchesWidth();
	/**
	 * Refers to the diagonal size of the screen of the device in millimetres. This property will return 'Unknown' for desktops or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenMMDiagonal property
	 */
	AspectPropertyValue<Double> getScreenMMDiagonal();
	/**
	 * Indicate the diagonal size of the device's screen in millimetres rounded to the nearest whole number. This property will return the value 'Unknown' for desktop or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenMMDiagonalRounded property
	 */
	AspectPropertyValue<Integer> getScreenMMDiagonalRounded();
	/**
	 * Refers to the screen height of the device in millimetres. This property will return 'Unknown' for desktops or for devices which do not have an integrated screen. For devices such as tablets or TV which are predominantly used in landscape mode, the screen height will be the smaller value compared to the screen width.
	 *
	 * @return the value of the ScreenMMHeight property
	 */
	AspectPropertyValue<Double> getScreenMMHeight();
	/**
	 * Indicates the area of the device's screen in square millimetres rounded to the nearest whole number. This property will return the value  'Unknown' for desktop or for devices which do not have an integrated screen.
	 *
	 * @return the value of the ScreenMMSquare property
	 */
	AspectPropertyValue<Integer> getScreenMMSquare();
	/**
	 * Refers to the screen width of the device in millimetres. This property will return 'Unknown' for desktops or for devices which do not have an integrated screen. For devices such as tablets or TV which are predominantly used in landscape mode, the screen height will be the smaller value compared to the screen width.
	 *
	 * @return the value of the ScreenMMWidth property
	 */
	AspectPropertyValue<Double> getScreenMMWidth();
	/**
	 * Indicates the height of the device's screen in pixels.This property is not applicable for a device that does not have a screen. For devices such as tablets or TV which are predominantly used in landscape mode, the pixel height will be the smaller value compared to the pixel width.
	 *
	 * @return the value of the ScreenPixelsHeight property
	 */
	AspectPropertyValue<Integer> getScreenPixelsHeight();
	/**
	 * JavaScript that can override the property value found by the server using information on the client device. This property is applicable for browsers that support screen pixels height cookie.
	 *
	 * @return the value of the ScreenPixelsHeightJavaScript property
	 */
	AspectPropertyValue<JavaScript> getScreenPixelsHeightJavaScript();
	/**
	 * Indicates the height of the device's screen in physical pixels. This property is not applicable for a device that does not have a screen. For devices such as tablets or TV which are predominantly used in landscape mode, the pixel height will be the smaller value compared to the pixel width. 
	 *
	 * @return the value of the ScreenPixelsPhysicalHeight property
	 */
	AspectPropertyValue<Integer> getScreenPixelsPhysicalHeight();
	/**
	 * Indicates the width of the device's screen in physical pixels. This property is not applicable for a device that does not have a screen. For devices such as tablets or TV which are predominantly used in landscape mode, the pixel width will be the larger value compared to the pixel height.
	 *
	 * @return the value of the ScreenPixelsPhysicalWidth property
	 */
	AspectPropertyValue<Integer> getScreenPixelsPhysicalWidth();
	/**
	 * Indicates the width of the device's screen in pixels. This property is not applicable for a device that does not have a screen. For devices such as tablets or TV which are predominantly used in landscape mode, the pixel width will be the larger value compared to the pixel height.
	 *
	 * @return the value of the ScreenPixelsWidth property
	 */
	AspectPropertyValue<Integer> getScreenPixelsWidth();
	/**
	 * JavaScript that can override the property value found by the server using information on the client device. This property is applicable for browsers that support screen pixels width cookie. 
	 *
	 * @return the value of the ScreenPixelsWidthJavaScript property
	 */
	AspectPropertyValue<JavaScript> getScreenPixelsWidthJavaScript();
	/**
	 * Indicates the screen type of the device. This property is not applicable for a device that does not have an integrated screen, e.g. a media hub. If the device manufacturer or vendor does not specify what the screen type of the device is then it is assumed the device has an LCD screen.
	 *
	 * @return the value of the ScreenType property
	 */
	AspectPropertyValue<String> getScreenType();
	/**
	 * Indicates the resolution of the device's second back camera in megapixels.
	 *
	 * @return the value of the SecondBackCameraMegaPixels property
	 */
	AspectPropertyValue<Double> getSecondBackCameraMegaPixels();
	/**
	 * Indicates the resolution of the device's second front camera in megapixels.
	 *
	 * @return the value of the SecondFrontCameraMegaPixels property
	 */
	AspectPropertyValue<Double> getSecondFrontCameraMegaPixels();
	/**
	 * Indicates the diagonal size of the device's second screen in inches. This property is not applicable for a device that does not have a second screen.
	 *
	 * @return the value of the SecondScreenInchesDiagonal property
	 */
	AspectPropertyValue<Double> getSecondScreenInchesDiagonal();
	/**
	 * Indicates the diagonal size of the device's second screen in inches rounded to the nearest whole number. This property will return the value 'N/A' for desktop or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenInchesDiagonalRounded property
	 */
	AspectPropertyValue<Integer> getSecondScreenInchesDiagonalRounded();
	/**
	 * Refers to the height of the device's second screen in inches. This property will return 'N/A' for desktops or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenInchesHeight property
	 */
	AspectPropertyValue<Double> getSecondScreenInchesHeight();
	/**
	 * Indicates the area of the device's second screen in square inches rounded to the nearest whole number. This property will return the value 'N/A' for desktop or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenInchesSquare property
	 */
	AspectPropertyValue<Integer> getSecondScreenInchesSquare();
	/**
	 * Refers to the width of the device's second screen in inches. This property will return the value 'N/A' for desktop or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenInchesWidth property
	 */
	AspectPropertyValue<Double> getSecondScreenInchesWidth();
	/**
	 * Refers to the diagonal size of the second screen of the device in millimetres. This property will return 'N/A' for desktops or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenMMDiagonal property
	 */
	AspectPropertyValue<Double> getSecondScreenMMDiagonal();
	/**
	 * Indicate the diagonal size of the device's second screen in millimetres rounded to the nearest whole number. This property will return the value 'N/A' for desktop or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenMMDiagonalRounded property
	 */
	AspectPropertyValue<Integer> getSecondScreenMMDiagonalRounded();
	/**
	 * Refers to the second screen height of the device in millimetres. This property will return 'N/A' for desktops or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenMMHeight property
	 */
	AspectPropertyValue<Double> getSecondScreenMMHeight();
	/**
	 * Indicates the area of the device's second screen in square millimetres rounded to the nearest whole number. This property will return the value  'N/A' for desktop or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenMMSquare property
	 */
	AspectPropertyValue<Integer> getSecondScreenMMSquare();
	/**
	 * Refers to the second screen width of the device in millimetres. This property will return 'N/A' for desktops or for devices which do not have a second screen.
	 *
	 * @return the value of the SecondScreenMMWidth property
	 */
	AspectPropertyValue<Double> getSecondScreenMMWidth();
	/**
	 * Indicates the height of the device's second screen in pixels. This property is not applicable for a device that does not have a second screen.
	 *
	 * @return the value of the SecondScreenPixelsHeight property
	 */
	AspectPropertyValue<Integer> getSecondScreenPixelsHeight();
	/**
	 * Indicates the width of the device's second screen in pixels. This property is not applicable for a device that does not have a second screen.
	 *
	 * @return the value of the SecondScreenPixelsWidth property
	 */
	AspectPropertyValue<Integer> getSecondScreenPixelsWidth();
	/**
	 * Indicates if the browser supports the querySelector() method that returns the first element matching a specified CSS selector(s) in the document.
	 *
	 * @return the value of the Selector property
	 * @see <a href="http://www.w3.org/TR/selectors-api2/">More information</a>
	 */
	AspectPropertyValue<Boolean> getSelector();
	/**
	 * Contains the Accept-CH HTTP header values to add to the HTTP response for the browser component. UACH values Sec-CH-UA, and Sec-CH-UA-Full-Version are relevant. The default value is Unknown if the browser does not fully support UACH.
	 *
	 * @return the value of the SetHeaderBrowserAccept-CH property
	 * @see <a href="https://learnclienthints.com">More information</a>
	 */
	AspectPropertyValue<String> getSetHeaderBrowserAcceptCH();
	/**
	 * Contains the Accept-CH HTTP header values to add to the HTTP response for the hardware component. UACH values Sec-CH-UA-Model, and Sec-CH-UA-Mobile are relevant. The default value is Unknown if the browser does not fully support UACH.
	 *
	 * @return the value of the SetHeaderHardwareAccept-CH property
	 * @see <a href="https://learnclienthints.com">More information</a>
	 */
	AspectPropertyValue<String> getSetHeaderHardwareAcceptCH();
	/**
	 * Contains the Accept-CH HTTP header values to add to the HTTP response for the platform component. UACH values Sec-CH-UA-Platform, and Sec-CH-UA-Platform-Version are relevant. The default value is Unknown if the browser does not fully support UACH.
	 *
	 * @return the value of the SetHeaderPlatformAccept-CH property
	 * @see <a href="https://learnclienthints.com">More information</a>
	 */
	AspectPropertyValue<String> getSetHeaderPlatformAcceptCH();
	/**
	 * Indicates the primary marketing name of the System on Chip (chipset) which includes the CPU, GPU and modem. e.g. Snapdragon S4
	 *
	 * @return the value of the SoC property
	 */
	AspectPropertyValue<String> getSoC();
	/**
	 * Indicates the Semiconductor Company that designed the System on Chip (chipset) e.g. Qualcomm, Intel or Mediatek.
	 *
	 * @return the value of the SoCDesigner property
	 */
	AspectPropertyValue<String> getSoCDesigner();
	/**
	 * Indicates the official model of the System on Chip (chipset) e.g. MSM8625, MT8312.
	 *
	 * @return the value of the SoCModel property
	 */
	AspectPropertyValue<String> getSoCModel();
	/**
	 * Refers to the list of audio codecs supported by an operating system. This list of codecs is supported for playback on a  basic software installation. The values of this property are the codec's common name.
	 *
	 * @return the value of the SoftwareAudioCodecsDecode property
	 */
	AspectPropertyValue<List<String>> getSoftwareAudioCodecsDecode();
	/**
	 * Refers to the list of audio codecs supported by an operating system. This list of codecs is supported for capture on a basic software installation. The values of this property are the codec's common name.
	 *
	 * @return the value of the SoftwareAudioCodecsEncode property
	 */
	AspectPropertyValue<List<String>> getSoftwareAudioCodecsEncode();
	/**
	 * Refers to the list of video codecs supported by an operating system. This list of codecs is supported for playback on a  basic software installation. The values of this property are the codec's common name.
	 *
	 * @return the value of the SoftwareVideoCodecsDecode property
	 */
	AspectPropertyValue<List<String>> getSoftwareVideoCodecsDecode();
	/**
	 * Refers to the list of video codecs supported by an operating system. This list of codecs is supported for capture on a basic software installation. The values of this property are the codec's common name.
	 *
	 * @return the value of the SoftwareVideoCodecsEncode property
	 */
	AspectPropertyValue<List<String>> getSoftwareVideoCodecsEncode();
	/**
	 * The Specific Absorbtion Rate (SAR) is a measure of the rate at which energy is absorbed by the human body when exposed by a radio frequency electromagnetic field. This property contains values in Watts per Kilogram (W/kg) in accordance with the European Committee for Electrotechnical Standardization (CENELEC).
	 *
	 * @return the value of the SpecificAbsorbtionRateEU property
	 */
	AspectPropertyValue<Double> getSpecificAbsorbtionRateEU();
	/**
	 * The Specific Absorbtion Rate (SAR) is a measure of the rate at which energy is absorbed by the human body when exposed by a radio frequency electromagnetic field. This property contains values in Watts per Kilogram (W/kg) in accordance with the Federal Communications Commission (FCC).
	 *
	 * @return the value of the SpecificAbsorbtionRateUS property
	 */
	AspectPropertyValue<Double> getSpecificAbsorbtionRateUS();
	/**
	 * A list of MIME types the device can stream. The list does not include MIME types that are only supported through the use of 3rd party applications.
	 *
	 * @return the value of the StreamingAccept property
	 */
	AspectPropertyValue<List<String>> getStreamingAccept();
	/**
	 * Refers to the suggested optimum height of a button in millimetres. Ensures the button is touchable on a touch screen and not too large on a non-touch screen. Assumes the actual device DPI (Dots Per Inch) is being used. 
	 *
	 * @return the value of the SuggestedImageButtonHeightMms property
	 */
	AspectPropertyValue<Double> getSuggestedImageButtonHeightMms();
	/**
	 * Refers to the suggested optimum height of a button in millimetres. Ensures the button is touchable on a touch screen and not too large on a non-touch screen. 
	 *
	 * @return the value of the SuggestedImageButtonHeightPixels property
	 */
	AspectPropertyValue<Double> getSuggestedImageButtonHeightPixels();
	/**
	 * Refers to the suggested optimum height of a hyperlink in pixels. Ensures the link is touchable on a touch screen and not too large on a non-touch screen. Assumes the actual device DPI is being used.
	 *
	 * @return the value of the SuggestedLinkSizePixels property
	 */
	AspectPropertyValue<Double> getSuggestedLinkSizePixels();
	/**
	 * Refers to the suggested optimum height of a hyperlink in points. Ensures the link is touchable on a touch screen and not too large on a non-touch screen. 
	 *
	 * @return the value of the SuggestedLinkSizePoints property
	 */
	AspectPropertyValue<Double> getSuggestedLinkSizePoints();
	/**
	 * Indicates the list of wireless data technologies supported by the device, including Bluetooth and Wi-Fi. For example, 4G cellular network technologies includes 'LTE' (Long Term Evolution), and 5G technologies includes 'NR' (New Radio). If the device supports phone calls, the SMS value is also returned.
	 *
	 * @return the value of the SupportedBearers property
	 * @see <a href="http://en.wikipedia.org/wiki/Wireless_Application_Protocol">More information</a>
	 */
	AspectPropertyValue<List<String>> getSupportedBearers();
	/**
	 * Indicates the highest version of Bluetooth the device supports.
	 *
	 * @return the value of the SupportedBluetooth property
	 */
	AspectPropertyValue<Double> getSupportedBluetooth();
	/**
	 * Indicates the Bluetooth profiles the device supports.
	 *
	 * @return the value of the SupportedBluetoothProfiles property
	 * @see <a href="https://en.wikipedia.org/wiki/List_of_Bluetooth_profiles">More information</a>
	 */
	AspectPropertyValue<List<String>> getSupportedBluetoothProfiles();
	/**
	 * This Property is no longer being supported. Please see Properties, SupportedBluetooth and SupportedBluetoothProfiles for the relevant data.
	 *
	 * @return the value of the SupportedBluetoothVersion property
	 * @see <a href="http://en.wikipedia.org/wiki/Bluetooth">More information</a>
	 */
	AspectPropertyValue<String> getSupportedBluetoothVersion();
	/**
	 * Indicates the list of features the device's camera supports.
	 *
	 * @return the value of the SupportedCameraFeatures property
	 */
	AspectPropertyValue<List<String>> getSupportedCameraFeatures();
	/**
	 * Indicates the list of charger types supported by the device. For devices that operate via mains power only, e.g. TVs, MediaHubs (which technically aren't being charged) this property is not applicable.
	 *
	 * @return the value of the SupportedChargerTypes property
	 */
	AspectPropertyValue<List<String>> getSupportedChargerTypes();
	/**
	 * Indicates the list of input and output communications the device can support, for example 3.5mm jack, micro-USB etc.
	 *
	 * @return the value of the SupportedI/O property
	 */
	AspectPropertyValue<List<String>> getSupportedIO();
	/**
	 * Indicates the list of sensors supported by the device. This property may be not applicable for devices without sensors, such as most feature phones and media hubs.
	 *
	 * @return the value of the SupportedSensorTypes property
	 */
	AspectPropertyValue<List<String>> getSupportedSensorTypes();
	/**
	 * Indicates whether the &quot;Universal Integrated Circuit Card (UICC - more commonly known as, SIM)&quot; is removable or embedded. If removable, the form factor of the UICC is returned.
	 *
	 * @return the value of the SupportedSIMCardTypes property
	 */
	AspectPropertyValue<List<String>> getSupportedSIMCardTypes();
	/**
	 * Indicates if the device supports 24p; a video format that operates at 24 frames per second.
	 *
	 * @return the value of the Supports24p property
	 */
	AspectPropertyValue<Boolean> getSupports24p();
	/**
	 * Indicates if the device can receive and make telephone calls using available bearers without any additional software such as VoIP. Devices that support voice calls do not necessarily support phone calls.
	 *
	 * @return the value of the SupportsPhoneCalls property
	 */
	AspectPropertyValue<Boolean> getSupportsPhoneCalls();
	/**
	 * Indicates if the browser supports TLS or SSL, essential for secure protocols such as HTTPS.
	 *
	 * @return the value of the SupportsTls/Ssl property
	 */
	AspectPropertyValue<Boolean> getSupportsTlsSsl();
	/**
	 * Indicates if the browser supports WebGL technology to generate hardware-accelerated 3D graphics.
	 *
	 * @return the value of the SupportsWebGL property
	 */
	AspectPropertyValue<Boolean> getSupportsWebGL();
	/**
	 * Indicates if the device supports Wireless Display Technology.
	 *
	 * @return the value of the SupportsWiDi property
	 */
	AspectPropertyValue<Boolean> getSupportsWiDi();
	/**
	 * Indicates if the browser supports SVG (scalable vector graphics), useful for 2D animations and applications where all objects within the SVG can be accessed via the DOM and can have assigned event listener elements.
	 *
	 * @return the value of the Svg property
	 * @see <a href="http://www.w3.org/TR/SVG/">More information</a>
	 */
	AspectPropertyValue<Boolean> getSvg();
	/**
	 * Indicates the Type Allocation Code (TAC) for devices supporting GSM/3GPP networks which come from multiple sources. This property will return 'N/A' if we cannot determine the device TAC authenticy.
	 *
	 * @return the value of the TAC property
	 */
	AspectPropertyValue<List<String>> getTAC();
	/**
	 * Indicates whether third party cookies are enabled. Where the JavaScript contained in ThirdPartyCookiesEnabledJavaScript has not run this will indicate the likely status.
	 *
	 * @return the value of the ThirdPartyCookiesEnabled property
	 */
	AspectPropertyValue<String> getThirdPartyCookiesEnabled();
	/**
	 * Contains JavaScript that can override the property ThirdPartyCookiesEnabled. This property returns NotSupported for browsers which cannot support third party cookies.
	 *
	 * @return the value of the ThirdPartyCookiesEnabledJavaScript property
	 */
	AspectPropertyValue<JavaScript> getThirdPartyCookiesEnabledJavaScript();
	/**
	 * Indicates if the browser supports the method of registering and interpreting finder (or stylus) activity on touch screens or trackpads.
	 *
	 * @return the value of the TouchEvents property
	 * @see <a href="http://www.w3.org/TR/touch-events/">More information</a>
	 */
	AspectPropertyValue<Boolean> getTouchEvents();
	/**
	 * Indicates if the browser supports a method of tracking text being played with media, e.g. subtitles and captions.
	 *
	 * @return the value of the Track property
	 * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html#the-track-element">More information</a>
	 */
	AspectPropertyValue<Boolean> getTrack();
	/**
	 * The matched User-Agents.
	 *
	 * @return the value of the UserAgents property
	 */
	AspectPropertyValue<List<String>> getUserAgents();
	/**
	 * Indicates if the browser supports the 'Video' element for playing videos on web pages without requiring a plug-in.
	 *
	 * @return the value of the Video property
	 * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html">More information</a>
	 */
	AspectPropertyValue<Boolean> getVideo();
	/**
	 * Indicates if the browser supports Viewport, to give control over view for different screen sizes and resolutions of devices accessing a website.
	 *
	 * @return the value of the Viewport property
	 * @see <a href="http://www.w3.org/TR/mwabp/#bp-viewport">More information</a>
	 */
	AspectPropertyValue<Boolean> getViewport();
	/**
	 * Indicates if the browser supports the WebP image format.
	 *
	 * @return the value of the WebP property
	 */
	AspectPropertyValue<Boolean> getWebP();
	/**
	 * Indicates if the browser supports background workers in JavaScript.
	 *
	 * @return the value of the WebWorkers property
	 * @see <a href="http://www.w3.org/TR/workers/">More information</a>
	 */
	AspectPropertyValue<Boolean> getWebWorkers();
	/**
	 * Indicates the weight of the device with battery in grams.
	 *
	 * @return the value of the WeightWithBattery property
	 */
	AspectPropertyValue<Double> getWeightWithBattery();
	/**
	 * Indicates the weight of the device without battery in grams.
	 *
	 * @return the value of the WeightWithoutBattery property
	 */
	AspectPropertyValue<Double> getWeightWithoutBattery();
	/**
	 * Indicates if the browser supports client-to-server communication with XmlHttpRequests. If the browser supports 'Xhr2' will also support 'DataForm' element. This property may need a vendor prefix, e.g. webkit, moz, etc.
	 *
	 * @return the value of the Xhr2 property
	 * @see <a href="http://www.w3.org/TR/XMLHttpRequest/">More information</a>
	 */
	AspectPropertyValue<Boolean> getXhr2();
}
