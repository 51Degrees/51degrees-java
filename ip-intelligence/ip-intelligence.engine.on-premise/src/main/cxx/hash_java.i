/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2019 51 Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY.
 *
 * This Original Work is licensed under the European Union Public Licence (EUPL) 
 * v.1.2 and is subject to its terms as set out below.
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


%include "./ip-intelligence-cxx/src/common-cxx/JavaTypes.i"

nofinalize(EvidenceIpi);
nofinalize(ResultsIpi);

%include "./ip-intelligence-cxx/src/ipi.i"

/* Avoid copying the key and value character by character and pass a pointer instead. */
%apply (char *STRING, size_t LENGTH) { (const char propertyName[], size_t propertyNameLength) }
%apply (char *STRING, size_t LENGTH) { (const char key[], size_t keyLength) }
%apply (char *STRING, size_t LENGTH) { (const char value[], size_t valueLength) }
%inline %{
  void Evidence_AddFromBytes(EvidenceBase *evidence, const char key[], size_t keyLength, const char value[], size_t valueLength) {
    (*evidence)[key] = value;
  }

  Value<std::string> Results_GetValueAsString(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->getValueAsString(propertyName);
  }
  Value<std::vector<std::string>> Results_GetValues(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->getValues(propertyName);
  }
  Value<bool> Results_GetValueAsBool(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->getValueAsBool(propertyName);
  }
  Value<int> Results_GetValueAsInteger(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->getValueAsInteger(propertyName);
  }
  Value<double> Results_GetValueAsDouble(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->getValueAsDouble(propertyName);
  }
  bool Results_ContainsProperty(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->containsProperty(propertyName);
  }
%}


/* Load the native library automatically. */
%pragma(java) jniclassimports=%{
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.interop.Constants;
import fiftyone.pipeline.engines.fiftyone.flowelements.interop.LibLoader;
import java.nio.ByteBuffer;
%}
%pragma(java) jniclasscode=%{
  static {
    try {
      LibLoader.load(IPIntelligenceOnPremiseEngine.class);
    } catch (UnsatisfiedLinkError e) {
      if (e.getMessage().contains("libatomic")) {
        throw new UnsatisfiedLinkError(
            Constants.UNSATISFIED_LINK_LIBATOMIC_MESSAGE +
                " Inner error: " +
                e.getMessage());
      }
      else {
        throw new UnsatisfiedLinkError(Constants.UNSATISFIED_LINK_MESSAGE +
            " Inner error: " +
            e.getMessage());
      }
    } catch (Exception e) {
      throw new RuntimeException(Constants.UNSATISFIED_LINK_MESSAGE, e);
    }
  }
%}
