/*-
 * #%L
 * math-evaluator
 * %%
 * Copyright (C) 2019 - 2022 Frederik Kammel
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package owpk.mathevaluator

/**
 * Wrapper for a double that extends the [MathLiteral] class
 */
data class MathNumber
constructor(val value: Double = 0.0) : MathLiteral() {

    override fun toString(): String = formulaRepresentation

    override val formulaRepresentation: String
        get() = value.toString()

    override fun supportsImplicitMultiplication(previousLiteral: MathLiteral): Boolean = false
}

fun Double.toMathLiteral() = MathNumber(this)
