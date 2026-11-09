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

package fiftyone.pipeline.web;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.slf4j.LoggerFactory;

import fiftyone.pipeline.core.data.DataKey;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.Evidence;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterAggregator;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import fiftyone.pipeline.web.services.ClientsidePropertyServiceCore;
import fiftyone.pipeline.web.services.FlowDataProviderCore;
import fiftyone.pipeline.web.services.WebRequestEvidenceServiceCore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ClientsidePropertyServiceTests {
  
  private ClientsidePropertyServiceCore service;
  private FlowDataProviderCore flowDataProvider;
  private Pipeline pipeline;
  private FlowData flowData;
  private DataKey dataKey;
  private Map<String, Object> evidence;
  private JavaScriptBuilderElement jsElement;
  private JsonBuilderElement jsonElement;
  private JavaScriptBuilderData jsData;
  private JsonBuilderData jsonData;

  private HttpServletRequest request;
  private HttpServletResponse response;
  private StringWriter responseString;

  private static final String JS_CONTENT = "JAVASCRIPT CONTENT";
  private static final String JSON_CONTENT = "JSON CONTENT";
  private static final String JS_CONTENT_TYPE = "application/x-javascript";
  private static final String JSON_CONTENT_TYPE = "application/json";

  private static final String USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36";

  /**
   * A weak entity tag holding a quoted SHA-256 digest in lower case hex.
   */
  private static final String WEAK_ETAG = "W/\"[0-9a-f]{64}\"";

  /**
   * The SHA-256 digest of the keys and values below, sorted by key, each
   * written as its length in four bytes, big endian, then its UTF-8 bytes,
   * worked out apart from the Java code.
   * header.sec-ch-ua-mobile = ?0
   * header.user-agent = USER_AGENT
   */
  private static final String ETAG_UA_AND_MOBILE =
    "W/\"9beee8189f1e05e5f821379597c3a690e612c465dc53d67de2e42ebffbd4dbd3\"";

  /**
   * As above, for header.user-agent = USER_AGENT alone.
   */
  private static final String ETAG_UA =
    "W/\"4eee715bd966ea5198b7d696aea05fd627dfd2df7f8be928e95797155ec2cf00\"";

	@Before
  public void init() throws IOException {
    request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("GET");
    response = mock(HttpServletResponse.class);
    responseString = new StringWriter();
    PrintWriter responseWriter = new PrintWriter(responseString);
    when(response.getWriter()).thenReturn(responseWriter);

    jsElement = mock(JavaScriptBuilderElement.class);
    jsonElement = mock(JsonBuilderElement.class);

    flowData = mock(FlowData.class);
    dataKey = mock(DataKey.class);
    when(flowData.generateKey(any(EvidenceKeyFilter.class))).thenReturn(dataKey);
    evidence = new HashMap<>();
    Evidence flowEvidence = mock(Evidence.class);
    when(flowEvidence.asKeyMap()).thenReturn(evidence);
    when(flowData.getEvidence()).thenReturn(flowEvidence);
    jsData = mock(JavaScriptBuilderData.class);
    when(jsData.getJavaScript()).thenReturn(JS_CONTENT);
    when(flowData.getFromElement(jsElement)).thenReturn(jsData);
    jsonData = mock(JsonBuilderData.class);
    when(jsonData.getJson()).thenReturn(JSON_CONTENT);
    when(flowData.getFromElement(jsonElement)).thenReturn(jsonData);
    
    flowDataProvider = mock(FlowDataProviderCore.class);
    when(flowDataProvider.getFlowData(any(HttpServletRequest.class))).thenReturn(flowData);

  }

  
  /**
   * @throws IOException
   */   
  @Test
  public void JavaScript_VaryHeader_UA() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element =  Arrays.asList(
      "header.User-Agent");
    elements.add(element);

    JavaScript_VaryHeader(elements, "User-Agent");
  }

  @Test
  public void JavaScript_VaryHeader_UA_AND_SEC() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.Sec-ch-ua",
      "header.User-Agent");
    elements.add(element);

    JavaScript_VaryHeader(elements, "User-Agent,Sec-ch-ua");
  }

  @Test
  public void JavaScript_VaryHeader_PSEUDO() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.sec-ch-uasec-ch-ua-full-version");
    elements.add(element);

    JavaScript_VaryHeader(elements, "");
  }

  @Test
  public void JavaScript_VaryHeader_UA_AND_PSEUDO() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.User-Agent",
      "header.sec-ch-uasec-ch-ua-full-version");
    elements.add(element);

    JavaScript_VaryHeader(elements, "User-Agent");
  }

  @Test
  public void JavaScript_VaryHeader_2Element_UA() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.User-Agent");
    List<String> element2 = Arrays.asList(
      "header.User-Agent");
    elements.add(element);
    elements.add(element2);

    JavaScript_VaryHeader(elements, "User-Agent");
  }

  @Test
  public void JavaScript_VaryHeader_2Element_UA_AND_PSEUDO() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.User-Agent");
    List<String> element2 = Arrays.asList(
      "header.sec-ch-uasec-ch-ua-full-version");
    elements.add(element);
    elements.add(element2);

    JavaScript_VaryHeader(elements, "User-Agent");
  }

  @Test
  public void JavaScript_VaryHeader_2Element_UA_AND_SEC() throws IOException {
    List<List<String>> elements = new ArrayList<List<String>>();
    List<String> element = Arrays.asList(
      "header.User-Agent");
    List<String> element2 = Arrays.asList(      
      "header.Sec-ch-ua");
    elements.add(element);
    elements.add(element2);

    JavaScript_VaryHeader(elements, "User-Agent,Sec-ch-ua");
  }

  /**
   * The headers of the elements in a parallel group are in the Pipeline's
   * Evidence key filter, so they are in the Vary header too.
   */
  @Test
  @SuppressWarnings("rawtypes")
  public void JavaScript_VaryHeader_ParallelElements() throws IOException {
    EvidenceKeyFilterAggregator group = new EvidenceKeyFilterAggregator();
    group.addFilter(new EvidenceKeyFilterWhitelist(
      Arrays.asList("header.User-Agent"),
      String.CASE_INSENSITIVE_ORDER));
    FlowElement element = mock(FlowElement.class);
    when(element.getEvidenceKeyFilter()).thenReturn(group);
    List<FlowElement> elements = new ArrayList<>();
    elements.add(element);
    Configure(elements);

    service.serveJavascript(request, response);

    verify(response).setHeader(eq("Vary"), eq("User-Agent"));
  }

  /**
   * The ETag is a weak entity tag, because the content can change for the
   * same Evidence, for example when a data file is updated, and RFC 9110
   * section 8.8.3 asks for such a validator to be marked weak. Its opaque
   * part is a quoted hash.
   */
  @Test
  public void JavaScript_ETag_IsWeakEntityTag() throws IOException {
    Configure(null);

    service.serveJavascript(request, response);

    verify(response).setHeader(eq("ETag"), matches(WEAK_ETAG));
  }

  @Test
  public void Json_ETag_IsWeakEntityTag() throws IOException {
    Configure(null);

    service.serveJson(request, response);

    verify(response).setHeader(eq("ETag"), matches(WEAK_ETAG));
  }

  /**
   * The same Evidence gives the same ETag in every process, so the ETag is
   * a digest with no per-process seed, compared here with a value worked
   * out apart from the Java code. Evidence outside the Pipeline's Evidence
   * key filter, and the case of a key, leave it unchanged.
   */
  @Test
  public void JavaScript_ETag_SameInEveryProcess() throws IOException {
    evidence.put("header.User-Agent", USER_AGENT);
    evidence.put("header.sec-ch-ua-mobile", "?0");
    evidence.put("header.X-Not-Accepted", "anything");
    Configure(null);
    when(pipeline.getEvidenceKeyFilter()).thenReturn(
      new EvidenceKeyFilterWhitelist(
        Arrays.asList("header.user-agent", "header.sec-ch-ua-mobile"),
        String.CASE_INSENSITIVE_ORDER));

    service.serveJavascript(request, response);

    verify(response).setHeader(eq("ETag"), eq(ETAG_UA_AND_MOBILE));
  }

  /**
   * Two Pipelines built the same way, as in two processes, give the same
   * ETag for the same request.
   */
  @Test
  public void Json_ETag_SameAcrossPipelines() throws Exception {
    try (Pipeline first = webPipeline();
         Pipeline second = webPipeline();
         FlowData firstData = first.createFlowData();
         FlowData secondData = second.createFlowData()) {
      String firstETag =
        servedETag(first, firstData, webRequest(USER_AGENT));
      String secondETag =
        servedETag(second, secondData, webRequest(USER_AGENT));

      assertEquals("Two Pipelines gave different ETags.",
        firstETag, secondETag);
      assertEquals(ETAG_UA, firstETag);
    }
  }

  /**
   * The Sequence Element adds a new session id to every request that has
   * none, so the ETag is made from the Evidence as the request supplied it,
   * and two requests with the same Evidence get the same ETag. A different
   * User-Agent gives a different one.
   */
  @Test
  public void Json_ETag_UnchangedWhenSequenceElementAddsSessionId()
    throws Exception {
    try (Pipeline webPipeline = webPipeline();
         FlowData firstData = webPipeline.createFlowData();
         FlowData secondData = webPipeline.createFlowData();
         FlowData otherData = webPipeline.createFlowData()) {
      String firstETag =
        servedETag(webPipeline, firstData, webRequest(USER_AGENT));
      String secondETag =
        servedETag(webPipeline, secondData, webRequest(USER_AGENT));
      String otherETag =
        servedETag(webPipeline, otherData, webRequest("Other"));

      Object firstSession =
        firstData.getEvidence().asKeyMap().get("query.session-id");
      Object secondSession =
        secondData.getEvidence().asKeyMap().get("query.session-id");
      assertNotNull("The Sequence Element added no session id.",
        firstSession);
      assertNotEquals("The Sequence Element added the same session id.",
        firstSession, secondSession);
      assertEquals("The ETag changed with the session id.",
        firstETag, secondETag);
      assertNotEquals("The ETag did not change with the User-Agent.",
        firstETag, otherETag);
    }
  }

  /**
   * A response built while processing recorded errors may lack the data of
   * the element that failed, so it is not kept and carries no ETag to
   * revalidate with.
   */
  @Test
  public void JavaScript_ProcessingErrors_NoCacheAndNoETag()
    throws IOException {
    recordProcessingError();
    Configure(null);

    service.serveJavascript(request, response);

    ValidateFailedResponse(JS_CONTENT, JS_CONTENT_TYPE);
  }

  @Test
  public void Json_ProcessingErrors_NoCacheAndNoETag() throws IOException {
    recordProcessingError();
    Configure(null);

    service.serveJson(request, response);

    ValidateFailedResponse(JSON_CONTENT, JSON_CONTENT_TYPE);
  }

  /**
   * A browser holding a good response keeps it while processing fails. The
   * 304 carries the caching headers a 200 to the same request would, so the
   * browser asks again next time rather than keeping it for 30 minutes.
   */
  @Test
  public void JavaScript_ProcessingErrors_MatchingIfNoneMatch_304()
    throws IOException {
    Configure(ConfigureElements(
      Collections.singletonList(Arrays.asList("header.User-Agent"))));
    String etag = etagSent();
    recordProcessingError();
    ifNoneMatch(etag);

    service.serveJavascript(request, response);

    verify(response).setStatus(eq(304));
    assertEquals("", responseString.toString());
    verify(response).setHeader(eq("Cache-Control"), eq("private, no-cache"));
    verify(response, times(0)).setHeader(eq("ETag"), anyString());
    verify(response).setHeader(eq("Vary"), eq("User-Agent"));
    verify(response).setHeader(eq("Access-Control-Allow-Origin"), eq("*"));
  }

  /**
   * A matching If-None-Match gets the same status while processing fails as
   * at any other time, so a POST gets 412.
   */
  @Test
  public void Json_ProcessingErrors_MatchingIfNoneMatch_Post_412()
    throws IOException {
    Configure(null);
    String etag = etagSent();
    recordProcessingError();
    when(request.getMethod()).thenReturn("POST");
    ifNoneMatch(etag);

    service.serveJson(request, response);

    verify(response).setStatus(eq(412));
    verify(response, times(0)).setHeader(eq("ETag"), anyString());
    assertEquals("", responseString.toString());
  }

  /**
   * A 304 carries the Cache-Control, Vary and ETag headers a 200 to the same
   * request would, as RFC 9110 section 15.4.5 requires, and the CORS header.
   */
  @Test
  public void JavaScript_MatchingIfNoneMatch_304_CarriesHeaders()
    throws IOException {
    Configure(ConfigureElements(
      Collections.singletonList(Arrays.asList("header.User-Agent"))));
    String etag = etagSent();
    ifNoneMatch(etag);

    service.serveJavascript(request, response);

    Validate304(etag);
  }

  @Test
  public void Json_MatchingIfNoneMatch_Get_304_CarriesHeaders()
    throws IOException {
    Configure(ConfigureElements(
      Collections.singletonList(Arrays.asList("header.User-Agent"))));
    String etag = etagSent();
    ifNoneMatch(etag);

    service.serveJson(request, response);

    Validate304(etag);
  }

  @Test
  public void JavaScript_MatchingIfNoneMatch_Head_304() throws IOException {
    Configure(null);
    String etag = etagSent();
    when(request.getMethod()).thenReturn("HEAD");
    ifNoneMatch(etag);

    service.serveJavascript(request, response);

    verify(response).setStatus(eq(304));
    assertEquals("", responseString.toString());
  }

  /**
   * RFC 9110 section 13.1.2 answers a matching If-None-Match with 412 for
   * any method other than GET or HEAD, such as the POST the client-side
   * script sends to the JSON endpoint.
   */
  @Test
  public void Json_MatchingIfNoneMatch_Post_412() throws IOException {
    Configure(null);
    String etag = etagSent();
    when(request.getMethod()).thenReturn("POST");
    ifNoneMatch(etag);

    service.serveJson(request, response);

    verify(response).setStatus(eq(412));
    verify(response, times(0)).setStatus(eq(304));
    verify(response).setHeader(eq("Access-Control-Allow-Origin"), eq("*"));
    assertEquals("", responseString.toString());
  }

  @Test
  public void JavaScript_MatchingIfNoneMatch_Post_412() throws IOException {
    Configure(null);
    String etag = etagSent();
    when(request.getMethod()).thenReturn("POST");
    ifNoneMatch(etag);

    service.serveJavascript(request, response);

    verify(response).setStatus(eq(412));
    assertEquals("", responseString.toString());
  }

  /**
   * Without a matching If-None-Match a POST gets the JSON as before.
   */
  @Test
  public void Json_Post_200() throws IOException {
    Configure(null);
    when(request.getMethod()).thenReturn("POST");

    service.serveJson(request, response);

    verify(response).setStatus(eq(200));
    assertEquals(JSON_CONTENT, responseString.toString());
  }

  /**
   * "*" matches, as the endpoint always has a representation. RFC 9110
   * section 13.1.2 step 1.
   */
  @Test
  public void IfNoneMatch_Star_304() throws IOException {
    Configure(null);
    validateIfNoneMatch(304, " * ");
  }

  /**
   * A list matches when one of its tags matches. Step 2.
   */
  @Test
  public void IfNoneMatch_ListHoldingTheTag_304() throws IOException {
    Configure(null);
    validateIfNoneMatch(304, "W/\"0\", " + etagSent() + ", \"1\"");
  }

  /**
   * The comparison is weak, so the tag matches without W/ as well.
   * RFC 9110 section 8.8.3.2.
   */
  @Test
  public void IfNoneMatch_TagWithoutWeakPrefix_304() throws IOException {
    Configure(null);
    validateIfNoneMatch(304, etagSent().substring(2));
  }

  /**
   * List elements may be separated by tabs as well as spaces, and empty
   * elements are ignored. RFC 9110 sections 5.6.1.2 and 5.6.3.
   */
  @Test
  public void IfNoneMatch_TabBetweenElements_304() throws IOException {
    Configure(null);
    validateIfNoneMatch(304, ",W/\"0\",\t" + etagSent() + "\t, ,");
  }

  /**
   * Several field lines are one list. RFC 9110 section 5.2.
   */
  @Test
  public void IfNoneMatch_SeveralFieldLines_304() throws IOException {
    Configure(null);
    validateIfNoneMatch(304, "W/\"0\"", etagSent());
  }

  @Test
  public void IfNoneMatch_OtherTag_200() throws IOException {
    Configure(null);
    validateIfNoneMatch(200, "W/\"0\"");
  }

  /**
   * A field that is neither "*" nor a list of entity tags does not match,
   * even when it holds the tag. Step 3.
   */
  @Test
  public void IfNoneMatch_TagThenNonTag_200() throws IOException {
    Configure(null);
    validateIfNoneMatch(200, etagSent() + ", other");
  }

  @Test
  public void IfNoneMatch_TwoTagsWithNoComma_200() throws IOException {
    Configure(null);
    validateIfNoneMatch(200, "W/\"0\" " + etagSent());
  }

  /**
   * A space is not allowed inside an entity tag's quotes. RFC 9110 section
   * 8.8.3.
   */
  @Test
  public void IfNoneMatch_SpaceInsideQuotes_200() throws IOException {
    Configure(null);
    validateIfNoneMatch(200, "W/\"0 1\", " + etagSent());
  }

  /**
   * "*" in a list with other values is not allowed. RFC 9110 section
   * 13.1.2.
   */
  @Test
  public void IfNoneMatch_StarAndTag_200() throws IOException {
    Configure(null);
    validateIfNoneMatch(200, "*", etagSent());
  }

  /**
   * The weak prefix is case sensitive. RFC 9110 section 8.8.3.
   */
  @Test
  public void IfNoneMatch_LowerCaseWeakPrefix_200() throws IOException {
    Configure(null);
    validateIfNoneMatch(200, "w" + etagSent().substring(1));
  }

  /**
   * An opaque tag is quoted. RFC 9110 section 8.8.3.
   */
  @Test
  public void IfNoneMatch_Unquoted_200() throws IOException {
    Configure(null);
    String etag = etagSent();
    validateIfNoneMatch(200, etag.substring(3, etag.length() - 1));
  }

  /**
   * The Content-Length header is the length of the content in bytes, in
   * the character encoding the response is written with.
   */
  @Test
  public void Json_ContentLength_InBytes() throws IOException {
    String json = "{\"city\":\"Zürich\"}";
    when(jsonData.getJson()).thenReturn(json);
    when(response.getCharacterEncoding()).thenReturn("UTF-8");
    Configure(null);

    service.serveJson(request, response);

    verify(response).setContentLength(eq(json.length() + 1));
  }

  /**
   * When the builder element is the one that failed there is no content,
   * and the response is still sent rather than throwing.
   */
  @Test
  public void JavaScript_ProcessingErrors_NoContent() throws IOException {
    recordProcessingError();
    when(flowData.getFromElement(jsElement)).thenReturn(null);
    Configure(null);

    service.serveJavascript(request, response);

    ValidateFailedResponse("", JS_CONTENT_TYPE);
  }

  /**
   * The ETag the JavaScript endpoint sends for the request as it stands,
   * from a response of its own.
   */
  private String etagSent() throws IOException {
    HttpServletResponse sent = mock(HttpServletResponse.class);
    when(sent.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    service.serveJavascript(request, sent);
    ArgumentCaptor<String> etag = ArgumentCaptor.forClass(String.class);
    verify(sent).setHeader(eq("ETag"), etag.capture());
    return etag.getValue();
  }

  /**
   * Give the request one If-None-Match field line for each value. getHeader
   * gives the first line, as a servlet container does.
   */
  private void ifNoneMatch(String... lines) {
    when(request.getHeaders("If-None-Match")).thenAnswer(
      invocation -> Collections.enumeration(Arrays.asList(lines)));
    when(request.getHeader("If-None-Match")).thenReturn(lines[0]);
  }

  private void validateIfNoneMatch(int expectedStatus, String... lines)
    throws IOException {
    ifNoneMatch(lines);

    service.serveJavascript(request, response);

    verify(response).setStatus(eq(expectedStatus));
    assertEquals(
      expectedStatus == 304 ? "" : JS_CONTENT,
      responseString.toString());
  }

  private void Validate304(String etag) {
    verify(response).setStatus(eq(304));
    verify(response, times(0)).setStatus(eq(200));
    assertEquals("", responseString.toString());
    verify(response).setHeader(
      eq("Cache-Control"), eq("private,max-age=1800"));
    verify(response).setHeader(eq("ETag"), eq(etag));
    verify(response).setHeader(eq("Vary"), eq("User-Agent"));
    verify(response).setHeader(eq("Access-Control-Allow-Origin"), eq("*"));
    verify(response, times(0)).setContentLength(anyInt());
    verify(response, times(0)).setContentType(anyString());
  }

  /**
   * A Pipeline with the Sequence Element, which adds a new session id to a
   * request that has none, an element that accepts the User-Agent header,
   * and the JSON builder.
   */
  private static Pipeline webPipeline() throws Exception {
    return new PipelineBuilder(LoggerFactory.getILoggerFactory())
      .addFlowElement(new SequenceElementBuilder(
        LoggerFactory.getILoggerFactory()).build())
      .addFlowElement(new UserAgentElement())
      .addFlowElement(new JsonBuilderElementBuilder(
        LoggerFactory.getILoggerFactory()).build())
      .setAutoCloseElements(true)
      .build();
  }

  /**
   * A GET request with a User-Agent header and no session id.
   */
  private static HttpServletRequest webRequest(String userAgent) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/51dpipeline/json");
    when(request.getHeaderNames()).thenReturn(
      Collections.enumeration(Arrays.asList("User-Agent", "X-Test")));
    when(request.getHeader("User-Agent")).thenReturn(userAgent);
    when(request.getHeader("X-Test")).thenReturn("not accepted");
    when(request.getRemoteAddr()).thenReturn("1.2.3.4");
    when(request.getScheme()).thenReturn("https");
    return request;
  }

  /**
   * Add the request's Evidence to the FlowData as the web integration does,
   * process it, and return the ETag the JSON endpoint sends.
   */
  private static String servedETag(
    Pipeline webPipeline,
    FlowData data,
    HttpServletRequest request) throws IOException {
    new WebRequestEvidenceServiceCore.Default()
      .addEvidenceFromRequest(data, request);
    data.process();
    assertTrue("Processing recorded errors: " + data.getErrors(),
      data.getErrors().isEmpty());

    FlowDataProviderCore provider = mock(FlowDataProviderCore.class);
    when(provider.getFlowData(request)).thenReturn(data);
    HttpServletResponse served = mock(HttpServletResponse.class);
    when(served.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    new ClientsidePropertyServiceCore.Default(provider, webPipeline)
      .serveJson(request, served);
    ArgumentCaptor<String> etag = ArgumentCaptor.forClass(String.class);
    verify(served).setHeader(eq("ETag"), etag.capture());
    return etag.getValue();
  }

  /**
   * An element that accepts the User-Agent header and adds nothing, in
   * place of an engine.
   */
  private static class UserAgentElement
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    private final EvidenceKeyFilter filter = new EvidenceKeyFilterWhitelist(
      Collections.singletonList("header.user-agent"),
      String.CASE_INSENSITIVE_ORDER);

    UserAgentElement() {
      super(LoggerFactory.getLogger(UserAgentElement.class), null);
    }

    @Override
    protected void processInternal(FlowData data) {
    }

    @Override
    public String getElementDataKey() {
      return "user-agent-test";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
      return filter;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
      return new ArrayList<>();
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
  }

  private void recordProcessingError() {
    when(flowData.getErrors()).thenReturn(
      Collections.singletonList(mock(FlowError.class)));
  }

  private void ValidateFailedResponse(
    String expectedContent,
    String contentType) {
    assertEquals(expectedContent, responseString.toString());
    verify(response).setStatus(eq(200));
    verify(response).setContentType(eq(contentType));
    verify(response).setContentLength(eq(expectedContent.length()));
    verify(response).setHeader(eq("Cache-Control"), eq("private, no-cache"));
    verify(response, times(0)).setHeader(eq("ETag"), anyString());
    verify(response).setHeader(eq("Access-Control-Allow-Origin"), eq("*"));
  }

  private void JavaScript_VaryHeader(
    List<List<String>> elements,
    String expectedVary) throws IOException {

    Configure(ConfigureElements(elements));
    service.serveJavascript(request, response);

    ValidateResponse(
      JS_CONTENT,
      JS_CONTENT.length(),
      JS_CONTENT_TYPE,
      200,
      String.valueOf(dataKey.hashCode()),
      expectedVary);
  }

  private void ValidateResponse(
    String expectedContent,
    Integer contentLength, 
    String contentType,
    int expectedStatusCode,
    String expectedETag,
    String expectedVary)
  {
    if (expectedContent == null) { expectedContent = ""; }
    assertEquals(expectedContent, responseString.toString());
    
    verify(response).setStatus(eq(expectedStatusCode));

    if (contentType != null) {
      verify(response).setContentType(eq(contentType));
    }
    else {
      verify(response, times(0)).setContentType(anyString());
    }

    if (contentLength != null) {
      verify(response).setContentLength(eq(contentLength));
    }
    else {
      verify(response, times(0)).setContentLength(anyInt());
    }
    
    if (expectedStatusCode == 200) {
      verify(response).setHeader(eq("Cache-Control"), eq("private,max-age=1800"));
    }
    else{
      verify(response, times(0)).setHeader(eq("Cache-Control"), anyString());
    }

    if (expectedVary != null) {
      String[] values = expectedVary.split(",");
      for (String value : values) {
        verify(response).setHeader(eq("Vary"), argThat(new CommaCountMatcher(values.length - 1)));
        verify(response).setHeader(eq("Vary"), contains(value));     
      }
    }
    else {
      verify(response, times(0)).setHeader(eq("Vary"), anyString());
    }
    
    if (expectedETag != null) {
      verify(response).setHeader(eq("ETag"), eq(expectedETag));
    }
    else {
      verify(response, times(0)).setHeader(eq("ETag"), anyString());
    }

    verify(response).setHeader(eq("Access-Control-Allow-Origin"), eq("*"));
  }

  @SuppressWarnings("rawtypes")
  private List<FlowElement> ConfigureElements(List<List<String>> evidenceHeaders) {
    List<FlowElement> elements = new ArrayList<FlowElement>();

    for (List<String> entry : evidenceHeaders) {
      if(entry != null) {
        FlowElement element = mock(FlowElement.class);
        EvidenceKeyFilter filter = new EvidenceKeyFilterWhitelist(entry);
        when(element.getEvidenceKeyFilter()).thenReturn(filter);

        elements.add(element);
      }
    }

    return elements;
  }

  @SuppressWarnings("rawtypes")
  private void Configure(List<FlowElement> elements) {
    if(elements == null){
      elements = new ArrayList<>();
    }
    
    pipeline = mock(Pipeline.class);
    when(pipeline.getFlowElements()).thenReturn(elements);
    when(pipeline.getElement(JavaScriptBuilderElement.class)).thenReturn(jsElement);
    when(pipeline.getElement(JsonBuilderElement.class)).thenReturn(jsonElement);
    when(pipeline.getEvidenceKeyFilter())
      .thenReturn(new EvidenceKeyFilterWhitelist(new ArrayList<String>()));

    when(flowData.getPipeline()).thenReturn(pipeline);

    service = new ClientsidePropertyServiceCore.Default(
      flowDataProvider, pipeline);
  }

  public class CommaCountMatcher implements ArgumentMatcher<String> {

    private int expectedCommaCount;

    public CommaCountMatcher(int expectedCommaCount) {
      this.expectedCommaCount = expectedCommaCount;
    }

    @Override
    public boolean matches(String actualValue) {
      int count = 0;
      for (char chr : actualValue.toCharArray()) {
        if(chr == ',') count++;
      };
      return count == expectedCommaCount;
    }
}
}
