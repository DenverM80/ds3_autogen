/*
 * ******************************************************************************
 *   Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
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

package com.spectralogic.ds3autogen.go.models.request

/**
 * Represents a with-constructor used to set optional parameters in a
 * request handler.
 */
data class WithConstructor(val name: String, // name of the optional parameter
                           val type: String, // type of the optional parameter
                           val key: String, // the key used to set the optional query parameter
                           val assignment: String) { //The value to assign to the query parameter entry
}