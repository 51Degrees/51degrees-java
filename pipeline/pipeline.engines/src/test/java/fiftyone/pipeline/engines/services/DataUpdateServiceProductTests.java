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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fiftyone.common.wrappers.io.FileWrapperFactory;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.DataUpdateUrlFormatter;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.DataUpdateService.AutoUpdateStatus;
import fiftyone.pipeline.engines.services.update.FutureFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A license key can hold several products. The data update service then
 * answers a request that names no product with 300 Multiple Choices and a
 * JSON body that lists one URI for each product. The update repeats its
 * request with the product the data file needs added, and fails with an
 * error that names the products offered when none of them is that product.
 * <p>
 * The data update service here is a server in this process, so the tests
 * need no license key.
 */
public class DataUpdateServiceProductTests {

    private static final String KEY = "TESTKEYNOTREALABCDEFGHIJ1234";
    private static final String PATH = "/api/v2/download";
    private static final String FIRST_QUERY =
        "LicenseKeys=" + KEY + "&Download=True&Type=HashV41";
    private static final byte[] TAC_DATA =
        "data for V4TAC".getBytes(StandardCharsets.UTF_8);
    private static final byte[] IPI_DATA =
        "data for IPIV4Enterprise".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;
    private String address;
    /**
     * The query of each request the server received, in order.
     */
    private final List<String> requests =
        Collections.synchronizedList(new ArrayList<String>());
    /**
     * The status the server gives a request that names no product.
     */
    private volatile int firstStatus = 300;
    /**
     * Whether the 300 response carries a Location header, which names the
     * first URI offered.
     */
    private volatile boolean sendLocation = true;
    /**
     * The status the server gives a request that names a product. A 300
     * carries the same body as the one for a request that names none.
     */
    private volatile int productStatus = 200;
    /**
     * The body of the 300 response, or null for the list of the two
     * products.
     */
    private volatile String choicesBody = null;

    private Logger logger;
    private DataUpdateServiceDefault service;
    private OnPremiseAspectEngine<? extends AspectData,
        ? extends AspectPropertyMetaData> engine;
    private AspectEngineDataFileDefault file;

