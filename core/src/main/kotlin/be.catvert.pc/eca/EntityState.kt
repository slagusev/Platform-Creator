package be.catvert.pc.eca

import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.EmptyAction
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.utility.Renderable
import be.catvert.pc.utility.Updeatable
import be.catvert.pc.utility.cast
import com.badlogic.gdx.graphics.g2d.Batch
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.reflect.KClass

/**
 * Représente un état d'une entité, cet état contient différents components.
 * Un état est par exemple quand le joueur à sa vie au maximum, et un autre état quand il lui reste 1 point de vie.
 * Différents états permettent d'avoir différentes interactions avec l'entité au cour du temps.
 */
class EntityState(var name: String, components: MutableSet<Component> = mutableSetOf()) : Renderable, Updeatable {
    @JsonCreator private constructor() : this("State")

    /**
     * Représente l'entité où cet état est implémenté.
     * Injecté lors de l'ajout de l'entité à son conteneur(par exemple le niveau), via la méthode
     * @see onAddToContainer
     */
    private lateinit var entity: Entity

    @JsonProperty("comps")
    private val components: MutableSet<Component> = components

    private var active = false

    /**
     * Action appelée lorsque cet état devient actif.
     */
    var startAction: Action = EmptyAction()

    @JsonIgnore
    fun getComponents() = components.toSet()

    fun isActive() = active

    /**
     * Permet d'activer cet état.
     */
    fun active(entity: Entity, container: EntityContainer) {
        this.entity = entity

        components.forEach { it.onStateActive(entity, this, container) }

        active = true
    }

    /**
     * Permet de désactiver cet état.
     */
    fun disabled() {
        active = false
    }

    /**
     * Permet d'ajouter un component à cet état.
     * @see Component
     */
    fun addComponent(component: Component) {
        if (components.none { it.javaClass.isInstance(component) }) {
            components.add(component)

            if (active) {
                component.onStateActive(entity, this, entity.container!!)
            }
        }
    }

    /**
     * Permet de supprimer un component de cet état.
     * @see Component
     */
    fun removeComponent(component: Component) {
        components.remove(component)
    }

    /**
     * Permet d'obtenir un component présent dans cet état.
     * @return Le component ou null si le component n'existe pas.
     */
    inline fun <reified T : Component> getComponent(): T? = getComponents().firstOrNull { it is T }.cast()

    /**
     * Permet de vérifier si un component est présent dans cet état.
     */
    inline fun <reified T : Component> hasComponent(): Boolean = getComponent<T>() != null

    fun hasComponent(klass: KClass<out Component>) = getComponents().any { klass.isInstance(it) }

    override fun render(batch: Batch) {
        components.filter { it is Renderable && it.active }.forEach {
            (it as Renderable).render(batch)
        }
    }

    override fun update() {
        components.filter { it is Updeatable && it.active }.forEach {
            (it as Updeatable).update()
        }
    }

    override fun toString(): String = name

    operator fun plusAssign(component: Component) {
        addComponent(component)
    }
}