/*
 * Copyright (c) 2022. 
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.dom.Document
import nl.adaptivity.xmlutil.dom.Node
import nl.adaptivity.xmlutil.dom.ProcessingInstruction

internal class ProcessingInstructionImpl(
    ownerDocument: Document,
    override val target: String,
    data: String
) : CharacterDataImpl(ownerDocument, data), ProcessingInstruction {
    constructor(ownerDocument: DocumentImpl, original: ProcessingInstruction) : this(
        ownerDocument,
        original.nodeName,
        original.data
    )

    override val nodeType: Short get() = Node.PROCESSING_INSTRUCTION_NODE

    override val nodeName: String get() = target
}
