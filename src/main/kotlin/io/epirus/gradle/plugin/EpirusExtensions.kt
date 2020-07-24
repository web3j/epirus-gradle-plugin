package io.epirus.gradle.plugin

import org.codehaus.groovy.runtime.InvokerHelper

/**
 * Extension method mimicking Groovy dynamic properties.
 */
internal operator fun Any.get(property: String): Any {
    return InvokerHelper.getProperty(this, property)
}
