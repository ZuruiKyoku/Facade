package com.slygames.facade.data.model

/**
 * Metadata + parsed `appfilter.xml` contents for a third-party icon pack
 * APK, produced by the icon pack parsing engine
 * (`com.slygames.facade.features.appdrawer` icon override pipeline) and
 * consumed by the app drawer / workspace to swap in themed drawables.
 *
 * See the "Icon pack drawable" spec used by Nova/ADW/Apex-compatible packs:
 * `<item component="ComponentInfo{pkg/activity}" drawable="res_name" />`
 * inside `res/xml/appfilter.xml` of [packageName].
 */
data class IconPackInfo(
    val packageName: String,
    val displayName: String,
    /** componentKey ("pkg/activity") -> drawable resource name inside the icon pack APK. */
    val componentToDrawable: Map<String, String>,
    /** Drawable resource names usable as random fallback "back" images for un-themed icons. */
    val iconBackNames: List<String> = emptyList(),
    val iconUponName: String? = null,
    val iconMaskName: String? = null,
    /** Scale factor (0f-1f) applied to the foreground icon before compositing onto [iconBackNames]. */
    val iconScale: Float = 1f
) {
    fun drawableNameFor(componentKey: String): String? = componentToDrawable[componentKey]
}
