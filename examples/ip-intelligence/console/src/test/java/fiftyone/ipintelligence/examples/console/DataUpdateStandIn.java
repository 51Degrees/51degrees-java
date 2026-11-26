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

package fiftyone.ipintelligence.examples.console;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/**
 * A stand-in for the Distributor on this machine, so that a test of a data
 * file update example never reaches the Distributor. It answers as the
 * Distributor does for a license key that holds several products, whatever
 * key it is sent:
 * <ul>
 *     <li>a request that names no product gets 300 (Multiple choices) with
 *     the list of products and a Location header naming the first</li>
 *     <li>a request for a product not on the list gets 403 (Forbidden)</li>
 *     <li>a request for a product on the list gets the data file to serve,
 *     compressed and with its MD5 hash, unless there is no file to serve or
 *     the request's If-Modified-Since is not earlier than the file's
 *     published date, when it gets 304 (Not modified)</li>
 * </ul>
 * Every request is recorded, so a test can check what the example asked
 * for.
 */
final class DataUpdateStandIn implements Closeable {

    /**
     * Where the published date starts in the header of a Hash or an IP
     * Intelligence data file. The date is a 16 bit year, little endian,
     * then a byte each for the month and the day.
     */
    private static final int PUBLISHED_OFFSET = 66;

    /**
     * The path of the Distributor's download address.
     */
    private static final String PATH = "/api/v2/download";

    private static final String NOT_OFFERED =
        "{\"Error\":\"The license key provided does not contain the " +
        "requested product.\"}";

    private static final String SEVERAL_PRODUCTS =
        "There are multiple products associated with this license key. " +
        "Please use the URI returned in the Location header.";

    private static final int BUFFER_SIZE = 1 << 16;

    /**
     * A request the stand-in received, with the product it named, or null
     * for none, and the status the stand-in answered with.
     */
    static final class Request {
        final String product;
        final int status;

        Request(String product, int status) {
            this.product = product;
            this.status = status;
        }

        @Override
        public String toString() {
            return (product == null ? "no product" : product) + " " + status;
        }
    }

    private final List<String> products;

    private final Path compressed;

    private final String md5;

    private final LocalDate published;

    private final HttpServer server;

    private final String url;

    private final List<Request> requests = new ArrayList<>();

