// © Art Chaidarun

package com.chaidarun.chronofile

import android.util.Log
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable

/** All actions must be immutable */
sealed class Action {
  data class AddEntry(val activity: String, val note: String?, val latLong: Pair<Double, Double>?) :
    Action()

  data class EditEntry(
    val oldStartTime: Long,
    val newStartTime: String,
    val activity: String,
    val note: String
  ) : Action()

  data class RegisterNfcTag(val id: String, val entry: List<String>) : Action()

  data class RemoveEntry(val entry: Long) : Action()

  data class SetConfigFromText(val text: String) : Action()

  data class SetConfigFromFile(val config: Config) : Action()

  data class SetGraphGrouping(val grouped: Boolean) : Action()

  data class SetGraphMetric(val metric: Metric) : Action()

  data class SetGraphRangeEnd(val timestamp: Long) : Action()

  data class SetGraphRangeStart(val timestamp: Long) : Action()

  data class SetGraphStacking(val stacked: Boolean) : Action()

  data class SetHistory(val history: History) : Action()

  data class SetSearchQuery(val query: String?) : Action()

  data class AddWeeklyGoal(val goal: WeeklyGoal) : Action()

  data class RemoveWeeklyGoal(val goalId: String) : Action()

  data class UpdateWeeklyGoal(val goal: WeeklyGoal) : Action()

  data class SetWeeklyNotificationsEnabled(val enabled: Boolean) : Action()
  
  // New Recommendation Actions
  data class UpdateLifeBalance(val metrics: LifeBalanceMetrics) : Action()
  data class CelebrateAchievement(val achievement: Achievement) : Action()
  data class DismissRecommendation(val recommendationId: String) : Action()
  data class AcceptRecommendation(val recommendation: SmartRecommendation) : Action()
  data class UpdateHabitMetrics(val metrics: Map<String, HabitMetrics>) : Action()
  
  // Theme Action
  data class SetDarkTheme(val isDarkTheme: Boolean) : Action()
}

/** This class must be deeply immutable and preferably printable */
data class State(
  val config: Config? = null,
  val history: History? = null,
  val graphConfig: GraphConfig = GraphConfig(),
  val searchQuery: String? = null,
  val recommendations: List<SmartRecommendation> = emptyList(),
  val lifeBalance: LifeBalanceMetrics? = null,
  val habitMetrics: Map<String, HabitMetrics> = emptyMap(),
  val achievements: List<Achievement> = emptyList(),
  val dismissedRecommendations: Set<String> = emptySet(),
  val isDarkTheme: Boolean = false
)

