package io.tokend.template.util.validation

/**
 * Validator of password strength
 */
object PasswordValidator :
    RegexValidator("^.{6,}$")