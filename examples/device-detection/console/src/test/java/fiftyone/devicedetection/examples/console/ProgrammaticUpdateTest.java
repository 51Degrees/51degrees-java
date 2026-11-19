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

package fiftyone.devicedetection.examples.console;

import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.DataUpdateService;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * The data update example illustrates a programmatic (non-automatic)
 * update as well as update on start-up, the file system watcher and
 * polling (device-detection-specification/required-examples.md, "The data
 * update service has a range of options available. Implementors SHOULD
 * illustrate:", the last of which is "programmatic (non-automatic)
 * update").
 *
 * An update needs a licence key and the Enterprise data file, so the test
 * gives the example's update step a stand-in service and engine, and checks
 * that the step asks the service to check for an update of that engine now
 * and gives back the status the service answers.
 */
public class ProgrammaticUpdateTest {

    /**
     * A stand-in for an interface that answers Object's methods and records
     * every other call.
     */
    @SuppressWarnings("unchecked")
    private static <T> T standIn(Class<T> type, List<Object[]> calls,
                                 Object answer) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    case "toString":
                        return type.getSimpleName();
                    default:
                        calls.add(new Object[]{method.getName(), args});
                        return answer;
                }
            });
    }

    @Test
    public void updateNow_AsksTheServiceToCheckThatEngineNow() {
        List<Object[]> engineCalls = new ArrayList<>();
        List<Object[]> serviceCalls = new ArrayList<>();
        OnPremiseAspectEngine<?, ?> engine =
            standIn(OnPremiseAspectEngine.class, engineCalls, null);
        DataUpdateService service = standIn(DataUpdateService.class,
            serviceCalls,
            DataUpdateService.AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED);

        DataUpdateService.AutoUpdateStatus status =
            UpdateDataFile.updateNow(service, engine);

        assertEquals(DataUpdateService.AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED,
            status);
        assertEquals(1, serviceCalls.size());
        assertEquals("checkForUpdate", serviceCalls.get(0)[0]);
        assertSame(engine, ((Object[]) serviceCalls.get(0)[1])[0]);
    }
}
