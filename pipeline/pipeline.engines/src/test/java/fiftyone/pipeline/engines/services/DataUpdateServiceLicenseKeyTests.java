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

package fiftyone.pipeline.engines.services;

import fiftyone.common.wrappers.io.FileWrapperFactory;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.update.FutureFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The data update URL carries the license keys in its query. Every log
 * entry the data update service writes about a request, at any level and
 * with any exception it logs, shows each key only by its last 4
 * characters.
 */
public class DataUpdateServiceLicenseKeyTests {

    private static final String KEY = "TESTKEYNOTREALABCDEFGHIJ1234";
    private static final String MASKED = "************************1234";
    private static final String OTHER_KEY = "OTHERTESTKEYNOTREALXYZ5678";
    private static final String OTHER_MASKED = "**********************5678";
    private static final String UPDATE_URL =
        "https://distributor.example/api/v2/download";

    private Logger logger;
    private HttpClient httpClient;
    private HttpURLConnection connection;
    private DataUpdateServiceDefault service;
    private OnPremiseAspectEngine<? extends AspectData,
        ? extends AspectPropertyMetaData> engine;

    @Before
    @SuppressWarnings("unchecked")
    public void init() throws IOException {
        logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(logger.isInfoEnabled()).thenReturn(true);
        when(logger.isWarnEnabled()).thenReturn(true);
        when(logger.isErrorEnabled()).thenReturn(true);
        connection = mock(HttpURLConnection.class);
        httpClient = mock(HttpClient.class);
        when(httpClient.connect(any(URL.class))).thenReturn(connection);
        service = new DataUpdateServiceDefault(
            logger,
            httpClient,
            mock(FileWrapperFactory.class),
            mock(FutureFactory.class));

        // A data file updated in memory, whose URL carries the keys as the
        // 51Degrees URL formatter puts them.
        final DataFileConfiguration config = new DataFileConfigurationDefault();
        config.setDataUpdateUrl(UPDATE_URL);
        config.setDataUpdateLicenseKeys(Arrays.asList(KEY, OTHER_KEY));
        config.setUrlFormatter(dataFile -> config.getDataUpdateUrl() +
            "?LicenseKeys=" + KEY + "|" + OTHER_KEY +
            "&Download=True&Type=HashV41");
        config.setVerifyModifiedSince(false);
        config.setFileSystemWatcherEnabled(false);
        config.setAutomaticUpdatesEnabled(false);
        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setConfiguration(config);

        engine = mock(OnPremiseAspectEngine.class);
        when(engine.getDataFileMetaData()).thenReturn(file);
    }

    @Test
    public void Forbidden_LogsMaskedKeys() throws IOException {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
        assertLogsMaskedKeys();
    }

    @Test
    public void TooManyRequests_LogsMaskedKeys() throws IOException {
        when(connection.getResponseCode()).thenReturn(429);
        assertLogsMaskedKeys();
    }

    @Test
    public void OtherStatus_LogsMaskedKeys() throws IOException {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        assertLogsMaskedKeys();
    }

    @Test
    public void NotModified_LogsMaskedKeys() throws IOException {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        assertLogsMaskedKeys();
    }

    @Test
    public void NoResponse_LogsMaskedKeys() throws IOException {
        when(httpClient.connect(any(URL.class))).thenReturn(null);
        assertLogsMaskedKeys();
    }

    /**
     * The exception an HTTP library raises often names the URL it
     * requested, and the exception is logged with its stack trace.
     */
    @Test
    public void RequestException_LogsMaskedKeys() throws IOException {
        when(connection.getResponseCode()).thenThrow(new IOException(
            "Server returned HTTP response code: 403 for URL: " +
                UPDATE_URL + "?LicenseKeys=" + KEY + "|" + OTHER_KEY));
        assertLogsMaskedKeys();
    }

    /**
     * A key typed straight into the data update URL, rather than given as
     * a data update license key, is masked too.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void KeyTypedIntoTheUrl_LogsMaskedKey() throws IOException {
        DataFileConfiguration config = new DataFileConfigurationDefault();
        config.setDataUpdateUrl(UPDATE_URL + "?LicenseKeys=" + KEY +
            "&Download=True&Type=HashV41");
        config.setVerifyModifiedSince(false);
        config.setFileSystemWatcherEnabled(false);
        config.setAutomaticUpdatesEnabled(false);
        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setConfiguration(config);
        OnPremiseAspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData> typed =
            mock(OnPremiseAspectEngine.class);
        when(typed.getDataFileMetaData()).thenReturn(file);
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_FORBIDDEN);

        service.checkForUpdate(typed);

        String logged = logged();
        assertFalse(logged, logged.contains(KEY));
        assertTrue(logged, logged.contains(MASKED));
    }

    private void assertLogsMaskedKeys() {
        service.checkForUpdate(engine);

        String logged = logged();
        assertFalse(logged, logged.contains(KEY));
        assertFalse(logged, logged.contains(OTHER_KEY));
        assertTrue(logged, logged.contains(MASKED));
        assertTrue(logged, logged.contains(OTHER_MASKED));
    }

    /**
     * Everything passed to the logger, at every level, with the stack trace
     * of each throwable as it would be printed.
     */
    private String logged() {
        StringBuilder text = new StringBuilder();
        for (Invocation invocation :
            Mockito.mockingDetails(logger).getInvocations()) {
            text.append(invocation.getMethod().getName()).append(':');
            for (Object argument : invocation.getArguments()) {
                append(text, argument);
            }
            text.append('\n');
        }
        return text.toString();
    }

    private static void append(StringBuilder text, Object argument) {
        if (argument instanceof Object[]) {
            for (Object item : (Object[]) argument) {
                append(text, item);
            }
        } else if (argument instanceof Throwable) {
            StringWriter trace = new StringWriter();
            ((Throwable) argument).printStackTrace(new PrintWriter(trace));
            text.append(' ').append(trace);
        } else {
            text.append(' ').append(argument);
        }
    }
}
