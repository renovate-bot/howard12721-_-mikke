package jp.xhw.mikke.services.identity.application

import jp.xhw.mikke.services.identity.model.Email
import jp.xhw.mikke.services.identity.model.IdentityUser
import jp.xhw.mikke.services.identity.model.UserId

interface IdentityUserRepository {
    fun saveUser(user: IdentityUser)

    fun findByLogin(login: String): IdentityUser?

    fun findByEmails(emails: List<Email>): List<IdentityUser>

    fun findByIds(ids: List<UserId>): List<IdentityUser>
}

class DuplicateIdentityUserException(
    message: String = "identity user already exists",
    cause: Throwable? = null,
) : IdentityApplicationException(message, cause)
