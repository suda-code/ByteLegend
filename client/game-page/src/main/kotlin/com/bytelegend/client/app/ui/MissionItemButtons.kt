/*
 * Copyright 2021 ByteLegend Technologies and the original author or authors.
 *
 * Licensed under the GNU Affero General Public License v3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://github.com/ByteLegend/ByteLegend/blob/master/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytelegend.client.app.ui

import com.bytelegend.app.client.api.EventListener
import com.bytelegend.app.client.api.GameMission
import com.bytelegend.app.client.api.missionItemsButtonRepaintEvent
import com.bytelegend.app.client.ui.bootstrap.BootstrapSpinner
import com.bytelegend.app.shared.PixelCoordinate
import com.bytelegend.app.shared.objects.GameObjectRole
import com.bytelegend.client.app.engine.GAME_ANIMATION_EVENT
import kotlinext.js.jso
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import react.Component
import react.Fragment
import react.State
import react.create
import react.dom.html.ReactHTML.img
import react.react
import kotlin.math.abs
import kotlin.math.max

private const val MISSION_TITLE_BUTTONS_LAYER = "mission-title-buttons-layer"

class MissionItemButtons : GameUIComponent<GameProps, State>() {
    private val onAnimation: EventListener<Nothing> = this::onAnimation

    private val divCoordinate: PixelCoordinate
        get() = canvasCoordinateInGameContainer - canvasCoordinateInMap

    override fun render() = Fragment.create {
        absoluteDiv(
            left = divCoordinate.x,
            top = divCoordinate.y,
            width = mapPixelSize.width,
            height = mapPixelSize.height,
            zIndex = Layer.MissionItemButton.zIndex(),
            className = MISSION_TITLE_BUTTONS_LAYER
        ) {
            it.id = MISSION_TITLE_BUTTONS_LAYER
            activeScene.objects.getByRole<GameMission>(GameObjectRole.Mission).forEach {
                child(MissionItemButton::class.react, jso {
                    this.game = props.game
                    this.mission = it
                })
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onAnimation(n: Nothing) {
        document.getElementById(MISSION_TITLE_BUTTONS_LAYER)?.apply {
            val divStyle = unsafeCast<HTMLDivElement>().style
            divStyle.top = "${divCoordinate.y}px"
            divStyle.left = "${divCoordinate.x}px"
        }
    }

    override fun componentDidMount() {
        super.componentDidMount()
        props.game.eventBus.on(GAME_ANIMATION_EVENT, onAnimation)
    }

    override fun componentWillUnmount() {
        super.componentWillUnmount()
        props.game.eventBus.remove(GAME_ANIMATION_EVENT, onAnimation)
    }
}

interface MissionItemButtonProps : GameProps {
    var mission: GameMission
}

interface MissionItemButtonState : State {
    var item: String?
    var disabled: Boolean
    var loading: Boolean
    var flickering: Boolean
}

class MissionItemButton(props: MissionItemButtonProps) : Component<MissionItemButtonProps, MissionItemButtonState>(props) {
    init {
        state = jso {
            disabled = distanceToHero > 2
            loading = false
            flickering = distanceToHero <= 2
        }
    }

    private val onRepaint: EventListener<Nothing> = this::onRepaint
    private val distanceToHero: Int
        get() = max(abs(props.game.heroPlayer.x - props.mission.gridCoordinate.x), abs(props.game.heroPlayer.y - props.mission.gridCoordinate.y))

    override fun componentDidMount() {
        props.game.eventBus.on(missionItemsButtonRepaintEvent(props.mission.id), onRepaint)
    }

    override fun componentWillUnmount() {
        props.game.eventBus.remove(missionItemsButtonRepaintEvent(props.mission.id), onRepaint)
    }

    private fun onRepaint(n: Nothing) {
        setState {
            disabled = distanceToHero > 2
            flickering = distanceToHero <= 2
        }
    }

    override fun render() = Fragment.create {
        val item = props.game.itemManager.getItemForMission(props.mission.id).firstOrNull()
        if (item != null) {
            var className = "mission-item-button" // mission-item-button-animation" else "mission-item-button"
            if (state.flickering) {
                className += " mission-item-button-animation"
            }
            if (state.disabled) {
                className += " mission-item-button-disabled"
            }
            absoluteDiv(
                left = props.mission.pixelCoordinate.x - 16,
                top = props.mission.pixelCoordinate.y - 16,
                width = 64,
                height = 64,
                className = className,
                zIndex = Layer.MissionItemButton.zIndex(),
            ) { div ->
                img {
                    src = props.game.resolve("/img/icon/$item.png")
                }
                if (state.loading) {
                    BootstrapSpinner {
                        animation = "border"
                    }
                }
                div.onClick = {
                    if (state.disabled) {
                        val title = props.game.i("CantUseItem")
                        val body = props.game.i("YouMustBeAdjacentToUseTheItem", item)
                        props.game.toastController.addToast(title, body, 5000)
                    }
                }
            }
        }
    }
}