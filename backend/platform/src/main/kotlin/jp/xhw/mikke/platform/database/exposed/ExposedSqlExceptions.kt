package jp.xhw.mikke.platform.database.exposed

import java.sql.SQLException

fun Throwable.isUniqueConstraintViolation(): Boolean =
    when (this) {
        is SQLException -> sqlState == "23000" || (nextException?.isUniqueConstraintViolation() == true)
        else -> cause?.isUniqueConstraintViolation() == true
    }
