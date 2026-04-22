package jp.xhw.mikke.platform.auth

object PasswordPolicy {
    const val MIN_LENGTH: Int = 8

    private const val INVALID_PASSWORD_MESSAGE =
        "password must be at least 8 characters and include at least one letter and one digit"

    fun validate(password: String) {
        require(
            password.length >= MIN_LENGTH &&
                password.any(Char::isLetter) &&
                password.any(Char::isDigit),
        ) { INVALID_PASSWORD_MESSAGE }
    }
}
