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

package com.spectralogic.ds3autogen.net.model.typeparser;

import com.spectralogic.ds3autogen.net.model.common.NullableVariable;

public class NullableElement extends NullableVariable {

    private final String xmlTag;

    /** The name of the model parser associated with this type */
    private final String parserName;


    public NullableElement(
            final String name,
            final String type,
            final boolean questionMarkForNullable,
            final boolean nullable,
            final String xmlTag,
            final String parserName) {
        super(name, type, questionMarkForNullable, nullable);

        this.xmlTag = xmlTag;
        this.parserName = parserName;
    }

    public String getXmlTag() {
        return xmlTag;
    }

    public String getParserName() {
        return parserName;
    }
}
