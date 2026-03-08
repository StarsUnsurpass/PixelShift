/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2024 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the \"License\");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package com.t8rin.imagetoolbox.feature.draw.presentation.components.model

import android.os.Parcelable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.t8rin.imagetoolbox.core.domain.model.IntegerSize
import com.t8rin.imagetoolbox.core.domain.model.Pt
import com.t8rin.imagetoolbox.feature.draw.domain.DrawMode
import com.t8rin.imagetoolbox.feature.draw.domain.DrawPathMode
import com.t8rin.imagetoolbox.feature.draw.presentation.components.utils.PathHelper
import kotlinx.parcelize.Parcelize

@Parcelize
data class UiDrawPath(
    val points: List<Offset>,
    val drawDownPosition: Offset,
    val currentDrawPosition: Offset,
    val pathMode: UiDrawPathMode
) : Parcelable {
    fun createPath(
        strokeWidth: Pt,
        canvasSize: IntegerSize,
        isEraserOn: Boolean,
        drawMode: DrawMode
    ): Path {
        var path = Path()
        val helper = PathHelper(
            drawDownPosition = drawDownPosition,
            currentDrawPosition = currentDrawPosition,
            onPathChange = { path = it },
            strokeWidth = strokeWidth,
            canvasSize = canvasSize,
            drawPathMode = pathMode.toDomain(),
            isEraserOn = isEraserOn,
            drawMode = drawMode
        )

        val drawPathMode = pathMode.toDomain()
        if (drawPathMode is DrawPathMode.Free || drawPathMode is DrawPathMode.Lasso) {
            if (points.isNotEmpty()) {
                path.moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p = points[i]
                    val prev = points[i - 1]
                    path.quadraticTo(
                        prev.x,
                        prev.y,
                        (prev.x + p.x) / 2,
                        (prev.y + p.y) / 2
                    )
                }
            }
        } else {
            // For shapes, PathHelper logic is more complex, we use its built-in methods
            when (drawPathMode) {
                is DrawPathMode.Line,
                is DrawPathMode.LinePointingArrow,
                is DrawPathMode.DoubleLinePointingArrow -> helper.drawLine()

                is DrawPathMode.Rect,
                is DrawPathMode.OutlinedRect -> helper.drawRect(
                    rotationDegrees = (drawPathMode as? DrawPathMode.Rect)?.rotationDegrees
                        ?: (drawPathMode as? DrawPathMode.OutlinedRect)?.rotationDegrees ?: 0,
                    cornerRadius = (drawPathMode as? DrawPathMode.Rect)?.cornerRadius
                        ?: (drawPathMode as? DrawPathMode.OutlinedRect)?.cornerRadius ?: 0f
                )

                DrawPathMode.Triangle,
                DrawPathMode.OutlinedTriangle -> helper.drawTriangle()

                DrawPathMode.Oval,
                DrawPathMode.OutlinedOval -> helper.drawOval()

                is DrawPathMode.Polygon -> helper.drawPolygon(
                    vertices = drawPathMode.vertices,
                    rotationDegrees = drawPathMode.rotationDegrees,
                    isRegular = drawPathMode.isRegular
                )

                is DrawPathMode.OutlinedPolygon -> helper.drawPolygon(
                    vertices = drawPathMode.vertices,
                    rotationDegrees = drawPathMode.rotationDegrees,
                    isRegular = drawPathMode.isRegular
                )

                is DrawPathMode.Star -> helper.drawStar(
                    vertices = drawPathMode.vertices,
                    innerRadiusRatio = drawPathMode.innerRadiusRatio,
                    rotationDegrees = drawPathMode.rotationDegrees,
                    isRegular = drawPathMode.isRegular
                )

                is DrawPathMode.OutlinedStar -> helper.drawStar(
                    vertices = drawPathMode.vertices,
                    innerRadiusRatio = drawPathMode.innerRadiusRatio,
                    rotationDegrees = drawPathMode.rotationDegrees,
                    isRegular = drawPathMode.isRegular
                )

                else -> Unit
            }
        }
        return path
    }
}
