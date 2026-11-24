# 3.24 or later, as cxx needs. From 3.15 CMake puts no warning level in the
# default MSVC flags, so the level set below is the only one.
cmake_minimum_required(VERSION 3.24)
set (CMAKE_C_STANDARD 11)
set (CMAKE_CXX_STANDARD 17)

# Include the C API
project(DeviceDetectionHashEngine VERSION 4.3.0 LANGUAGES CXX C)

# Every target here, the wrapper and the cxx libraries alike, warns at /W4
# with MSVC and at -Wall -Wextra with gcc and clang, and every warning is an
# error.
set(CMAKE_COMPILE_WARNING_AS_ERROR ON)
if (MSVC)
    add_compile_options(/W4)
else()
    add_compile_options(-Wall -Wextra -fPIC)
endif()

if (MSVC)
    if (CMAKE_GENERATOR_PLATFORM)
        set (GEN_PLATFORM ${CMAKE_GENERATOR_PLATFORM})
    else()
        set (GEN_PLATFORM ${CMAKE_VS_PLATFORM_NAME_DEFAULT})
    endif()

    if (${GEN_PLATFORM} MATCHES .*64.*)
        set(ARCH x64)
    else()
        set(ARCH x86)
    endif()

else()
    if (DEFINED ARCH)
        # Use the defined ARCH
    elseif (${CMAKE_SYSTEM_PROCESSOR} MATCHES ^armhf)
        set(ARCH armhf)
    elseif (${CMAKE_SYSTEM_PROCESSOR} MATCHES ^aarch64)
        set(ARCH aarch64)
    elseif (${CMAKE_SYSTEM_PROCESSOR} MATCHES ^arm64)
        set(ARCH aarch64)
    elseif (${CMAKE_SYSTEM_PROCESSOR} MATCHES .*64.*)
        if (_51DEGREES_32BIT)
            set (ARCH x86)
        else()
            set (ARCH x64)
        endif()
    else()
        set (ARCH x86)
    endif()
endif()
message(STATUS "Arch name: ${ARCH}")

if (MSVC)
    set (OS windows)
elseif (UNIX AND NOT APPLE)
    set (OS linux)
elseif (APPLE)
    set (OS mac)
endif()
message(STATUS "OS name: ${OS}")

# The C and C++ code is the 51degrees-cxx submodule at the root of this
# repository. Only Device Detection is built here, without its tests.
get_filename_component(FIFTYONE_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../.." ABSOLUTE)
set(_51DEGREES_BUILD_IP_INTELLIGENCE OFF CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
include(${FIFTYONE_ROOT}/cxx/CMakeLists.txt NO_POLICY_SCOPE)

set(SRCOUT ${CMAKE_CURRENT_LIST_DIR}/../java/fiftyone/devicedetection/hash/engine/onpremise)
set(RES ${CMAKE_CURRENT_LIST_DIR}/../../../target/classes)


option(RebuildSwig "RebuildSwig" OFF)

if (RebuildSwig)
    find_package(SWIG 4.0 COMPONENTS java)
    if (NOT SWIG_FOUND)
        message(WARNING "SWIG executable cannot be found so will not be run")
    elseif (NOT SWIG_java_FOUND)
        message(WARNING "SWIG does not include Java support so will not be run")
    else()
        message(STATUS "Rebuilding SWIG interfaces")
        # SWIG writes into the build folder, and the committed wrapper and Java
        # proxies are replaced only when it succeeds, as a failed run removes
        # its output files.
        set(SWIGOUT ${CMAKE_CURRENT_BINARY_DIR}/swig)
        file(REMOVE_RECURSE ${SWIGOUT})
        file(MAKE_DIRECTORY ${SWIGOUT}/java)
        execute_process(COMMAND ${SWIG_EXECUTABLE} -Wall -Werror -c++ -java -I${FIFTYONE_ROOT} -package fiftyone.devicedetection.hash.engine.onpremise.interop.swig -outdir ${SWIGOUT}/java -o ${SWIGOUT}/Java_Hash_Engine.cpp ${CMAKE_CURRENT_LIST_DIR}/hash_java.i COMMAND_ERROR_IS_FATAL ANY)
        file(GLOB OLD_PROXIES ${SRCOUT}/interop/swig/*.java)
        file(REMOVE ${OLD_PROXIES})
        file(GLOB NEW_PROXIES ${SWIGOUT}/java/*.java)
        file(COPY ${NEW_PROXIES} DESTINATION ${SRCOUT}/interop/swig)
        file(COPY_FILE ${SWIGOUT}/Java_Hash_Engine.cpp ${CMAKE_CURRENT_LIST_DIR}/Java_Hash_Engine.cpp)
    endif()
endif()
message(STATUS "Done SWIG interfaces")

find_package(Java)

# Only the includes are needed, so we set the libraries to none so they
# are ignored by FindJNI.
set(JAVA_AWT_LIBRARY None)
set(JAVA_JVM_LIBRARY None)
find_package(JNI REQUIRED)

add_library(fiftyone-hash-java SHARED
    ${CMAKE_CURRENT_LIST_DIR}/Java_Hash_Engine.cpp)

set_target_properties(fiftyone-hash-java
    PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY "${RES}"
    LIBRARY_OUTPUT_DIRECTORY "${RES}"
    RUNTIME_OUTPUT_DIRECTORY_RELEASE "${RES}"
    PREFIX ""
    OUTPUT_NAME "DeviceDetectionHashEngine-${OS}-${ARCH}"
)
target_link_libraries(fiftyone-hash-java fiftyone-hash-cxx)
if (NOT MSVC)
    target_compile_options(fiftyone-hash-java INTERFACE "-static-libgcc -static-libstdc++")
endif()

# The SWIG wrapper turns some MSVC warnings off unless these are defined.
target_compile_definitions(fiftyone-hash-java PRIVATE
    SWIG_MSC_UNSUPPRESS_4505
    SWIG_NO_CRT_SECURE_NO_DEPRECATE
    SWIG_NO_SCL_SECURE_NO_DEPRECATE)
if (MSVC)
	target_compile_options(fiftyone-hash-java PRIVATE "/W4" "/WX")
	target_link_options(fiftyone-hash-java PRIVATE "/WX")
else()
	target_compile_options(fiftyone-hash-java PRIVATE ${COMPILE_OPTION_DEBUG} "-Wall" "-Wextra" "-Werror")
	# The wrapper SWIG makes for Java passes C++ pointers to Java as jlong
	# through pointer casts. Section 27.2.4 of the SWIG Java documentation
	# says to compile it with -fno-strict-aliasing when gcc optimises, as
	# those casts are not safe under strict aliasing.
	target_compile_options(fiftyone-hash-java PRIVATE "-fno-strict-aliasing")
endif()

# The wrapper includes through cxx/, so the folder that holds it is on the path.
target_include_directories(fiftyone-hash-java PRIVATE ${FIFTYONE_ROOT})
target_include_directories(fiftyone-hash-java PUBLIC ${JAVA_INCLUDE_PATH})
if(WIN32)
    target_include_directories(fiftyone-hash-java PUBLIC ${JAVA_INCLUDE_PATH}/win32)
endif()
if(APPLE)
    target_include_directories(fiftyone-hash-java PUBLIC ${JAVA_INCLUDE_PATH}/darwin)
endif()
if(UNIX AND NOT APPLE)
    target_include_directories(fiftyone-hash-java PUBLIC ${JAVA_INCLUDE_PATH}/linux)
endif()
