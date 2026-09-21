package com.logix.browser.adblock

import javax.inject.Qualifier

/** Application-scoped coroutine scope for fire-and-forget background work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