private val reducer: (State, Action) -> State = { state, action ->
  with(state) {
    val start = System.currentTimeMillis()
    val nextState =
      when (action) {
        is Action.AddEntry ->
          copy(history = history?.withNewEntry(action.activity, action.note, action.latLong))
        is Action.EditEntry ->
          copy(
            history =
              history?.withEditedEntry(
                action.oldStartTime,
                action.newStartTime,
                action.activity,
                action.note
              )
          )
        is Action.RegisterNfcTag -> {
          val oldConfig = config ?: Config()
          val oldNfcTags = oldConfig.nfcTags ?: mutableMapOf()
          val newConfig = oldConfig.copy(nfcTags = oldNfcTags + (action.id to action.entry))
          newConfig.save()
          copy(config = newConfig)
        }
        is Action.RemoveEntry -> copy(history = history?.withoutEntry(action.entry))
        is Action.SetConfigFromText ->
          try {
            val config = Config.fromText(action.text)
            App.toast("Saved config")
            copy(config = config)
          } catch (e: Throwable) {
            App.toast("Failed to save invalid config")
            this
          }
        is Action.SetConfigFromFile -> copy(config = action.config)
        is Action.SetGraphGrouping -> copy(graphConfig = graphConfig.copy(grouped = action.grouped))
        is Action.SetGraphMetric -> copy(graphConfig = graphConfig.copy(metric = action.metric))
        is Action.SetGraphRangeEnd -> {
          val timestamp = action.timestamp
          val newSettings =
            if (timestamp >= (state.graphConfig.startTime ?: 0)) {
              graphConfig.copy(endTime = timestamp)
            } else {
              graphConfig.copy(endTime = timestamp, startTime = timestamp)
            }
          copy(graphConfig = newSettings)
        }
        is Action.SetGraphRangeStart -> {
          val timestamp = action.timestamp
          val newSettings =
            if (timestamp <= (state.graphConfig.endTime ?: Long.MAX_VALUE)) {
              graphConfig.copy(startTime = timestamp)
            } else {
              graphConfig.copy(endTime = timestamp, startTime = timestamp)
            }
          copy(graphConfig = newSettings)
        }
        is Action.SetGraphStacking -> copy(graphConfig = graphConfig.copy(stacked = action.stacked))
        is Action.SetHistory -> copy(history = action.history)
        is Action.SetSearchQuery -> copy(searchQuery = action.query)
        is Action.AddWeeklyGoal -> {
          val oldConfig = config ?: Config()
          val oldGoals = oldConfig.weeklyGoals ?: listOf()
          val newConfig = oldConfig.copy(weeklyGoals = oldGoals + action.goal)
          newConfig.save()
          copy(config = newConfig)
        }
        is Action.RemoveWeeklyGoal -> {
          val oldConfig = config ?: Config()
          val oldGoals = oldConfig.weeklyGoals ?: listOf()
          val newConfig = oldConfig.copy(weeklyGoals = oldGoals.filter { it.id != action.goalId })
          newConfig.save()
          copy(config = newConfig)
        }
        is Action.UpdateWeeklyGoal -> {
          val oldConfig = config ?: Config()
          val oldGoals = oldConfig.weeklyGoals ?: listOf()
          val newConfig = oldConfig.copy(weeklyGoals = oldGoals.map { 
            if (it.id == action.goal.id) action.goal else it 
          })
          newConfig.save()
          copy(config = newConfig)
        }
        is Action.SetWeeklyNotificationsEnabled -> {
          val oldConfig = config ?: Config()
          val newConfig = oldConfig.copy(weeklyNotificationsEnabled = action.enabled)
          newConfig.save()
          copy(config = newConfig)
        }
        is Action.UpdateLifeBalance -> copy(lifeBalance = action.metrics)
        is Action.CelebrateAchievement -> copy(achievements = achievements + action.achievement)
        is Action.DismissRecommendation -> copy(
          dismissedRecommendations = dismissedRecommendations + action.recommendationId,
          recommendations = recommendations.filter { it.id != action.recommendationId }
        )
        is Action.AcceptRecommendation -> {
          // Handle recommendation acceptance
          copy(dismissedRecommendations = dismissedRecommendations + action.recommendation.id)
        }
        is Action.UpdateHabitMetrics -> copy(habitMetrics = action.metrics)
        is Action.SetDarkTheme -> {
          val oldConfig = config ?: Config()
          val newConfig = oldConfig.copy(isDarkTheme = action.isDarkTheme)
          newConfig.save()
          copy(config = newConfig, isDarkTheme = action.isDarkTheme)
        }
      }

    Log.i(TAG, "Reduced $action in ${System.currentTimeMillis() - start} ms")
    nextState
  }
}

/** API heavily inspired by Redux */
object Store {

  private val stateRelay: BehaviorRelay<State> = BehaviorRelay.create()
  private val actionRelay =
    PublishRelay.create<Action>().apply {
      scan(State(), reducer).distinctUntilChanged().subscribe { stateRelay.accept(it) }
    }

  val state: State
    get() = stateRelay.value

  val observable: Observable<State>
    get() = stateRelay

  fun dispatch(action: Action) = actionRelay.accept(action)
}
