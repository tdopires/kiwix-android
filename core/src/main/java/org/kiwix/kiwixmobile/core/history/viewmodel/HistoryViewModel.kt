package org.kiwix.kiwixmobile.core.history.viewmodel

import Finish
import OpenHistoryItem
import ShowToast
import StartSpeechInput
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function5
import io.reactivex.functions.Function3
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ConfirmedDelete
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.CreatedWithIntent
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.DeleteHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitHistory
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.StartSpeechInputFailed
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ToggleShowAllHistoryAvailability
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ToggleShowHistoryFromAllBooks
import org.kiwix.kiwixmobile.core.history.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.history.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.ToggleShowAllHistorySwitchAvailability
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
  private val historyDao: HistoryDao,
  private val zimReaderContainer: ZimReaderContainer
) : ViewModel() {
  val state = MutableLiveData<State>().apply { value = NoResults("", listOf()) }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val filter = BehaviorProcessor.createDefault("")
  private val compositeDisposable = CompositeDisposable()
  private val currentBook = BehaviorProcessor.createDefault("")
  private val showAllSwitchToggle = BehaviorProcessor.createDefault(false)
  private val unselectAllItems = BehaviorProcessor.createDefault(false)
  private var isInSelectionMode = false

  init {
    compositeDisposable.addAll(viewStateReducer(), actionMapper(), updateSelectionMode())
  }

  private fun updateSelectionMode() =
    unselectAllItems.subscribe({ isInSelectionMode = it }, Throwable::printStackTrace)

  private fun viewStateReducer() = Flowable.combineLatest(
    currentBook,
    searchResults(),
    unselectAllItems,
    filter,
    showAllSwitchToggle,
    Function5(::updateResultsState)
  )
    .subscribe(state::postValue, Throwable::printStackTrace)

  private fun searchResults(): Flowable<List<HistoryListItem>> =
    Flowable.combineLatest(
      filter,
      showAllSwitchToggle,
      historyDao.history(),
      Function3(::searchResults)
    )

  private fun searchResults(
    searchString: String,
    showAllToggle: Boolean,
    historyList: List<HistoryListItem>
  ): List<HistoryListItem> =
    historyList.filterIsInstance<HistoryItem>()
      .filter { h ->
        h.historyTitle.contains(searchString, true) &&
          (h.zimName == zimReaderContainer.name || showAllToggle)
      }

  private fun updateResultsState(
    currentBook: String,
    historyBookResults: List<HistoryListItem>,
    unselectAllItems: Boolean,
    searchString: String,
    showAllSwitchOn: Boolean
  ): State {
    if(unselectAllItems){
      historyBookResults.filterIsInstance<HistoryItem>().forEach { it.isSelected = false }
    }
    return Results(searchString, historyBookResults, showAllSwitchOn, currentBook)
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }

  private fun actionMapper() = actions.map {
    when (it) {
      ExitHistory -> effects.offer(Finish)
      is Filter -> filter.offer(it.searchTerm)
      is ToggleShowHistoryFromAllBooks -> showAllSwitchToggle.offer(it.isChecked)
      is ToggleShowAllHistoryAvailability ->
        effects.offer(ToggleShowAllHistorySwitchAvailability(it.showAllHistorySwitch))
      is CreatedWithIntent -> filter.offer(it.searchTerm)
      is ConfirmedDelete -> deleteItemAndShowToast(it)
      is OnItemLongClick -> selectItemAndOpenSelectionMode(it.historyItem)
      is OnItemClick -> appendItemToSelectionOrOpenIt(it)
      is DeleteHistoryItems -> historyDao.deleteHistory(it.itemsToDelete)
      ReceivedPromptForSpeechInput -> effects.offer(StartSpeechInput(actions))
      ExitActionModeMenu -> unselectAllItems.offer(true)
      StartSpeechInputFailed -> effects.offer(ShowToast(R.string.speech_not_supported))
    }
  }.subscribe({}, Throwable::printStackTrace)


  private fun selectItemAndOpenSelectionMode(historyItem: HistoryItem) {
    historyItem.isSelected = true
    unselectAllItems.offer(false)
  }

  private fun isInSelctionMode(): Boolean {
    return state
      .value
      ?.historyItems
      ?.filterIsInstance<HistoryItem>()
      ?.filter { it.isSelected }
      ?.isNotEmpty() ?: false
  }


  private fun deselectAllHistoryItems(){


  }

  private fun appendItemToSelectionOrOpenIt(onItemClick: OnItemClick) {
    val historyItem = onItemClick.historyListItem as HistoryItem
    when {
      historyItem.isSelected -> {
        historyItem.isSelected = false
        unselectAllItems.offer(false)
      }
      isInSelctionMode() -> {
        historyItem.isSelected = true
        unselectAllItems.offer(false)
      }
      else -> {
        effects.offer(OpenHistoryItem(historyItem, zimReaderContainer))
      }
    }
  }

  private fun deleteItemAndShowToast(it: ConfirmedDelete) {
  }
}
