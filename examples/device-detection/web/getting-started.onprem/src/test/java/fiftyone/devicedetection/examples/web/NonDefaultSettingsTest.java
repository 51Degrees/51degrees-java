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

package fiftyone.devicedetection.examples.web;

import org.junit.Test;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import static fiftyone.devicedetection.examples.web.GettingStartedWebOnPrem.resourceBase;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The on-premise web example sets and documents a commonly used setting
 * that is not the default, suppressing exceptions from processing so that
 * a failure does not stop a production web site
 * (device-detection-specification/required-examples.md, "Examples MUST also
 * demonstrate or at least document commonly used, but non-default
 * settings").
 */
public class NonDefaultSettingsTest {

    @Test
    public void suppressProcessExceptionIsSetAndDocumented() throws Exception {
        File file = getFilePath(resourceBase + "/WEB-INF/51Degrees-OnPrem.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(
            "http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(file);
        Element root = document.getDocumentElement();

        // The pipeline's own build parameters, not an element's.
        Element parameters = null;
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element &&
                "BuildParameters".equals(child.getNodeName())) {
                parameters = (Element) child;
            }
        }
        assertNotNull("The pipeline should have build parameters", parameters);
        NodeList suppress = parameters.getElementsByTagName(
            "SuppressProcessException");
        assertEquals(1, suppress.getLength());
        assertEquals("true", suppress.item(0).getTextContent().trim());

        // A comment before the setting says what it does.
        Node previous = parameters.getPreviousSibling();
        while (previous != null && previous instanceof Comment == false) {
            previous = previous.getPreviousSibling();
        }
        assertNotNull("The setting should be documented", previous);
        assertTrue(previous.getTextContent(),
            previous.getTextContent().contains("Suppress"));
    }
}
