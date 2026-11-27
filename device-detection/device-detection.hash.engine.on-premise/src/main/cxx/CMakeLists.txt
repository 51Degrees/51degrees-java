# 3.24 or later, as cxx needs. From 3.15 CMake puts no warning level in the
# default MSVC flags, so the level set below is the only one.
cmake_minimum_required(VERSION 3.24)
set (CMAKE_C_STANDARD 11)
set (CMAKE_CXX_STANDARD 17)

# Include the C API
project(DeviceDetectionHashEngine VERSION 4.3.0 LANGUAGES CXX C)

# Every target here, the wrapper and the cxx libraries alike, warns at /W4
# with MSVC and at -Wall -Wextra with gcc and clang. Warnings are errors only
# in the builds 51Degrees runs, which set the environment variable
# _51DEGREES_WARNINGS_AS_ERRORS to true, in any case. A build anywhere else
# shows its warnings and does not stop on one. The option can also be given
# with -D, which takes the place of the variable.
string(TOLOWER "$ENV{_51DEGREES_WARNINGS_AS_ERRORS}" FIFTYONE_WARNINGS_AS_ERRORS_ENV)
string(COMPARE EQUAL "${FIFTYONE_WARNINGS_AS_ERRORS_ENV}" "true" FIFTYONE_WARNINGS_AS_ERRORS_DEFAULT)
option(_51DEGREES_WARNINGS_AS_ERRORS
    "Make every compiler and linker warning an error"
    ${FIFTYONE_WARNINGS_AS_ERRORS_DEFAULT})

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
        message(FATAL_ERROR "RebuildSwig is on, and SWIG cannot be found")
    elseif (NOT SWIG_java_FOUND)
        message(FATAL_ERROR "RebuildSwig is on, and SWIG has no Java support")
    else()
        message(STATUS "Rebuilding SWIG interfaces")
        # SWIG writes into the build folder, and the committed wrapper and Java
        # proxies are replaced only when it succeeds, as a failed run removes
        # its output files. It runs from this folder and finds cxx by a
        # relative path, so the wrapper names no folder of this machine.
        set(SWIGOUT ${CMAKE_CURRENT_BINARY_DIR}/swig)
        file(REMOVE_RECURSE ${SWIGOUT})
        file(MAKE_DIRECTORY ${SWIGOUT}/java)
        execute_process(COMMAND ${SWIG_EXECUTABLE} -Wall -Werror -c++ -java -I../../../../.. -package fiftyone.devicedetection.hash.engine.onpremise.interop.swig -outdir ${SWIGOUT}/java -o ${SWIGOUT}/Java_Hash_Engine.cpp hash_java.i WORKING_DIRECTORY ${CMAKE_CURRENT_LIST_DIR} COMMAND_ERROR_IS_FATAL ANY)
        file(GLOB NEW_PROXIES ${SWIGOUT}/java/*.java)
        # SWIG's own std_vector.i puts a this-escape annotation on the vector
        # constructors that take an array or an Iterable. hash_java.i makes
        # the vector proxies final, so javac has no escape to report, and the
        # annotation is removed from final classes only. The run stops if any
        # proxy still turns a javac warning off.
        foreach (PROXY ${NEW_PROXIES})
            file(READ ${PROXY} JAVA)
            if ("${JAVA}" MATCHES "\npublic final class ")
                string(REGEX REPLACE "  @SuppressWarnings\\(\"this-escape\"\\)\n(  public [A-Za-z0-9_]+\\()" "\\1" JAVA "${JAVA}")
                file(WRITE ${PROXY} "${JAVA}")
            endif()
            if ("${JAVA}" MATCHES "@SuppressWarnings")
                message(FATAL_ERROR "${PROXY} turns a warning off")
            endif()
        endforeach()
        file(GLOB OLD_PROXIES ${SRCOUT}/interop/swig/*.java)
        file(REMOVE ${OLD_PROXIES})
        file(COPY ${NEW_PROXIES} DESTINATION ${SRCOUT}/interop/swig)
        # SWIG converts a derived class pointer to its base class through a
        # jlong read as a pointer of another type, with no typemap to change
        # it. Each conversion is replaced with the functions hash_java.i
        # defines, and the run stops if one is left.
        file(READ ${SWIGOUT}/Java_Hash_Engine.cpp WRAPPER)
        string(REGEX REPLACE
            "\\*\\(([^\n]+) \\*\\*\\)&baseptr = \\*\\(([^\n]+) \\*\\*\\)&jarg1;"
            "baseptr = fiftyoneToJlong< \\1 * >(fiftyoneFromJlong< \\2 * >(jarg1));"
            WRAPPER "${WRAPPER}")
        string(FIND "${WRAPPER}" "&baseptr" PUNNED)
        if (NOT PUNNED EQUAL -1)
            message(FATAL_ERROR "A base class conversion in the SWIG wrapper was not replaced")
        endif()
        file(WRITE ${SWIGOUT}/Java_Hash_Engine.cpp "${WRAPPER}")
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
	target_compile_options(fiftyone-hash-java PRIVATE "/W4")
else()
	target_compile_options(fiftyone-hash-java PRIVATE ${COMPILE_OPTION_DEBUG} "-Wall" "-Wextra")
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

# cxx turns warnings into errors on its own libraries whatever the build. Each
# target here, cxx's and the wrapper, follows _51DEGREES_WARNINGS_AS_ERRORS
# instead. When it is on, the linker and the MSVC librarian stop on a warning
# too.
function(fiftyone_follow_warnings_switch DIR)
	get_property(TARGETS DIRECTORY ${DIR} PROPERTY BUILDSYSTEM_TARGETS)
	foreach (TARGET ${TARGETS})
		get_target_property(TYPE ${TARGET} TYPE)
		if (NOT TYPE MATCHES "_LIBRARY$|^EXECUTABLE$" OR TYPE STREQUAL "INTERFACE_LIBRARY")
			continue()
		endif()
		# Set on each target rather than for the directory, as CMake 4.5
		# warns about directory-wide commands such as add_compile_options.
		if (MSVC)
			target_compile_options(${TARGET} BEFORE PRIVATE /W4)
		else()
			target_compile_options(${TARGET} BEFORE PRIVATE -Wall -Wextra -fPIC)
		endif()
		set_property(TARGET ${TARGET}
			PROPERTY COMPILE_WARNING_AS_ERROR ${_51DEGREES_WARNINGS_AS_ERRORS})
		get_target_property(OPTIONS ${TARGET} COMPILE_OPTIONS)
		if (OPTIONS)
			list(REMOVE_ITEM OPTIONS "-Werror" "/WX")
			set_property(TARGET ${TARGET} PROPERTY COMPILE_OPTIONS ${OPTIONS})
		endif()
		if (_51DEGREES_WARNINGS_AS_ERRORS)
			if (MSVC AND TYPE STREQUAL "STATIC_LIBRARY")
				set_property(TARGET ${TARGET} APPEND PROPERTY STATIC_LIBRARY_OPTIONS "/WX")
			elseif (MSVC)
				target_link_options(${TARGET} PRIVATE "/WX")
			elseif (APPLE AND NOT TYPE STREQUAL "STATIC_LIBRARY")
				target_link_options(${TARGET} PRIVATE "LINKER:-fatal_warnings")
			elseif (NOT TYPE STREQUAL "STATIC_LIBRARY")
				target_link_options(${TARGET} PRIVATE "LINKER:--fatal-warnings")
			endif()
		endif()
	endforeach()
	get_property(SUBDIRECTORIES DIRECTORY ${DIR} PROPERTY SUBDIRECTORIES)
	foreach (SUBDIRECTORY ${SUBDIRECTORIES})
		fiftyone_follow_warnings_switch(${SUBDIRECTORY})
	endforeach()
endfunction()
fiftyone_follow_warnings_switch(${CMAKE_CURRENT_SOURCE_DIR})
message(STATUS "Warnings as errors: ${_51DEGREES_WARNINGS_AS_ERRORS}")
