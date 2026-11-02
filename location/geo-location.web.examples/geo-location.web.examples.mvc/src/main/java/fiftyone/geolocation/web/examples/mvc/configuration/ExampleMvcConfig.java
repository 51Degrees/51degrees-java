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

package fiftyone.geolocation.web.examples.mvc.configuration;

import fiftyone.pipeline.web.mvc.components.FiftyOneInterceptor;
import fiftyone.pipeline.web.mvc.configuration.FiftyOneInterceptorConfig;
import fiftyone.pipeline.web.mvc.configuration.FiftyOneInterceptorConfigDefault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import javax.servlet.ServletContext;

import static fiftyone.pipeline.web.mvc.components.FiftyOneInterceptor.enableClientsideProperties;


/**
 * @example mvc/configuration/ExampleMvcConfig.java
 * Spring MVC device detection example
 *
 * This example shows how to:
 *
 * 1. Set up configuration options to add elements to the 51Degrees Pipeline.
 * 
 * @include geo-location.web.examples.mvc/src/main/webapp/WEB-INF/51Degrees-Cloud.xml
 *
 * 2. Set up MVC, enable configuration, and add the Pipeline component.
 *
 * 3. Configure the interceptor.
 *
 * 4. Enable client-side code to improve detection accuracy on devices like iPhones.
 *
 * 5. Add the interceptor.
 *
 * 6. Inject the `FlowDataProvider` into a controller.
 * ```{java}
 * @Controller
 * @RequestMapping("/")
 * public class ExampleController {
 *     private FlowDataProvider flowDataProvider;
 *
 *     @Autowired
 *     public ExampleController(FlowDataProvider flowDataProvider) {
 *         this.flowDataProvider = flowDataProvider;
 *     }
 *     ...
 * ```
 *
 * 7. Use the results contained in the flow data to display something on a page view.
 * ```{java}
 * @Controller
 * @RequestMapping("/")
 * public class ExampleController {
 *     ...
 *     @RequestMapping(method = RequestMethod.GET)
 *     public String get(ModelMap model, HttpServletRequest request) {
 *         FlowData data = flowDataProvider.getFlowData(request);
 *         GeoData geo = data.get(GeoData.class);
 *
 *         model.addAttribute("town", geo.getTown());
 *         model.addAttribute("county", geo.getCounty());
 *         model.addAttribute("state", geo.getState());
 *         model.addAttribute("country", geo.getCountry());
 *         return "example";
 *     }
 *     ...
 * ```
 * ## Controller
 
 * @include mvc/controller/ExampleController.java
 *
 * ## Config
 */
@EnableWebMvc
@Configuration
@ComponentScan({"fiftyone.geolocation.web.examples.mvc.controller","fiftyone.pipeline.web.mvc"})
public class ExampleMvcConfig implements WebMvcConfigurer {

    public ExampleMvcConfig() {
        super();
    }

    @Override
    public void addViewControllers(final ViewControllerRegistry registry) {
        enableClientsideProperties(registry);
    }

    @Autowired
    ServletContext servletContext;

    @Bean
    public FiftyOneInterceptorConfig fiftyOneInterceptorConfig() {
        final FiftyOneInterceptorConfigDefault bean = new FiftyOneInterceptorConfigDefault();

        bean.setDataFilePath(servletContext.getRealPath("/WEB-INF/51Degrees-Cloud.xml"));
        bean.setClientsidePropertiesEnabled(true);

        return bean;
    }

    @Bean
    public ViewResolver viewResolver() {
        final InternalResourceViewResolver bean = new InternalResourceViewResolver();

        bean.setViewClass(JstlView.class);
        bean.setPrefix("/WEB-INF/views/");
        bean.setSuffix(".jsp");

        return bean;
    }

    @Autowired
    FiftyOneInterceptor fiftyOneInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(fiftyOneInterceptor);
    }
}
