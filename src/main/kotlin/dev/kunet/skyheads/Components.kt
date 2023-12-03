package dev.kunet.skyheads

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Component(private val text: String) {
    private var parent: Component? = null
    private var hover: String? = null
    private var action: ClickEvent? = null

    operator fun plus(other: Component): Component {
        other.parent = this
        return other
    }

    operator fun plus(other: String): Component {
        val component = Component(other)
        component.parent = this
        return component
    }

    fun setHover(hover: String): Component {
        this.hover = hover
        return this
    }

    fun setAction(action: ClickEvent): Component {
        this.action = action
        return this
    }

    fun setAction(action: ClickEvent.Action, value: String): Component {
        this.action = ClickEvent(action, value)
        return this
    }

    fun build(): BaseComponent {
        var head = this
        val seen = mutableSetOf(head)
        while (head.parent != null) {
            head = head.parent!!
            if (!seen.add(head)) break
        }

        val base = TextComponent("")
        for (component in seen.reversed()) {
            val textComponent = TextComponent(component.text.colorCode())
            if (component.hover != null) textComponent.hoverEvent =
                HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent(component.hover!!.colorCode())))
            if (component.action != null) textComponent.clickEvent = component.action
            base.addExtra(textComponent)
        }

        return base
    }
}

fun String.comp(): Component = Component(this)

fun CommandSender.send(component: Component) {
    val build = component.build()

    if (this is Player) {
        spigot().sendMessage(build)
        return
    }

    sendMessage(build.toLegacyText())
}
