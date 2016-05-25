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

package com.spectralogic.ds3autogen.python.generators.type;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3autogen.api.models.Ds3Element;
import com.spectralogic.ds3autogen.api.models.Ds3Type;
import com.spectralogic.ds3autogen.python.model.type.TypeAttribute;
import com.spectralogic.ds3autogen.python.model.type.TypeDescriptor;
import com.spectralogic.ds3autogen.python.model.type.TypeElement;
import com.spectralogic.ds3autogen.python.model.type.TypeElementList;
import com.spectralogic.ds3autogen.utils.collections.GuavaCollectors;

import static com.spectralogic.ds3autogen.utils.ConverterUtil.hasContent;
import static com.spectralogic.ds3autogen.utils.ConverterUtil.isEmpty;
import static com.spectralogic.ds3autogen.utils.Ds3ElementUtil.getEncapsulatingTagAnnotations;
import static com.spectralogic.ds3autogen.utils.Ds3ElementUtil.getXmlTagName;
import static com.spectralogic.ds3autogen.utils.Ds3ElementUtil.isAttribute;
import static com.spectralogic.ds3autogen.utils.Ds3TypeClassificationUtil.isEnumType;
import static com.spectralogic.ds3autogen.utils.NormalizingContractNamesUtil.removePath;

public class BaseTypeGenerator implements TypeModelGenerator<TypeDescriptor> {

    @Override
    public TypeDescriptor generate(
            final Ds3Type ds3Type,
            final ImmutableMap<String, Ds3Type> typeMap) {
        final String name = removePath(ds3Type.getName());
        final ImmutableList<TypeAttribute> attributes = toAttributes(ds3Type.getElements());
        final ImmutableList<TypeElement> elements = toElements(ds3Type.getElements(), typeMap);
        final ImmutableList<TypeElementList> elementLists = toElementLists(ds3Type.getElements(), typeMap);

        return new TypeDescriptor(
                name,
                attributes,
                elements,
                elementLists);
    }

    //TODO
    /**
     * Converts all Ds3Elements that describe an attribute into a TypeAttribute. All other
     * Ds3Elements are removed.
     */
    protected static ImmutableList<TypeAttribute> toAttributes(final ImmutableList<Ds3Element> ds3Elements) {
        if (isEmpty(ds3Elements)) {
            return ImmutableList.of();
        }
        return ds3Elements.stream()
                .filter(ds3Element -> isAttribute(ds3Element.getDs3Annotations()))
                .map(BaseTypeGenerator::toAttribute)
                .collect(GuavaCollectors.immutableList());
    }

    //TODO
    /**
     * Converts a Ds3Element into a TypeAttribute
     */
    protected static TypeAttribute toAttribute(final Ds3Element ds3Element) {
        final String name = getXmlTagName(ds3Element);
        return new TypeAttribute(name);
    }

    //TODO
    /**
     * Converts all Ds3Elements that describe a single xml element into a TypeElement. All other
     * Ds3Elements are removed.
     */
    protected static ImmutableList<TypeElement> toElements(
            final ImmutableList<Ds3Element> ds3Elements,
            final ImmutableMap<String, Ds3Type> typeMap) {
        if (isEmpty(ds3Elements)) {
            return ImmutableList.of();
        }
        return ds3Elements.stream()
                .filter(ds3Element -> isEmpty(ds3Element.getComponentType()) && !isAttribute(ds3Element.getDs3Annotations()))
                .map(element -> toElement(element, typeMap))
                .collect(GuavaCollectors.immutableList());
    }

    //TODO
    /**
     * Converts a Ds3Element into a TypeElement
     */
    protected static TypeElement toElement(final Ds3Element ds3Element, final ImmutableMap<String, Ds3Type> typeMap) {
        final String xmlTag = getXmlTagName(ds3Element);
        final String typeModel = getTypeModelName(ds3Element.getType(), ds3Element.getComponentType(), typeMap);
        return new TypeElement(xmlTag, typeModel);
    }

    //TODO
    /**
     * Gets the type model name that describes the specified type, or 'None' if there
     * is no associated type model
     */
    protected static String getTypeModelName(
            final String type,
            final String componentType,
            final ImmutableMap<String, Ds3Type> typeMap) {
        if (hasContent(componentType) && typeMap.containsKey(componentType) && !isEnumType(componentType, typeMap)) {
            return removePath(componentType);
        } else if(isEmpty(componentType) && typeMap.containsKey(type) && !isEnumType(type, typeMap)) {
            return removePath(type);
        }
        return "None";
    }

    //TODO
    /**
     * Converts all Ds3Elements that describe a list of xml elements into a TypeElementList.
     * All other Ds3Elements are removed.
     */
    protected static ImmutableList<TypeElementList> toElementLists(
            final ImmutableList<Ds3Element> ds3Elements,
            final ImmutableMap<String, Ds3Type> typeMap) {
        if (isEmpty(ds3Elements)) {
            return ImmutableList.of();
        }
        return ds3Elements.stream()
                .filter(ds3Element -> hasContent(ds3Element.getComponentType()) && !isAttribute(ds3Element.getDs3Annotations()))
                .map(element -> toElementList(element, typeMap))
                .collect(GuavaCollectors.immutableList());
    }

    //TODO
    /**
     * Converts a Ds3Element into a TypeElementList
     */
    protected static TypeElementList toElementList(
            final Ds3Element ds3Element,
            final ImmutableMap<String, Ds3Type> typeMap) {
        final String xmlTag = getXmlTagName(ds3Element);
        final String encapsulatingTag = getEncapsulatingTagAnnotations(ds3Element.getDs3Annotations());
        final String typeModel = getTypeModelName(ds3Element.getType(), ds3Element.getComponentType(), typeMap);

        if(hasContent(encapsulatingTag)) {
            return new TypeElementList(xmlTag, encapsulatingTag, typeModel);
        }
        return new TypeElementList(xmlTag, "None", typeModel);
    }
}
