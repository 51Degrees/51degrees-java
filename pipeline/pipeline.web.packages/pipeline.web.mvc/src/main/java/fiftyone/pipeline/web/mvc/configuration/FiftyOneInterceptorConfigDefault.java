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

package fiftyone.pipeline.web.mvc.configuration;

import javax.servlet.ServletContext;

import static fiftyone.pipeline.web.Constants.DEFAULT_CLIENTSIDE_CACHE_MAX_AGE_SECONDS;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default implementation of the {@link FiftyOneInterceptorConfig} interface.
 */
public class FiftyOneInterceptorConfigDefault implements FiftyOneInterceptorConfig {

    private String dataFilePath;

    private boolean clientsidePropertiesEnabled;

    private int clientsideCacheMaxAge =
        DEFAULT_CLIENTSIDE_CACHE_MAX_AGE_SECONDS;

    private String javaScriptPath;

    private String jsonPath;

    private boolean sequenceElementEnabled = true;

    private boolean setHeadersElementEnabled = true;

    @Autowired
    public ServletContext servletContext;
    
    public void setDataFilePath(String dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    public void setClientsidePropertiesEnabled(boolean clientsidePropertiesEnabled) {
        this.clientsidePropertiesEnabled = clientsidePropertiesEnabled;
    }

    /**
     * Set how many seconds a browser may keep the client-side JavaScript or
     * JSON before it asks again, the max-age of their Cache-Control header.
     * @param seconds the max-age, 0 or more
     * @throws IllegalArgumentException if seconds is negative
     */
    public void setClientsideCacheMaxAge(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException(
                "The max-age of the client-side responses cannot be " +
                    "negative, and was " + seconds + ".");
        }
        this.clientsideCacheMaxAge = seconds;
    }

    public void setJavaScriptPath(String javaScriptPath) {
        this.javaScriptPath = javaScriptPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public void setSequenceElementEnabled(boolean sequenceElementEnabled) {
        this.sequenceElementEnabled = sequenceElementEnabled;
    }

    public void setSetHeadersElementEnabled(boolean setHeadersElementEnabled) {
        this.setHeadersElementEnabled = setHeadersElementEnabled;
    }

    @Override
    public String getJavaScriptPath() {
        return javaScriptPath;
    }

    @Override
    public String getJsonPath() {
        return jsonPath;
    }

    @Override
    public boolean getSequenceElementEnabled() {
        return sequenceElementEnabled;
    }

    @Override
    public boolean getSetHeadersElementEnabled() {
        return setHeadersElementEnabled;
    }

    @Override
    public String getDataFilePath() {
        return dataFilePath;
    }

    @Override
    public boolean getClientsidePropertiesEnabled() {
        return clientsidePropertiesEnabled;
    }

    @Override
    public int getClientsideCacheMaxAge() {
        return clientsideCacheMaxAge;
    }
}
