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

package fiftyone.ipintelligence.examples.web;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * Servlet filter that overrides {@link HttpServletRequest#getRemoteAddr()} to
 * return the real client IP when running behind a reverse proxy (e.g. ngrok,
 * Cloudflare Tunnel, nginx).
 * <p>
 * This filter must be declared <b>before</b> the 51Degrees PipelineFilter in
 * web.xml so that the Pipeline sees the correct IP in {@code server.client-ip}
 * evidence.
 * <p>
 * The filter checks the following headers in priority order:
 * <ol>
 *     <li>{@code X-Forwarded-For} (standard proxy header)</li>
 *     <li>{@code X-Real-IP} (nginx convention)</li>
 * </ol>
 */
public class RealClientIpFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String realIp = getRealClientIp(httpRequest);
            if (realIp != null) {
                chain.doFilter(new RemoteAddrOverrideWrapper(httpRequest, realIp), response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private String getRealClientIp(HttpServletRequest request) {
        // X-Forwarded-For may contain: client, proxy1, proxy2
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        // Fallback to X-Real-IP (nginx)
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp.trim();
        }
        return null;
    }

    /**
     * Wrapper that overrides getRemoteAddr() to return the real client IP.
     */
    private static class RemoteAddrOverrideWrapper extends HttpServletRequestWrapper {
        private final String realIp;

        RemoteAddrOverrideWrapper(HttpServletRequest request, String realIp) {
            super(request);
            this.realIp = realIp;
        }

        @Override
        public String getRemoteAddr() {
            return realIp;
        }
    }
}
