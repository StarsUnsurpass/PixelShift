/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2024 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package com.t8rin.imagetoolbox.feature.draw.presentation.components.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Polygon
import androidx.compose.material.icons.outlined.Spray
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.resources.icons.DoublePointingArrow
import com.t8rin.imagetoolbox.core.resources.icons.FloodFill
import com.t8rin.imagetoolbox.core.resources.icons.Lasso
import com.t8rin.imagetoolbox.core.resources.icons.LinePointingArrow
import com.t8rin.imagetoolbox.core.resources.icons.Oval
import com.t8rin.imagetoolbox.core.resources.icons.PaletteSwatch
import com.t8rin.imagetoolbox.core.resources.icons.Polygon
import com.t8rin.imagetoolbox.core.resources.icons.Rect
import com.t8rin.imagetoolbox.core.resources.icons.Square
import com.t8rin.imagetoolbox.core.resources.icons.Triangle
import com.t8rin.imagetoolbox.core.ui.widget.controls.selection.DrawPathModeSelector
import com.t8rin.imagetoolbox.feature.draw.domain.DrawPathMode

internal fun DrawPathMode.tolerance(): Float = when (this) {
    is DrawPathMode.FloodFill -> tolerance
    is DrawPathMode.GlobalReplace -> tolerance
    else -> 0f
}

internal fun DrawPathMode.updateFloodFill(
    tolerance: Float
): DrawPathMode = when (this) {
    is DrawPathMode.FloodFill -> copy(tolerance = tolerance)
    is DrawPathMode.GlobalReplace -> copy(tolerance = tolerance)
    else -> this
}

internal fun DrawPathMode.isFloodFill(): Boolean =
    this is DrawPathMode.FloodFill || this is DrawPathMode.GlobalReplace

internal fun DrawPathMode.getSubtitle(): Int = when (this) {
    DrawPathMode.DoubleLinePointingArrow -> R.string.double_line_arrow_sub
    DrawPathMode.DoublePointingArrow -> R.string.double_arrow_sub
    DrawPathMode.Free -> R.string.free_drawing_sub
    DrawPathMode.Lasso -> R.string.lasso_sub
    DrawPathMode.Line -> R.string.line_sub
    DrawPathMode.LinePointingArrow -> R.string.line_arrow_sub
    is DrawPathMode.OutlinedOval -> R.string.outlined_oval_sub
    is DrawPathMode.OutlinedRect -> R.string.outlined_rect_sub
    DrawPathMode.PointingArrow -> R.string.arrow_sub
    DrawPathMode.Oval -> R.string.oval_sub
    DrawPathMode.Rect -> R.string.rect_sub
    is DrawPathMode.OutlinedTriangle -> R.string.outlined_triangle_sub
    DrawPathMode.Triangle -> R.string.triangle_sub
    is DrawPathMode.OutlinedPolygon -> R.string.outlined_polygon_sub
    is DrawPathMode.Polygon -> R.string.polygon_sub
    is DrawPathMode.OutlinedStar -> R.string.outlined_star_sub
    is DrawPathMode.Star -> R.string.star_sub
    is DrawPathMode.FloodFill -> R.string.flood_fill_sub
    is DrawPathMode.GlobalReplace -> R.string.global_replace_sub
    is DrawPathMode.Spray -> R.string.spray_sub
}

internal fun DrawPathMode.getTitle(): Int = when (this) {
    DrawPathMode.DoubleLinePointingArrow -> R.string.double_line_arrow
    DrawPathMode.DoublePointingArrow -> R.string.double_arrow
    DrawPathMode.Free -> R.string.free
    DrawPathMode.Lasso -> R.string.lasso
    DrawPathMode.Line -> R.string.line
    DrawPathMode.LinePointingArrow -> R.string.line_arrow
    is DrawPathMode.OutlinedOval -> R.string.outlined_oval
    is DrawPathMode.OutlinedRect -> R.string.outlined_rect
    DrawPathMode.PointingArrow -> R.string.arrow
    DrawPathMode.Oval -> R.string.oval
    DrawPathMode.Rect -> R.string.rect
    is DrawPathMode.OutlinedTriangle -> R.string.outlined_triangle
    DrawPathMode.Triangle -> R.string.triangle
    is DrawPathMode.OutlinedPolygon -> R.string.outlined_polygon
    is DrawPathMode.Polygon -> R.string.polygon
    is DrawPathMode.OutlinedStar -> R.string.outlined_star
    is DrawPathMode.Star -> R.string.star
    is DrawPathMode.FloodFill -> R.string.flood_fill
    is DrawPathMode.GlobalReplace -> R.string.global_replace
    is DrawPathMode.Spray -> R.string.spray
}

internal fun DrawPathMode.getIcon(): ImageVector = when (this) {
    DrawPathMode.DoubleLinePointingArrow -> Icons.Rounded.DoubleArrow
    DrawPathMode.DoublePointingArrow -> Icons.Filled.DoublePointingArrow
    DrawPathMode.Free -> Icons.Rounded.CallMade
    DrawPathMode.Lasso -> Icons.Filled.Lasso
    DrawPathMode.Line -> Icons.Rounded.CallMade
    DrawPathMode.LinePointingArrow -> Icons.Filled.LinePointingArrow
    is DrawPathMode.OutlinedOval -> Icons.Rounded.RadioButtonUnchecked
    is DrawPathMode.OutlinedRect -> Icons.Rounded.CheckBoxOutlineBlank
    DrawPathMode.PointingArrow -> Icons.Rounded.CallMade
    DrawPathMode.Oval -> Icons.Filled.Oval
    DrawPathMode.Rect -> Icons.Filled.Rect
    is DrawPathMode.OutlinedTriangle -> Icons.Filled.Triangle
    DrawPathMode.Triangle -> Icons.Filled.Triangle
    is DrawPathMode.OutlinedPolygon -> Icons.Outlined.Polygon
    is DrawPathMode.Polygon -> Icons.Filled.Polygon
    is DrawPathMode.OutlinedStar -> Icons.Rounded.StarOutline
    is DrawPathMode.Star -> Icons.Rounded.Star
    is DrawPathMode.FloodFill -> Icons.Rounded.FloodFill
    is DrawPathMode.GlobalReplace -> Icons.Filled.PaletteSwatch
    is DrawPathMode.Spray -> Icons.Outlined.Spray
}
