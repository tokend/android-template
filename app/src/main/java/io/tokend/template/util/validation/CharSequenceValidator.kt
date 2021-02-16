package io.tokend.template.util.validation

interface CharSequenceValidator {
    fun isValid(sequence: CharSequence?): Boolean
}