package com.ktomek.yamv.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ktomek.yamv.hilt.hiltMviStore
import com.ktomek.yamv.profile.logic.SaveProfileIntention
import com.ktomek.yamv.profile.logic.UpdateEmailIntention
import com.ktomek.yamv.profile.logic.UpdateNameIntention
import com.ktomek.yamv.profile.logic.state.ProfileState
import com.ktomek.yamv.profile.logic.state.ProfileStateStore
import com.ktomek.yamv.profile.logic.uistate.ProfileUiState
import com.ktomek.yamv.state.uiState

/**
 * Demonstrates State -> UiState projection.
 *
 * Edit the fields and press Save. The two counters make the benefit visible:
 * pressing Save churns internal bookkeeping (`saveAttempts`) several times, so the
 * raw-state probe recomposes on every tick, while the projected form recomposes
 * only for the two `isSaving` transitions the UI actually cares about.
 */
@Composable
fun ProfileScreen(
    store: ProfileStateStore = hiltMviStore(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GAP),
        verticalArrangement = Arrangement.spacedBy(GAP),
    ) {
        Text("Edit your profile, then press Save and watch the two counters below.")
        ProjectedProfileForm(store)
        Spacer(Modifier.height(GAP))
        RawStateProbe(store)
    }
}

/** Collects the UiState projection — recomposes only when the projected values change. */
@Composable
private fun ProjectedProfileForm(store: ProfileStateStore) {
    val scope = rememberCoroutineScope()
    val ui by remember(store, scope) {
        store.uiState<ProfileState, ProfileUiState>(scope)
    }.collectAsState()
    val recompositions = countRecompositions()

    OutlinedTextField(
        value = ui.name,
        onValueChange = { store.dispatch(UpdateNameIntention(it)) },
        label = { Text("Name") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = ui.email,
        onValueChange = { store.dispatch(UpdateEmailIntention(it)) },
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { store.dispatch(SaveProfileIntention) },
        enabled = ui.canSave,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (ui.isSaving) "Saving…" else "Save")
    }
    Text("Projected (uiState) recompositions: $recompositions")
}

/** Collects the FULL state — recomposes on every emission, including internal bookkeeping churn. */
@Composable
private fun RawStateProbe(store: ProfileStateStore) {
    val state by store.state.collectAsState()
    val recompositions = countRecompositions()
    Text("Raw (store.state) recompositions: $recompositions")
    Text("Internal saveAttempts (hidden from UiState): ${state.saveAttempts}")
}

/** Plain (non-snapshot) recomposition counter — increments once per recomposition. */
@Composable
private fun countRecompositions(): Int {
    val counter = remember { intArrayOf(0) }
    counter[0]++
    return counter[0]
}

private val GAP = 16.dp
