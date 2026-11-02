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

package fiftyone.geolocation.web.examples.mvc.controller;

import fiftyone.geolocation.core.data.GeoData;
import fiftyone.pipeline.core.data.FlowData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import fiftyone.pipeline.web.mvc.components.FlowDataProvider;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/")
public class ExampleController {

    private FlowDataProvider flowDataProvider;

    @Autowired
    public ExampleController(FlowDataProvider flowDataProvider) {
        this.flowDataProvider = flowDataProvider;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String get(ModelMap model, HttpServletRequest request) {
        FlowData data = flowDataProvider.getFlowData(request);
        GeoData geo = data.get(GeoData.class);

        model.addAttribute("town", geo.getTown());
        model.addAttribute("county", geo.getCounty());
        model.addAttribute("state", geo.getState());
        model.addAttribute("country", geo.getCountry());
        return "example";
    }
}