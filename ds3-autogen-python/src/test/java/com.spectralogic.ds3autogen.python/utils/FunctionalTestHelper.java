/*
 * ******************************************************************************
 *   Copyright 2014-2016 Spectra Logic Corporation. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *   this file except in compliance with the License. A copy of the License is located at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file.
 *   This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *   CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *   specific language governing permissions and limitations under the License.
 * ****************************************************************************
 */

package com.spectralogic.ds3autogen.python.utils;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3autogen.api.models.HttpVerb;
import com.spectralogic.ds3autogen.api.models.Operation;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.spectralogic.ds3autogen.utils.ConverterUtil.hasContent;
import static org.junit.Assert.assertTrue;

/**
 * Contains utilities for testing the python generated code
 */
public class FunctionalTestHelper {

    /**
     * Determines if the code contains the specified request handler
     */
    public static void hasRequestHandler(
            final String requestName,
            final HttpVerb httpVerb,
            final ImmutableList<String> reqArgs,
            final ImmutableList<String> optArgs,
            final ImmutableList<String> voidArgs,
            final String code) {
        assertTrue(hasRequestHandlerDef(requestName, code));
        assertTrue(hasRequestHandlerInit(reqArgs, optArgs, code));
        assertTrue(hasHttpVerb(httpVerb, code));
        if (hasContent(reqArgs)) {
            for (final String arg : reqArgs) {
                assertTrue(requestHasReqParam(arg, code));
            }
        }
        if (hasContent(optArgs)) {
            for (final String arg : optArgs) {
                assertTrue(requestHasOptParam(arg, code));
            }
        }
        if (hasContent(voidArgs)) {
            for (final String arg : voidArgs) {
                assertTrue(requestHasVoidParam(arg, code));
            }
        }
    }

    /**
     * Determines if the code contains the specified HttpVerb
     */
    private static boolean hasHttpVerb(final HttpVerb httpVerb, final String code) {
        final String search = "self.http_verb = HttpVerb." + httpVerb.toString();
        return code.contains(search);
    }

    /**
     * Determines if the code contains the void argument's query param assignment
     */
    private static boolean requestHasVoidParam(final String paramName, final String code) {
        final String search = "self.query_params['" + paramName + "'] = ''";
        return code.contains(search);
    }

    /**
     * Determines if the code contains the optional query parameter assignment
     */
    private static boolean  requestHasOptParam(final String paramName, final String code) {
        final Pattern search = Pattern.compile(
                "\\s+if " + paramName + " is not None:"
                + "\\s+self\\.query_params\\['" + paramName + "'\\] = " + paramName,
                Pattern.MULTILINE | Pattern.UNIX_LINES);
        return search.matcher(code).find();
    }

    /**
     * Determines if the code contains the required parameter assignment
     */
    private static boolean requestHasReqParam(final String paramName, final String code) {
        final String search = "self." + paramName + " = " + paramName;
        return code.contains(search);
    }

    /**
     * Determines if the code contains the python request handler definition
     */
    private static boolean hasRequestHandlerDef(
            final String requestName,
            final String code) {
        final String search = "class " + requestName + "(AbstractRequest):";
        return code.contains(search);
    }

    /**
     * Determines if the code contains the python request handler init line
     */
    private static boolean hasRequestHandlerInit(
            final ImmutableList<String> reqArgs,
            final ImmutableList<String> optArgs,
            final String code) {
        final StringBuilder search = new StringBuilder();
        search.append("def __init__(self");
        if (hasContent(reqArgs)) {
            final String reqArgString = reqArgs.stream()
                    .collect(Collectors.joining(", "));
            search.append(", ").append(reqArgString);
        }
        if (hasContent(optArgs)) {
            final String optArgString = optArgs.stream()
                    .map(i -> i + "=None")
                    .collect(Collectors.joining(", "));
            search.append(", ").append(optArgString);
        }
        search.append("):");
        return code.contains(search.toString());
    }

    /**
     * Determines if the code contains the operation query param assignment
     */
    public static boolean hasOperation(final Operation operation, final String code) {
        final String search = "self.query_params['operation'] = '" + operation.toString().toLowerCase() + "'";
        return code.contains(search);
    }
}