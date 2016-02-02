/*
 * ******************************************************************************
 *   Copyright 2014-2015 Spectra Logic Corporation. All Rights Reserved.
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

package com.spectralogic.ds3autogen.c.converters;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3autogen.api.models.Ds3Element;
import com.spectralogic.ds3autogen.api.models.Ds3Type;
import com.spectralogic.ds3autogen.c.helpers.C_TypeHelper;
import com.spectralogic.ds3autogen.c.helpers.StructHelper;
import com.spectralogic.ds3autogen.c.models.Struct;
import com.spectralogic.ds3autogen.c.models.StructMember;

import java.text.ParseException;

public final class StructConverter {
    private StructConverter() {}

    public static Struct toStruct(final Ds3Type ds3Type) throws ParseException {
        final ImmutableList<StructMember> variablesList = convertDs3Elements(ds3Type.getElements());
        return new Struct(
                ds3Type.getName(),
                variablesList);
    }

    private static ImmutableList<StructMember> convertDs3Elements(final ImmutableList<Ds3Element> elementsList) throws ParseException {
        final ImmutableList.Builder<StructMember> builder = ImmutableList.builder();
        for (final Ds3Element currentElement : elementsList) {
            builder.add(new StructMember(C_TypeHelper.convertDs3ElementType(currentElement), StructHelper.getNameUnderscores(currentElement.getName())));
        }
        return builder.build();
    }
}
