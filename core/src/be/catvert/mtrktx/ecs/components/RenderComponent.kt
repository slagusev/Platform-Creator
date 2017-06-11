package be.catvert.mtrktx.ecs.components

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture

/**
 * Created by arno on 05/06/17.
 */

class RenderComponent(var texture: Pair<FileHandle, Texture>, var flipX: Boolean = false, var flipY: Boolean = false) : BaseComponent()