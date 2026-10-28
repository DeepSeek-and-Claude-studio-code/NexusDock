package com.nexus.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.app.role.RoleManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val contacts = mutableStateListOf<PhoneContact>()
    private val recents = mutableStateListOf<RecentCall>()
    private var favoriteNumbers by mutableStateOf<Set<String>>(emptySet())
    private var requestedNumber by mutableStateOf("")

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { loadPhoneData() }

    private val roleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedNumber = intent?.data?.schemeSpecificPart.orEmpty()
        favoriteNumbers = getSharedPreferences("nexus_phone", Context.MODE_PRIVATE)
            .getStringSet("favorites", emptySet())?.toSet() ?: emptySet()
        permissions.launch(arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE
        ))
        setContent {
            NexusTheme {
                NexusPhoneApp(
                    contacts = contacts,
                    recents = recents,
                    favorites = favoriteNumbers,
                    initialNumber = requestedNumber,
                    onDial = ::placeCall,
                    onMakeDefault = ::requestDefaultDialer,
                    onToggleFavorite = ::toggleFavorite
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadPhoneData()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedNumber = intent.data?.schemeSpecificPart.orEmpty()
    }

    private fun loadPhoneData() {
        contacts.clear()
        recents.clear()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            contacts.addAll(PhoneRepository.loadContacts(this))
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            recents.addAll(PhoneRepository.loadRecents(this))
        }
    }

    private fun toggleFavorite(number: String) {
        favoriteNumbers = if (number in favoriteNumbers) favoriteNumbers - number else favoriteNumbers + number
        getSharedPreferences("nexus_phone", Context.MODE_PRIVATE).edit()
            .putStringSet("favorites", favoriteNumbers.toSet()).apply()
    }

    private fun requestDefaultDialer() {
        val manager = getSystemService(RoleManager::class.java) ?: return
        if (manager.isRoleAvailable(RoleManager.ROLE_DIALER) && !manager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            roleRequest.launch(manager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        }
    }

    private fun placeCall(number: String) {
        val clean = number.trim()
        if (clean.isEmpty()) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissions.launch(arrayOf(Manifest.permission.CALL_PHONE))
            return
        }
        runCatching {
            startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", clean, null)))
        }.onFailure {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", clean, null)))
        }
    }
}

@Composable
private fun NexusTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF5266A9),
        secondary = androidx.compose.ui.graphics.Color(0xFF596078),
        background = androidx.compose.ui.graphics.Color(0xFFF7F6FC),
        surface = androidx.compose.ui.graphics.Color(0xFFF7F6FC)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NexusPhoneApp(
    contacts: List<PhoneContact>,
    recents: List<RecentCall>,
    favorites: Set<String>,
    initialNumber: String,
    onDial: (String) -> Unit,
    onMakeDefault: () -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf(initialNumber) }
    LaunchedEffect(initialNumber) {
        if (initialNumber.isNotBlank()) {
            number = initialNumber
            tab = 2
        }
    }
    val filteredContacts = contacts.filter {
        (it.name + " " + it.number).contains(search, ignoreCase = true)
    }
    val filteredRecents = recents.filter {
        (it.name + " " + it.number).contains(search, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nexus Phone", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(onClick = onMakeDefault) { Text("По умолчанию") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Contacts, contentDescription = null) },
                    label = { Text("Контакты") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Недавние") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Dialpad, contentDescription = null) },
                    label = { Text("Клавиатура") }
                )
                NavigationBarItem(
                    selected = tab == 3, onClick = { tab = 3 },
                    icon = { Text("★") },
                    label = { Text("Избранное") }
                )
            }
        }
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp)
        ) {
            if (tab != 2) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                    singleLine = true,
                    label = { Text("Поиск по имени или номеру") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            }
            when (tab) {
                0 -> ContactList(filteredContacts, favorites, onDial, onToggleFavorite)
                1 -> RecentList(filteredRecents, onDial)
                2 -> DialPad(number, { number = it }, onDial)
                else -> ContactList(
                    filteredContacts.filter { it.number in favorites },
                    favorites, onDial, onToggleFavorite,
                    emptyMessage = "Избранные контакты появятся здесь"
                )
            }
        }
    }
}

@Composable
private fun ContactList(
    contacts: List<PhoneContact>,
    favorites: Set<String>,
    onDial: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    emptyMessage: String = "Контакты не найдены"
) {
    if (contacts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(contacts, key = { it.number }) { contact ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(contact.name.ifBlank { contact.number }, fontWeight = FontWeight.Medium)
                            if (contact.name.isNotBlank()) {
                                Text(contact.number, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            if (contact.number in favorites) "★" else "☆",
                            modifier = Modifier.clickable { onToggleFavorite(contact.number) }.padding(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { onDial(contact.number) }) {
                            Icon(Icons.Default.Call, contentDescription = "Позвонить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentList(calls: List<RecentCall>, onDial: (String) -> Unit) {
    if (calls.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("История звонков появится после выдачи разрешения")
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(calls, key = { "${it.date}-${it.number}" }) { call ->
                ListItem(
                    headlineContent = { Text(call.name.ifBlank { call.number }) },
                    supportingContent = { Text("${call.number} · ${call.timeLabel}") },
                    trailingContent = {
                        IconButton(onClick = { onDial(call.number) }) {
                            Icon(Icons.Default.Call, contentDescription = "Позвонить")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DialPad(number: String, onNumberChange: (String) -> Unit, onDial: (String) -> Unit) {
    OutlinedTextField(
        value = number, onValueChange = onNumberChange,
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 16.dp),
        singleLine = true, label = { Text("Номер телефона") }
    )
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(keys.size) { index ->
            FilledTonalButton(
                onClick = { onNumberChange(number + keys[index]) },
                modifier = Modifier.padding(5.dp).height(58.dp)
            ) { Text(keys[index], style = MaterialTheme.typography.titleLarge) }
        }
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { if (number.isNotEmpty()) onNumberChange(number.dropLast(1)) }, modifier = Modifier.weight(1f)) {
            Text("⌫")
        }
        Button(onClick = { onDial(number) }, enabled = number.isNotBlank(), modifier = Modifier.weight(2f)) {
            Icon(Icons.Default.Call, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Позвонить")
        }
    }
}
