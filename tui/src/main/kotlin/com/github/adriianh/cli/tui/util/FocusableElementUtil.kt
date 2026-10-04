package com.github.adriianh.cli.tui.util

import dev.tamboui.toolkit.element.StyledElement

/**
 * Sets the element to be focusable based on the specified condition.
 *
 * @param condition A boolean value indicating whether the element should be focusable.
 *                  If true, the element will be focusable; otherwise, it will not.
 * @return The modified [StyledElement], allowing for further chaining of configuration.
 */
fun <T : StyledElement<T>> T.focusableIf(condition: Boolean): T = focusable(condition)
