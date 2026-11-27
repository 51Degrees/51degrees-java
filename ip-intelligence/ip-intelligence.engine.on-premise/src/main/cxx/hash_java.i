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

/* SWIG passes each C++ pointer to Java as a jlong. Its own typemaps read and
   write the jlong through a pointer of another type, which breaks the strict
   aliasing rules and makes gcc warn when it optimizes. These typemaps copy
   the bytes with memcpy instead, which is defined behavior and compiles to
   the same move. The build replaces the base class conversions SWIG writes
   for each derived class with the same functions, as SWIG has no typemap for
   them. */
%fragment("fiftyonePointers", "header") %{
#include <cstring>
  template <typename P> static P fiftyoneFromJlong(jlong value) {
    P pointer = 0;
    memcpy(&pointer, &value, sizeof(pointer));
    return pointer;
  }
  template <typename P> static jlong fiftyoneToJlong(P pointer) {
    jlong value = 0;
    memcpy(&value, &pointer, sizeof(pointer));
    return value;
  }
%}
%typemap(in, fragment="fiftyonePointers") SWIGTYPE ($&1_type argp)
%{ argp = fiftyoneFromJlong< $&1_ltype >($input);
   if (!argp) {
     SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "Attempt to dereference null $1_type");
     return $null;
   }
   $1 = *argp; %}
%typemap(out, fragment="fiftyonePointers") SWIGTYPE
%{ $result = fiftyoneToJlong< $&1_ltype >(new $1_ltype($1)); %}
%typemap(in, fragment="fiftyonePointers") SWIGTYPE *, SWIGTYPE []
%{ $1 = fiftyoneFromJlong< $1_ltype >($input); %}
%typemap(in, fragment="fiftyonePointers") SWIGTYPE &
%{ $1 = fiftyoneFromJlong< $1_ltype >($input);
  if (!$1) {
    SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "$1_type is null");
    return $null;
  } %}
%typemap(out, fragment="fiftyonePointers") SWIGTYPE *, SWIGTYPE &, SWIGTYPE []
%{ $result = fiftyoneToJlong($1); %}
%typemap(in, fragment="fiftyonePointers") SWIGTYPE *const& ($*1_ltype temp = 0)
%{ temp = fiftyoneFromJlong< $*1_ltype >($input);
   $1 = ($1_ltype)&temp; %}
%typemap(out, fragment="fiftyonePointers") SWIGTYPE *const&
%{ $result = fiftyoneToJlong(*$1); %}

%include "cxx/src/common/JavaTypes.i"

/* Nothing extends the vector proxies, so they are final. The constructors
   SWIG's std_vector.i gives them pass this to native code before they
   return, which javac reports as a this-escape in a class that can be
   extended. The build removes the annotation std_vector.i puts on them to
   hide it, and only from final classes. */
%typemap(javaclassmodifiers) std::vector "public final class"

/* The Java side. Each proxy class documents the members SWIG adds to it, so
   javadoc checks the proxies as it checks the rest of the code. The class
   that declares the native methods and the module class are only used by
   the proxies, so they are not public. */
%pragma(java) jniclassclassmodifiers="final class"
%pragma(java) moduleclassmodifiers="final class"

/* A proxy that owns its C++ object deletes it when the garbage collector
   finalizes the proxy, unless JavaTypes.i says the class is closed instead.
   Object.finalize is not deprecated in the Java 8 API these classes are
   compiled against, so the method needs no annotation. */
%typemap(javafinalize) SWIGTYPE %{
  /**
   * Deletes the C++ object, if this proxy owns it, when the garbage
   * collector finalizes this proxy.
   */
  protected void finalize() {
    delete();
  }
%}

%typemap(javadestruct, methodname="delete", methodmodifiers="/**\n   * Deletes the C++ object if this proxy owns it. The proxy cannot be used\n   * after this.\n   */\n  public synchronized", parameters="") SWIGTYPE {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        $jnicall;
      }
      swigCPtr = 0;
    }
  }

%typemap(javadestruct_derived, methodname="delete", methodmodifiers="/**\n   * Deletes the C++ object if this proxy owns it. The proxy cannot be used\n   * after this.\n   */\n  public synchronized", parameters="") SWIGTYPE {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        $jnicall;
      }
      swigCPtr = 0;
    }
    super.delete();
  }