    private DataUpdateStandIn(List<String> products, Path dataFile)
        throws IOException {
        this.products = new ArrayList<>(products);
        if (dataFile != null) {
            published = readPublished(dataFile);
            compressed = Paths.get(dataFile + ".gz");
            compress(dataFile, compressed);
            // The Distributor sends the MD5 hash of the compressed file as
            // lower case hexadecimal, which the data update service checks.
            md5 = md5(compressed);
        } else {
            published = null;
            compressed = null;
            md5 = null;
        }
        server = HttpServer.create(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        url = "http://127.0.0.1:" + server.getAddress().getPort() + PATH;
    }

    /**
     * Starts a stand-in on this machine.
     * @param products the products the license key holds, in the order the
     *                 Distributor lists them
     * @param dataFile the data file to serve, or null to answer every
     *                 request for a product on the list with 304 (Not
     *                 modified)
     * @return the stand-in, which is closed to stop it
     * @throws IOException if the data file cannot be read or the server
     * cannot start
     */
    static DataUpdateStandIn start(List<String> products, Path dataFile)
        throws IOException {
        DataUpdateStandIn standIn = new DataUpdateStandIn(products, dataFile);
        standIn.server.createContext(PATH, standIn::answer);
        standIn.server.start();
        return standIn;
    }

    /**
     * @return the data update URL to give an example in place of the
     * Distributor's
     */
    String getUrl() {
        return url;
    }

    /**
     * @return every request received so far, in the order received
     */
    List<Request> requests() {
        synchronized (requests) {
            return new ArrayList<>(requests);
        }
    }

    /**
     * The published date in a data file's header.
     * @param dataFile the data file
     * @return the date
     * @throws IOException if the file cannot be read
     */
    static LocalDate readPublished(Path dataFile) throws IOException {
        try (RandomAccessFile file =
                 new RandomAccessFile(dataFile.toFile(), "r")) {
            file.seek(PUBLISHED_OFFSET);
            int low = file.readUnsignedByte();
            int high = file.readUnsignedByte();
            int month = file.readUnsignedByte();
            int day = file.readUnsignedByte();
            return LocalDate.of(low | (high << 8), month, day);
        }
    }

    /**
     * Sets the published date in a data file's header, so that a copy of a
     * data file can stand for a newer one.
     * @param dataFile the data file
     * @param date the date to set
     * @throws IOException if the file cannot be written
     */
    static void writePublished(Path dataFile, LocalDate date)
        throws IOException {
        try (RandomAccessFile file =
                 new RandomAccessFile(dataFile.toFile(), "rw")) {
            file.seek(PUBLISHED_OFFSET);
            file.write(new byte[]{
                (byte) date.getYear(),
                (byte) (date.getYear() >> 8),
                (byte) date.getMonthValue(),
                (byte) date.getDayOfMonth()});
        }
    }

    @Override
    public void close() throws IOException {
        server.stop(0);
        if (compressed != null) {
            Files.deleteIfExists(compressed);
        }
    }

    private void answer(HttpExchange exchange) throws IOException {
        try {
            String product = productOf(exchange.getRequestURI().getRawQuery());
            if (product == null) {
                List<String> uris = new ArrayList<>();
                for (String offered : products) {
                    uris.add(withProduct(
                        exchange.getRequestURI().getRawQuery(), offered));
                }
                record(null, 300);
                exchange.getResponseHeaders().add("Location", uris.get(0));
                sendJson(exchange, 300, "{\"AvailableUris\":[\"" +
                    String.join("\",\"", uris) + "\"],\"Error\":\"" +
                    SEVERAL_PRODUCTS + "\"}");
            } else if (offers(product) == false) {
                record(product, 403);
                sendJson(exchange, 403, NOT_OFFERED);
            } else if (compressed == null || isNotModified(exchange)) {
                record(product, 304);
                exchange.sendResponseHeaders(304, -1);
            } else {
                record(product, 200);
                exchange.getResponseHeaders().add(
                    "Content-Type", "application/octet-stream");
                exchange.getResponseHeaders().add("Content-MD5", md5);
                exchange.sendResponseHeaders(200, Files.size(compressed));
                try (OutputStream out = exchange.getResponseBody()) {
                    Files.copy(compressed, out);
                }
            }
        } finally {
            exchange.close();
        }
    }

    private void record(String product, int status) {
        synchronized (requests) {
            requests.add(new Request(product, status));
        }
    }

    private boolean offers(String product) {
        for (String offered : products) {
            if (offered.equalsIgnoreCase(product)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the request's If-Modified-Since is not earlier than the
     * published date of the file served, so the file is not newer.
     */
    private boolean isNotModified(HttpExchange exchange) {
        String since = exchange.getRequestHeaders().getFirst(
            "If-Modified-Since");
        if (since == null) {
            return false;
        }
        try {
            ZonedDateTime date = ZonedDateTime.parse(
                since, DateTimeFormatter.RFC_1123_DATE_TIME);
            return date.toInstant().isBefore(
                published.atStartOfDay(ZoneOffset.UTC).toInstant()) == false;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * The value of the Product parameter of a query, its name matched
     * ignoring case, or null when there is none or it is empty.
     */
    private static String productOf(String query)
        throws UnsupportedEncodingException {
        if (query == null) {
            return null;
        }
        for (String parameter : query.split("&")) {
            int equals = parameter.indexOf('=');
            if (equals > 0 && parameter.substring(0, equals)
                .equalsIgnoreCase("Product")) {
                String value = URLDecoder.decode(
                    parameter.substring(equals + 1), "UTF-8");
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    /**
     * The URL requested with Product set to the product, as each URI the
     * Distributor lists is.
     */
    private String withProduct(String query, String product)
        throws UnsupportedEncodingException {
        StringBuilder uri = new StringBuilder(url).append('?');
        if (query != null) {
            for (String parameter : query.split("&")) {
                if (parameter.isEmpty() == false &&
                    parameter.regionMatches(true, 0, "Product=", 0, 8) ==
                        false) {
                    uri.append(parameter).append('&');
                }
            }
        }
        return uri.append("Product=")
            .append(URLEncoder.encode(product, "UTF-8")).toString();
    }

    private static void sendJson(HttpExchange exchange, int status,
                                 String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static void compress(Path source, Path target)
        throws IOException {
        try (OutputStream out = new GZIPOutputStream(
                Files.newOutputStream(target), BUFFER_SIZE) {
            {
                // The fastest compression, as the file is large and only
                // has to arrive intact.
                def.setLevel(Deflater.BEST_SPEED);
            }
        }) {
            Files.copy(source, out);
        }
    }

    private static String md5(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                .append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