    @Before
    public void init() throws IOException {
        server = HttpServer.create(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        address = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext(PATH, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                respond(exchange);
            }
        });
        server.createContext("/moved", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                requests.add("moved");
                exchange.getResponseHeaders().add("Location",
                    address + PATH + "?" + FIRST_QUERY + "&Product=V4TAC");
                send(exchange, 302, new byte[0]);
            }
        });
        server.createContext("/moved-to-choices", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                requests.add("moved-to-choices");
                // A location relative to the address requested.
                exchange.getResponseHeaders().add("Location",
                    PATH + "?" + FIRST_QUERY);
                send(exchange, 307, new byte[0]);
            }
        });
        server.createContext("/moved-in-a-circle", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                requests.add("moved-in-a-circle");
                exchange.getResponseHeaders().add("Location",
                    address + "/moved-in-a-circle?" + FIRST_QUERY);
                send(exchange, 302, new byte[0]);
            }
        });
        server.start();

        logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(logger.isInfoEnabled()).thenReturn(true);
        when(logger.isWarnEnabled()).thenReturn(true);
        when(logger.isErrorEnabled()).thenReturn(true);
        service = new DataUpdateServiceDefault(
            logger,
            new HttpClientDefault(),
            mock(FileWrapperFactory.class),
            mock(FutureFactory.class));
    }

    @After
    public void stop() {
        server.stop(0);
    }

    /**
     * The key holds two products and the data file needs the second one
     * listed. The first request is repeated with that product added, and
     * the update is that product's data.
     */
    @Test
    public void SeveralProducts_RepeatsTheRequestWithTheProductNeeded() {
        configure(PATH, "V4TAC");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged, AutoUpdateStatus.AUTO_UPDATE_SUCCESS, status);
        verify(engine).refreshData(any(), eq(TAC_DATA));
        assertEquals(2, requests.size());
        assertEquals(FIRST_QUERY, requests.get(0));
        assertEquals(FIRST_QUERY + "&Product=V4TAC", requests.get(1));
        assertTrue(logged, logged.contains("Requesting product"));
        assertFalse(logged, logged.contains(KEY));
    }

    /**
     * The product is matched whatever its letter case, and is asked for as
     * the response wrote it.
     */
    @Test
    public void SeveralProducts_MatchesTheProductIgnoringCase() {
        configure(PATH, "ipiv4enterprise");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        assertEquals(logged(), AutoUpdateStatus.AUTO_UPDATE_SUCCESS, status);
        verify(engine).refreshData(any(), eq(IPI_DATA));
        assertEquals(FIRST_QUERY + "&Product=IPIV4Enterprise",
            requests.get(1));
    }

    /**
     * The 300 response is handled the same way when it has no Location
     * header.
     */
    @Test
    public void SeveralProducts_NoLocationHeader_RepeatsTheRequestWithTheProductNeeded() {
        sendLocation = false;
        configure(PATH, "V4TAC");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        assertEquals(logged(), AutoUpdateStatus.AUTO_UPDATE_SUCCESS, status);
        verify(engine).refreshData(any(), eq(TAC_DATA));
    }

    /**
     * None of the products offered is the one the data file needs. The
     * update fails, the engine keeps its data, and the error names the
     * products offered without showing the license key their URIs carry.
     */
    @Test
    public void SeveralProducts_NoneIsTheProductNeeded_FailsNamingTheProductsOffered() {
        configure(PATH, "V4Enterprise");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged, AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR, status);
        verify(engine, never()).refreshData(any(), any(byte[].class));
        assertEquals(logged, 1, requests.size());
        assertTrue(logged, logged.contains(
            "Product 'V4Enterprise' is not offered by data update service " +
            "at '"));
        assertTrue(logged, logged.contains(
            "Products offered: 'IPIV4Enterprise,V4TAC'"));
        assertFalse(logged, logged.contains(KEY));
    }

    /**
     * The data file does not say which product it needs. The update fails
     * and the error names the products offered.
     */
    @Test
    public void SeveralProducts_ProductNeededNotKnown_FailsNamingTheProductsOffered() {
        configure(PATH, null);

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged, AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR, status);
        verify(engine, never()).refreshData(any(), any(byte[].class));
        assertEquals(logged, 1, requests.size());
        assertTrue(logged, logged.contains(
            "Product '' is not offered by data update service at '"));
        assertTrue(logged, logged.contains(
            "Products offered: 'IPIV4Enterprise,V4TAC'"));
        assertFalse(logged, logged.contains(KEY));
    }

    /**
     * A key that holds one product gets its data from the first request,
     * and no second request is made.
     */
    @Test
    public void OneProduct_UpdatesFromTheFirstRequest() {
        firstStatus = 200;
        configure(PATH, "V4TAC");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        assertEquals(logged(), AutoUpdateStatus.AUTO_UPDATE_SUCCESS, status);
        verify(engine).refreshData(any(), eq(TAC_DATA));
        assertEquals(1, requests.size());
        assertFalse(requests.get(0), requests.get(0).contains("Product="));
    }

    /**
     * A key that does not hold the product asked for is reported as access
     * denied.
     */
    @Test
    public void Forbidden_ReportsAccessDenied() {
        firstStatus = 403;
        configure(PATH, "V4TAC");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged,
            AutoUpdateStatus.AUTO_UPDATE_ERR_403_FORBIDDEN, status);
        verify(engine, never()).refreshData(any(), any(byte[].class));
        assertEquals(1, requests.size());
        assertTrue(logged, logged.contains(
            "Access denied to data update service"));
    }

    /**
     * A redirect that names one new location is followed to it, with one
     * request to each address.
     */
    @Test
    public void Redirect_IsFollowed() {
        configure("/moved", "V4TAC");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged, AutoUpdateStatus.AUTO_UPDATE_SUCCESS, status);
        verify(engine).refreshData(any(), eq(TAC_DATA));
        assertEquals(requests.toString(), 2, requests.size());
        assertEquals("moved", requests.get(0));
        assertEquals(FIRST_QUERY + "&Product=V4TAC", requests.get(1));
        assertFalse(logged, logged.contains(KEY));
    }

    /**
     * A redirect leads to an address that answers 300. The product needed
     * is chosen there as it is for a first request, and the request to that
     * address is repeated with the product added.
     */
    @Test
    public void Redirect_ToSeveralProducts_RepeatsTheRequestWithTheProductNeeded() {
        configure("/moved-to-choices", "V4TAC");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        assertEquals(logged(), AutoUpdateStatus.AUTO_UPDATE_SUCCESS, status);
        verify(engine).refreshData(any(), eq(TAC_DATA));
        assertEquals(requests.toString(), 3, requests.size());
        assertEquals("moved-to-choices", requests.get(0));
        assertEquals(FIRST_QUERY, requests.get(1));
        assertEquals(FIRST_QUERY + "&Product=V4TAC", requests.get(2));
    }

    /**
     * Redirects that never reach a data file end the check with an error
     * after a fixed number of requests.
     */
    @Test
    public void Redirect_InACircle_Fails() {
        configure("/moved-in-a-circle", "V4TAC");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged, AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR, status);
        verify(engine, never()).refreshData(any(), any(byte[].class));
        assertTrue(requests.toString(), requests.size() <= 21);
        assertTrue(logged, logged.contains("HTTP status code '302'"));
        assertFalse(logged, logged.contains(KEY));
    }

    /**
     * The request that names the product chosen is answered with 300 too.
     * It is not followed again, and is reported by its status.
     */
    @Test
    public void SeveralProducts_SecondRequestAnswers300_IsNotFollowed() {
        productStatus = 300;
        configure(PATH, "V4TAC");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged, AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR, status);
        verify(engine, never()).refreshData(any(), any(byte[].class));
        assertEquals(requests.toString(), 2, requests.size());
        assertEquals(FIRST_QUERY + "&Product=V4TAC", requests.get(1));
        assertTrue(logged, logged.contains("HTTP status code '300'"));
        assertFalse(logged, logged.contains(KEY));
    }

    /**
     * A 300 whose body is not JSON is reported by its status.
     */
    @Test
    public void SeveralProducts_BodyIsNotJson_ReportsTheStatus() {
        choicesBody = "<html>Multiple choices</html>";
        assertReportsTheStatus();
    }

    /**
     * A 300 whose JSON body has no list of URIs is reported by its status.
     */
    @Test
    public void SeveralProducts_BodyHasNoList_ReportsTheStatus() {
        choicesBody = "{\"Error\":\"There are multiple products\"}";
        assertReportsTheStatus();
    }

    /**
     * A 300 whose list of URIs names no product is reported by its status.
     */
    @Test
    public void SeveralProducts_ListNamesNoProduct_ReportsTheStatus() {
        choicesBody = "{\"AvailableUris\":[\"" + address + PATH +
            "?LicenseKeys=" + KEY + "&Type=HashV41\"]}";
        assertReportsTheStatus();
    }

    /**
     * The name of the product parameter in a URI offered is matched
     * whatever its letter case.
     */
    @Test
    public void SeveralProducts_ParameterNameIsMatchedIgnoringCase() {
        choicesBody = "{\"AvailableUris\":[\"" + address + PATH +
            "?LicenseKeys=" + KEY + "&Type=HashV41&product=V4TAC\"]}";
        configure(PATH, "V4TAC");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        assertEquals(logged(), AutoUpdateStatus.AUTO_UPDATE_SUCCESS, status);
        verify(engine).refreshData(any(), eq(TAC_DATA));
        assertEquals(FIRST_QUERY + "&Product=V4TAC", requests.get(1));
    }

    /**
     * The URL formatter raises an error when asked for the product needed.
     * The product is then not known, and the error names the products
     * offered.
     */
    @Test
    public void SeveralProducts_ProductLookupRaisesAnError_FailsNamingTheProductsOffered() {
        configure(PATH, null);
        file.getConfiguration().setUrlFormatter(new ProductFormatter(null) {
            @Override
            public String getDataUpdateProduct(AspectEngineDataFile dataFile) {
                throw new IllegalStateException("closed");
            }
        });

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged, AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR, status);
        assertEquals(logged, 1, requests.size());
        assertTrue(logged, logged.contains(
            "Product '' is not offered by data update service at '"));
        assertTrue(logged, logged.contains(
            "Products offered: 'IPIV4Enterprise,V4TAC'"));
    }

    /**
     * A product is configured, so the first request names it and gets its
     * data.
     */
    @Test
    public void ConfiguredProduct_IsNamedInTheFirstRequest() {
        configure(PATH, null);
        file.getConfiguration().setUrlFormatter(
            new ConfiguredFormatter("V4TAC"));

        AutoUpdateStatus status = service.checkForUpdate(engine);

        assertEquals(logged(), AutoUpdateStatus.AUTO_UPDATE_SUCCESS, status);
        verify(engine).refreshData(any(), eq(TAC_DATA));
        assertEquals(requests.toString(), 1, requests.size());
        assertEquals(FIRST_QUERY + "&Product=V4TAC", requests.get(0));
    }

    /**
     * A product is configured and the response is 300. No product is chosen
     * from it and no second request is made. It is reported by its status.
     */
    @Test
    public void ConfiguredProduct_Answers300_IsNotFollowed() {
        productStatus = 300;
        configure(PATH, null);
        file.getConfiguration().setUrlFormatter(
            new ConfiguredFormatter("V4TAC"));

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged, AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR, status);
        verify(engine, never()).refreshData(any(), any(byte[].class));
        assertEquals(requests.toString(), 1, requests.size());
        assertTrue(logged, logged.contains("HTTP status code '300'"));
        assertFalse(logged, logged.contains("Requesting product"));
    }

    /**
     * A product is configured that the license key does not hold. It is
     * reported as access denied.
     */
    @Test
    public void ConfiguredProduct_NotHeld_ReportsAccessDenied() {
        configure(PATH, null);
        file.getConfiguration().setUrlFormatter(
            new ConfiguredFormatter("V4Enterprise"));

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged,
            AutoUpdateStatus.AUTO_UPDATE_ERR_403_FORBIDDEN, status);
        assertEquals(requests.toString(), 1, requests.size());
        assertTrue(logged, logged.contains(
            "Access denied to data update service"));
    }

    private void assertReportsTheStatus() {
        configure(PATH, "V4TAC");

        AutoUpdateStatus status = service.checkForUpdate(engine);

        String logged = logged();
        assertEquals(logged, AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR, status);
        verify(engine, never()).refreshData(any(), any(byte[].class));
        assertEquals(requests.toString(), 1, requests.size());
        assertTrue(logged, logged.contains("HTTP status code '300'"));
        assertFalse(logged, logged.contains("is not offered"));
        assertFalse(logged, logged.contains(KEY));
    }

    /**
     * The fake data update service. A request that names a product the
     * license key holds gets that product's data, or the status under test
     * for such a request. A request that names another product gets 403.
     * One that names none gets the status under test.
     */
    private void respond(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        requests.add(query);
        boolean namesProduct = query.contains("Product=");
        if (namesProduct && productStatus == 200 && query.contains(
            "Download=True&Type=HashV41&Product=V4TAC")) {
            send(exchange, 200, TAC_DATA);
        } else if (namesProduct && productStatus == 200 && query.contains(
            "Download=True&Type=HashV41&Product=IPIV4Enterprise")) {
            send(exchange, 200, IPI_DATA);
        } else if (namesProduct && productStatus == 200) {
            send(exchange, 403, ("{\"Error\":\"The license key provided " +
                "does not contain the requested product.\"}")
                .getBytes(StandardCharsets.UTF_8));
        } else if (namesProduct || firstStatus == 300) {
            String body = choicesBody != null ? choicesBody :
                "{\"AvailableUris\":[\"" +
                productUri("IPIV4Enterprise") + "\",\"" +
                productUri("V4TAC") + "\"],\"Error\":\"There are " +
                "multiple products associated with this license key. " +
                "Please use the URI returned in the Location header.\"}";
            exchange.getResponseHeaders().add(
                "Content-Type", "application/json");
            if (sendLocation) {
                exchange.getResponseHeaders().add(
                    "Location", productUri("IPIV4Enterprise"));
            }
            send(exchange, 300, body.getBytes(StandardCharsets.UTF_8));
        } else if (firstStatus == 403) {
            send(exchange, 403, ("{\"Error\":\"The license key provided " +
                "does not contain the requested product.\"}")
                .getBytes(StandardCharsets.UTF_8));
        } else {
            send(exchange, firstStatus, TAC_DATA);
        }
    }

    private String productUri(String product) {
        return address + PATH + "?LicenseKeys=" + KEY +
            "&LicenceKeys=&Type=HashV41&Product=" + product;
    }

    private static void send(
        HttpExchange exchange,
        int status,
        byte[] body) throws IOException {
        exchange.sendResponseHeaders(
            status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
        exchange.close();
    }

    /**
     * A data file held in memory that is updated from the given path of the
     * fake data update service, and that needs the given product.
     */
    @SuppressWarnings("unchecked")
    private void configure(String path, String product) {
        DataFileConfiguration config = new DataFileConfigurationDefault();
        config.setDataUpdateUrl(address + path);
        config.setDataUpdateLicenseKeys(Collections.singletonList(KEY));
        config.setUrlFormatter(new ProductFormatter(product));
        config.setVerifyModifiedSince(false);
        config.setVerifyMd5(false);
        config.setDecompressContent(false);
        config.setFileSystemWatcherEnabled(false);
        config.setAutomaticUpdatesEnabled(false);
        file = new AspectEngineDataFileDefault();
        file.setConfiguration(config);
        engine = mock(OnPremiseAspectEngine.class);
        when(engine.getDataFileMetaData()).thenReturn(file);
        file.setEngine(engine);
    }

    /**
     * Formats the URL as the 51Degrees URL formatter does, and says which
     * product the data file needs.
     */
    private static class ProductFormatter implements DataUpdateUrlFormatter {
        private final String product;

        ProductFormatter(String product) {
            this.product = product;
        }

        @Override
        public String getFormattedDataUpdateUrl(AspectEngineDataFile dataFile) {
            return dataFile.getConfiguration().getDataUpdateUrl() +
                "?" + FIRST_QUERY;
        }

        @Override
        public String getDataUpdateProduct(AspectEngineDataFile dataFile) {
            return product;
        }
    }

    /**
     * Formats the URL with a configured product, as the 51Degrees URL
     * formatter does when it is built with one.
     */
    private static class ConfiguredFormatter extends ProductFormatter {
        private final String configured;

        ConfiguredFormatter(String product) {
            super(product);
            this.configured = product;
        }

        @Override
        public String getFormattedDataUpdateUrl(AspectEngineDataFile dataFile) {
            return super.getFormattedDataUpdateUrl(dataFile) +
                "&Product=" + configured;
        }
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
