package jp.xhw.mikke.mobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
