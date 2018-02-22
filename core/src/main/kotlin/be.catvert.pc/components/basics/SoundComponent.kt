package be.catvert.pc.components.basics

import be.catvert.pc.GameObject
import be.catvert.pc.PCGame
import be.catvert.pc.components.Component
import be.catvert.pc.containers.Level
import be.catvert.pc.managers.ResourceManager
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Component permettant d'ajouter des sons à une entité
 */
@Description("Ajoute la possibilité d'ajouter des sons à une entité")
class SoundComponent(var sounds: ArrayList<SoundData>) : Component(), ResourceLoader, CustomEditorImpl, CustomEditorTextImpl {
    constructor(vararg sounds: SoundData) : this(arrayListOf(*sounds))
    @JsonCreator private constructor() : this(arrayListOf())

    class SoundData(var soundFile: FileWrapper, var levelResources: Boolean = false) : CustomEditorImpl, ResourceLoader {
        @JsonCreator private constructor() : this(Constants.defaultSoundPath.toFileWrapper())

        private var sound: Sound? = null

        fun play() {
            sound?.play(PCGame.soundVolume)
        }

        override fun loadResources() {
            sound = ResourceManager.getSound(soundFile.get())
        }

        override fun toString(): String = soundFile.get().nameWithoutExtension()

        override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
            with(ImGui) {
                val soundsResources = if (levelResources) level.resourcesSounds() else PCGame.gameSounds

                val index = intArrayOf(soundsResources.indexOf(soundFile.get()))
                functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                    if (ImGuiHelper.comboWithSettingsButton("son", index, soundsResources.map { it.nameWithoutExtension() }, {
                                checkbox("sons importés", ::levelResources)
                            })) {
                        soundFile = soundsResources[index[0]].toFileWrapper()
                    }
                }
            }
        }
    }

    /**
     * Permet de jouer le son spécifié
     */
    fun playSound(soundIndex: Int) {
        if (soundIndex in sounds.indices)
            sounds[soundIndex].play()
    }

    override fun loadResources() {
        sounds.forEach { it.loadResources() }
    }

    override fun insertImgui(label: String, gameObject: GameObject, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        ImGuiHelper.addImguiWidgetsArray("sounds", sounds, { it.toString() }, { SoundData(Constants.defaultSoundPath.toFileWrapper()) }, gameObject, level, editorSceneUI)
    }

    override fun insertText() {
        ImGuiHelper.textColored(Color.RED, "<-->")
        sounds.forEach {
            ImGuiHelper.textPropertyColored(Color.ORANGE, "son :", it.soundFile.get().nameWithoutExtension())
        }
        ImGuiHelper.textColored(Color.RED, "<-->")
    }
}