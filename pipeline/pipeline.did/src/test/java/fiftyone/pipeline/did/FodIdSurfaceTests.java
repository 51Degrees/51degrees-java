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

package fiftyone.pipeline.did;

import com.swancommunity.owid.Owid;
import com.swancommunity.owid.Version;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks that {@link FodId} keeps the byte layout out of its public
 * surface, has no member for the payload version, and is only ever made by
 * reading a complete Envelope, as the 51Did specification asks.
 */
public class FodIdSurfaceTests {

    /**
     * True when a caller outside the package can reach the member.
     */
    private static boolean isVisible(Member member) {
        int modifiers = member.getModifiers();
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    /**
     * did-specification/README.md lines 33 to 35, "Application code MUST
     * NOT read the layout by hand, and the packages therefore keep the
     * offsets and lengths out of their public surface."
     */
    @Test
    public void layout_OffsetsAndLengths_AreNotPublic() {
        List<String> visible = new ArrayList<>();
        for (Field field : FodId.class.getDeclaredFields()) {
            if (isVisible(field)) {
                visible.add(field.getName());
            }
        }
        assertEquals(
            "FodId fields a caller can reach",
            new ArrayList<String>(),
            visible);
    }

    /**
     * did-specification/package-surface.md lines 55 and 56, "The version
     * belongs to the refusal and MUST NOT appear as a member of the
     * identifier". The only version a caller reads from a FodId is the
     * Envelope's own, through getVersion.
     */
    @Test
    public void payloadVersion_IsNotAMemberOfTheIdentifier() {
        List<String> visible = new ArrayList<>();
        for (Method method : FodId.class.getDeclaredMethods()) {
            boolean namesVersion = method.getName()
                .toLowerCase(Locale.ROOT)
                .contains("version");
            if (isVisible(method) &&
                namesVersion &&
                method.getReturnType() != Version.class) {
                visible.add(method.getName());
            }
        }
        assertEquals(
            "FodId methods that give a version other than the Envelope's",
            new ArrayList<String>(),
            visible);
    }

    /**
     * did-specification/identifier-layout.md lines 42 and 43, "a 51Did
     * package MUST create instances only by reading bytes that are already
     * a complete Envelope."
     */
    @Test
    public void instances_AreMadeOnlyByReadingAnEnvelope() {
        assertTrue(
            "FodId is final, so no subclass can make an instance",
            Modifier.isFinal(FodId.class.getModifiers()));
        for (Constructor<?> constructor :
                FodId.class.getDeclaredConstructors()) {
            assertTrue(
                "constructor " + constructor + " is private",
                Modifier.isPrivate(constructor.getModifiers()));
        }
        List<Class<?>> readable = Arrays.<Class<?>>asList(
            String.class, byte[].class, Owid.class);
        for (Method method : FodId.class.getDeclaredMethods()) {
            if (isVisible(method) &&
                Modifier.isStatic(method.getModifiers()) &&
                method.getReturnType() == FodId.class) {
                Class<?>[] parameters = method.getParameterTypes();
                assertTrue(
                    method.getName() + " takes one encoded Envelope",
                    parameters.length == 1 &&
                    readable.contains(parameters[0]));
            }
        }
    }
}