%typemap(javabody) SWIGTYPE %{
  private transient long swigCPtr;

  /** Whether deleting this proxy deletes its C++ object. */
  protected transient boolean swigCMemOwn;

  /**
   * Makes a proxy for a C++ object.
   *
   * @param cPtr the address of the C++ object
   * @param cMemoryOwn whether the proxy deletes the C++ object
   */
  protected $javaclassname(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  /**
   * The address of the C++ object of a proxy, or 0 for null.
   *
   * @param obj the proxy, or null
   * @return the address of its C++ object
   */
  protected static long getCPtr($javaclassname obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  /**
   * Takes the C++ object from a proxy that owns it, so the caller deletes
   * it and the proxy can no longer be used.
   *
   * @param obj the proxy, or null
   * @return the address of its C++ object, or 0 for null
   */
  protected static long swigRelease($javaclassname obj) {
    long ptr = 0;
    if (obj != null) {
      if (!obj.swigCMemOwn)
        throw new RuntimeException("Cannot release ownership as memory is not owned");
      ptr = obj.swigCPtr;
      obj.swigCMemOwn = false;
      obj.delete();
    }
    return ptr;
  }
%}

%typemap(javabody_derived) SWIGTYPE %{
  private transient long swigCPtr;

  /**
   * Makes a proxy for a C++ object.
   *
   * @param cPtr the address of the C++ object
   * @param cMemoryOwn whether the proxy deletes the C++ object
   */
  protected $javaclassname(long cPtr, boolean cMemoryOwn) {
    super($imclassname.$javaclazznameSWIGUpcast(cPtr), cMemoryOwn);
    swigCPtr = cPtr;
  }

  /**
   * The address of the C++ object of a proxy, or 0 for null.
   *
   * @param obj the proxy, or null
   * @return the address of its C++ object
   */
  protected static long getCPtr($javaclassname obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  /**
   * Takes the C++ object from a proxy that owns it, so the caller deletes
   * it and the proxy can no longer be used.
   *
   * @param obj the proxy, or null
   * @return the address of its C++ object, or 0 for null
   */
  protected static long swigRelease($javaclassname obj) {
    long ptr = 0;
    if (obj != null) {
      if (!obj.swigCMemOwn)
        throw new RuntimeException("Cannot release ownership as memory is not owned");
      ptr = obj.swigCPtr;
      obj.swigCMemOwn = false;
      obj.delete();
    }
    return ptr;
  }
%}

/* The methods that copy a UTF-8 value into the caller's buffer take a Java
   byte array, and the length is the array's own, so the C++ code never
   writes past its end. The C++ code writes into a copy of the array, which
   is copied back after it returns, so nothing is left to release if it
   throws. */
%typemap(jni) (unsigned char copy[], int length) "jbyteArray"
%typemap(jtype) (unsigned char copy[], int length) "byte[]"
%typemap(jstype) (unsigned char copy[], int length) "byte[]"
%typemap(javain) (unsigned char copy[], int length) "$javainput"
%typemap(in) (unsigned char copy[], int length) (std::vector<unsigned char> buffer) {
  if (!$input) {
    SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "The array to copy into is null");
    return $null;
  }
  $2 = (int)jenv->GetArrayLength($input);
  buffer.resize((size_t)$2);
  if ($2 > 0) {
    jenv->GetByteArrayRegion($input, 0, $2, reinterpret_cast<jbyte *>(buffer.data()));
  }
  $1 = buffer.data();
}
%typemap(argout) (unsigned char copy[], int length) {
  if ($2 > 0) {
    jenv->SetByteArrayRegion($input, 0, $2, reinterpret_cast<const jbyte *>(buffer$argnum.data()));
  }
}
%typemap(freearg) (unsigned char copy[], int length) ""

nofinalize(EvidenceIpi);
nofinalize(ResultsIpi);

%include "cxx/src/ipi/ipi.i"

/* Avoid copying the key and value character by character and pass a pointer instead. */
%apply (char *STRING, size_t LENGTH) { (const char propertyName[], size_t propertyNameLength) }
%apply (char *STRING, size_t LENGTH) { (const char key[], size_t keyLength) }
%apply (char *STRING, size_t LENGTH) { (const char value[], size_t valueLength) }
/* The Java side ends each byte array with a zero, which the string leaves
   out. The length stops the read at the end of the array if there is none. */
%{
  static std::string fiftyoneBytesToString(const char bytes[], size_t length) {
    size_t used = 0;
    while (used < length && bytes[used] != '\0') {
      used++;
    }
    return used == 0 ? std::string() : std::string(bytes, used);
  }
%}
%inline %{
  void Evidence_AddFromBytes(EvidenceBase *evidence, const char key[], size_t keyLength, const char value[], size_t valueLength) {
    (*evidence)[fiftyoneBytesToString(key, keyLength)] = fiftyoneBytesToString(value, valueLength);
  }

  Value<std::string> Results_GetValueAsString(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->getValueAsString(fiftyoneBytesToString(propertyName, propertyNameLength));
  }
  Value<std::vector<std::string>> Results_GetValues(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->getValues(fiftyoneBytesToString(propertyName, propertyNameLength));
  }
  Value<bool> Results_GetValueAsBool(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->getValueAsBool(fiftyoneBytesToString(propertyName, propertyNameLength));
  }
  Value<int> Results_GetValueAsInteger(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->getValueAsInteger(fiftyoneBytesToString(propertyName, propertyNameLength));
  }
  Value<double> Results_GetValueAsDouble(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->getValueAsDouble(fiftyoneBytesToString(propertyName, propertyNameLength));
  }
  bool Results_ContainsProperty(ResultsBase *results, const char propertyName[], size_t propertyNameLength) {
    return results->containsProperty(fiftyoneBytesToString(propertyName, propertyNameLength));
  }
%}


/* Load the native library automatically. */
%pragma(java) jniclassimports=%{
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.interop.Constants;
import fiftyone.pipeline.engines.fiftyone.flowelements.interop.LibLoader;
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
