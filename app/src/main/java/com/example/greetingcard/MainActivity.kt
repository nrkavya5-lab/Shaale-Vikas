package com.example.greetingcard

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greetingcard.viewmodel.MainViewModel
import com.example.greetingcard.model.Need as FirebaseNeed
import com.example.greetingcard.model.ImpactItem as FirebaseImpactItem
import com.example.greetingcard.model.Role as FirebaseRole
import com.example.greetingcard.model.Donor as FirebaseDonor

// ==========================================
// 1. DATA MODELS & ENUMS (Refactored to match FirebaseModels)
// ==========================================
enum class Role { None, Admin, Alumni }
enum class Screen { Landing, LoginHm, LoginAlumni, Dash, Needs, NeedDetail, Impact, Donors, Settings }

// Local aliases for easier migration if needed, but we'll use FirebaseModels directly
// ==========================================
// 2. MAIN ACTIVITY & THEME
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShaaleVikasTheme {
                ShaaleVikasApp()
            }
        }
    }
}

private val PrimaryBlue = Color(0xFF2563EB)
private val SecondaryEmerald = Color(0xFF10B981)
private val DarkBg = Color(0xFF0F172A)
private val SurfaceLight = Color(0xFFF8FAFC)

@Composable
fun ShaaleVikasTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = PrimaryBlue,
            secondary = SecondaryEmerald,
            background = DarkBg,
            surface = Color(0xFF1E293B),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = PrimaryBlue,
            secondary = SecondaryEmerald,
            background = SurfaceLight,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF1E293B)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

// ==========================================
// 3. GLOBAL APP STATE & NAVIGATION ENGINE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShaaleVikasApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var currentRole by remember { mutableStateOf(Role.None) }
    var currentScreen by remember { mutableStateOf(Screen.Landing) }
    var viewingNeedId by remember { mutableStateOf<String?>(null) }
    var isDarkTheme by remember { mutableStateOf(false) }

    val needsList by viewModel.needs.collectAsState()
    val impactList by viewModel.impactItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddNeed by remember { mutableStateOf(false) }
    var showAddImpact by remember { mutableStateOf(false) }
    var showDonate by remember { mutableStateOf(false) }

    fun showToast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    ShaaleVikasTheme(darkTheme = isDarkTheme) {
        Scaffold(
            bottomBar = {
                if (currentRole != Role.None && currentScreen !in listOf(Screen.Landing, Screen.LoginHm, Screen.LoginAlumni, Screen.NeedDetail)) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        if (currentRole == Role.Admin) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, "Dash") },
                                label = { Text("Dash", fontSize = 10.sp) },
                                selected = currentScreen == Screen.Dash,
                                onClick = { currentScreen = Screen.Dash }
                            )
                        }
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.List, "Needs") },
                            label = { Text("Needs", fontSize = 10.sp) },
                            selected = currentScreen == Screen.Needs,
                            onClick = { currentScreen = Screen.Needs }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.CheckCircle, "Impact") },
                            label = { Text("Impact", fontSize = 10.sp) },
                            selected = currentScreen == Screen.Impact,
                            onClick = { currentScreen = Screen.Impact }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Star, "Donors") },
                            label = { Text("Donors", fontSize = 10.sp) },
                            selected = currentScreen == Screen.Donors,
                            onClick = { currentScreen = Screen.Donors }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, "Settings") },
                            label = { Text("Settings", fontSize = 10.sp) },
                            selected = currentScreen == Screen.Settings,
                            onClick = { currentScreen = Screen.Settings }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentRole == Role.Admin && currentScreen == Screen.Needs) {
                    FloatingActionButton(
                        onClick = { showAddNeed = true },
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Need")
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().zIndex(1f))
                }
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } + fadeIn()) togetherWith
                                (slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } + fadeOut())
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        Screen.Landing -> LandingScreen(
                            onHmClick = { currentScreen = Screen.LoginHm },
                            onAlumniClick = { currentScreen = Screen.LoginAlumni }
                        )
                        Screen.LoginHm -> RegistrationScreen("School Registration", "Official UDISE Code", "Register School") {
                            currentRole = Role.Admin
                            currentScreen = Screen.Dash
                            showToast("Logged in as Headmaster")
                        }
                        Screen.LoginAlumni -> RegistrationScreen("Alumni Registration", "Passing Year", "Join Alumni Network") {
                            currentRole = Role.Alumni
                            currentScreen = Screen.Needs
                            showToast("Logged in as Alumni")
                        }
                        Screen.Dash -> DashScreen()
                        Screen.Needs -> NeedsScreen(
                            role = currentRole,
                            needs = needsList,
                            onNeedClick = { id ->
                                viewingNeedId = id
                                currentScreen = Screen.NeedDetail
                            }
                        )
                        Screen.NeedDetail -> {
                            val need = needsList.find { it.id == viewingNeedId }
                            if (need != null) {
                                NeedDetailScreen(
                                    need = need,
                                    role = currentRole,
                                    onBack = { currentScreen = Screen.Needs },
                                    onDonateClick = { showDonate = true }
                                )
                            }
                        }
                        Screen.Impact -> ImpactScreen(
                            role = currentRole,
                            impacts = impactList,
                            onAddImpact = { showAddImpact = true }
                        )
                        Screen.Donors -> DonorsScreen()
                        Screen.Settings -> SettingsScreen(
                            role = currentRole,
                            isDark = isDarkTheme,
                            onToggleDark = { isDarkTheme = !isDarkTheme },
                            onLogout = {
                                viewModel.logout()
                                currentRole = Role.None
                                currentScreen = Screen.Landing
                                showToast("Securely Logged Out")
                            },
                            onAction = { showToast(it) }
                        )
                    }
                }
            }
        }

        if (showAddNeed) {
            ModalBottomSheet(onDismissRequest = { showAddNeed = false }) {
                AddNeedForm(
                    onSubmit = { title, desc, cost, uri ->
                        viewModel.addNeed(title, desc, cost, uri)
                        showAddNeed = false
                        showToast("Problem Published Globally!")
                    }
                )
            }
        }

        if (showAddImpact) {
            ModalBottomSheet(onDismissRequest = { showAddImpact = false }) {
                AddImpactForm(
                    onSubmit = { title, desc, bUri, aUri ->
                        viewModel.addImpact(title, desc, bUri, aUri)
                        showAddImpact = false
                        showToast("Impact Report Published!")
                    }
                )
            }
        }

        if (showDonate) {
            val need = needsList.find { it.id == viewingNeedId }
            ModalBottomSheet(onDismissRequest = { showDonate = false }) {
                DonateForm(
                    onDonate = { amount ->
                        viewingNeedId?.let { id ->
                            viewModel.donate(id, amount)
                        }
                        showDonate = false
                        showToast("Awesome! You donated ₹$amount to ${need?.school}")
                    }
                )
            }
        }
    }
}

// ==========================================
// 4. SCREENS
// ==========================================

@Composable
fun LandingScreen(onHmClick: () -> Unit, onAlumniClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = "Logo", tint = PrimaryBlue, modifier = Modifier.size(50.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Shaale-Vikas", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "The transparent bridge connecting Rural Schools with Global Alumni.",
            fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onHmClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background)) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Login as Headmaster", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAlumniClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
            Icon(Icons.Default.Face, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Login as Alumni", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(title: String, uniqueField: String, btnText: String, onRegister: () -> Unit) {
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var field3 by remember { mutableStateOf("") }
    var field4 by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.Black) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            Text("Create your account to proceed.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 24.dp))

            OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text(uniqueField) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text("Email Address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = field4, onValueChange = { field4 = it }, label = { Text("Create Password") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                Text(btnText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun DashScreen() {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        Text("School Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = SecondaryEmerald, modifier = Modifier.size(32.dp))
                    Text("FUNDS RECEIVED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 12.dp))
                    Text("₹1.4L", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                    Text("PROBLEMS SOLVED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 12.dp))
                    Text("12", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Recent Activity", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = PrimaryBlue)
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
                    Text("Ramesh Kumar donated", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Towards Toilet Renovation", color = Color.Gray, fontSize = 12.sp)
                }
                Text("+₹5,000", fontWeight = FontWeight.Bold, color = SecondaryEmerald)
            }
        }
    }
}

import coil.compose.AsyncImage

@Composable
fun NeedsScreen(role: Role, needs: List<FirebaseNeed>, onNeedClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredNeeds = if (searchQuery.isBlank()) needs else needs.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.school.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(20.dp))
        if (role == Role.Alumni) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search schools or problems...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("Global School Needs", fontSize = 20.sp, fontWeight = FontWeight.Black)
        } else {
            Text("Your Active Requests", fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            items(filteredNeeds) { need ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNeedClick(need.id) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                            if (need.imageUrl != null) {
                                AsyncImage(
                                    model = need.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(PrimaryBlue.copy(alpha=0.5f), PrimaryBlue))))
                            }

                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
                            if (need.urgent) {
                                CustomBgText("URGENT", Color.White, Color.Red, FontWeight.Black, 10.sp, modifier = Modifier.padding(16.dp))
                            }
                            Text(need.school, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
                        }
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(need.title, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(need.desc, color = Color.Gray, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))

                            Spacer(modifier = Modifier.height(16.dp))
                            val progress = if (need.target > 0) need.collected.toFloat() / need.target else 0f
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${(progress * 100).toInt()}% Funded", color = SecondaryEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("₹${need.target}", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = SecondaryEmerald,
                                trackColor = Color.LightGray.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun NeedDetailScreen(need: FirebaseNeed, role: Role, onBack: () -> Unit, onDonateClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            if (need.imageUrl != null) {
                AsyncImage(
                    model = need.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(PrimaryBlue.copy(alpha=0.5f), PrimaryBlue))))
            }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                if (need.urgent) {
                    CustomBgText("URGENT", Color.White, Color.Red, FontWeight.Black, 10.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(need.title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(need.school, color = Color.LightGray, fontSize = 14.sp)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().offset(y = (-20).dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(MaterialTheme.colorScheme.background).padding(24.dp).verticalScroll(rememberScrollState())) {
            Text("Problem Description", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(need.desc, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp), lineHeight = 20.sp)

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Need more details?", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 14.sp)
                        Text("Contact School directly for queries.", color = Color.Gray, fontSize = 12.sp)
                    }
                    IconButton(onClick = { /* Launch Dial Intent */ }, modifier = Modifier.background(Color.White, CircleShape)) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = PrimaryBlue)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("FUNDING PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("₹${need.collected}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = SecondaryEmerald)
                            Text("Raised so far", fontSize = 12.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${need.target}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("Goal Amount", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    val progress = if (need.target > 0) need.collected.toFloat() / need.target else 0f
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = SecondaryEmerald,
                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                    )
                }
            }

            if (role == Role.Alumni) {
                Button(onClick = onDonateClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                    Text("Donate to this Cause", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ImpactScreen(role: Role, impacts: List<FirebaseImpactItem>, onAddImpact: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Global Impact", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
            if (role == Role.Admin) {
                Button(onClick = onAddImpact, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Impact", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxSize()) {
            items(impacts) { impact ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(impact.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                        Text("Completed • ${impact.school}", color = SecondaryEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(16.dp))) {
                                if (impact.beforeUrl != null) {
                                    AsyncImage(model = impact.beforeUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
                                }
                                CustomBgText("BEFORE", Color.White, Color.Black.copy(alpha=0.6f), FontWeight.Black, 10.sp, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
                            }
                            Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(16.dp))) {
                                if (impact.afterUrl != null) {
                                    AsyncImage(model = impact.afterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(SecondaryEmerald))
                                }
                                CustomBgText("AFTER", Color.White, SecondaryEmerald, FontWeight.Black, 10.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
                            }
                        }
                        Text(impact.desc, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp).background(Color.LightGray.copy(alpha=0.1f), RoundedCornerShape(12.dp)).padding(12.dp))
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DonorsScreen() {
    val donors = listOf(
        Donor("Ramesh Kumar", "Supported GHS Malgudi", "₹45k", 1),
        Donor("Priya Sharma", "Supported ZPHS Hubli", "₹30k", 2),
        Donor("Anonymous", "Supported Hassan Primary", "Items", 3)
    )

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF97316)))).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White.copy(alpha=0.8f), modifier = Modifier.size(48.dp))
                Text("Hall of Fame", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("Honoring global supporters", fontSize = 14.sp, color = Color.White.copy(alpha=0.9f))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column {
                donors.forEach { donor ->
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(donor.rank.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = if(donor.rank == 1) Color(0xFFF59E0B) else Color.Gray, modifier = Modifier.width(30.dp))
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.LightGray.copy(alpha=0.3f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                        }
                        Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
                            Text(donor.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(donor.supported, color = Color.Gray, fontSize = 12.sp)
                        }
                        Surface(color = SecondaryEmerald.copy(alpha=0.1f), shape = RoundedCornerShape(8.dp)) {
                            Text(donor.amountStr, color = SecondaryEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                    if (donor.rank < 3) HorizontalDivider(color = Color.LightGray.copy(alpha=0.2f))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(role: Role, isDark: Boolean, onToggleDark: () -> Unit, onLogout: () -> Unit, onAction: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("Account Settings", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))

        Text("PROFILE INFORMATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(30.dp))
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(if (role == Role.Admin) "Ramesh K." else "Priya Sharma", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(if (role == Role.Admin) "Headmaster • GHS Malgudi" else "Alumni • IT Professional", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha=0.2f))
                SettingsRow(icon = Icons.Default.Edit, title = "Edit Profile", onClick = { onAction("Edit Profile Opened") })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("APP PREFERENCES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column {
                SettingsSwitchRow(icon = Icons.Default.Settings, title = "Dark Mode", checked = isDark, onCheckedChange = { onToggleDark() })
                HorizontalDivider(color = Color.LightGray.copy(alpha=0.2f))
                SettingsSwitchRow(icon = Icons.Default.Notifications, title = "Allow Notifications", checked = true, onCheckedChange = { onAction("Notifications Updated") })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("SUPPORT & SECURITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column {
                SettingsRow(icon = Icons.Default.Lock, title = "Change Password", onClick = { onAction("Password reset link sent") })
                HorizontalDivider(color = Color.LightGray.copy(alpha=0.2f))
                SettingsRow(icon = Icons.Default.Call, title = "Customer Support", onClick = { onAction("Connecting to Support Center...") })
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.1f), contentColor = Color.Red)) {
            Text("Secure Logout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Text(title, modifier = Modifier.padding(start = 16.dp).weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
    }
}

@Composable
fun SettingsSwitchRow(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Text(title, modifier = Modifier.padding(start = 16.dp).weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue, checkedTrackColor = PrimaryBlue.copy(alpha=0.3f)))
    }
}

// ==========================================
// 5. MODALS / FORMS
// ==========================================

@Composable
fun AddNeedForm(onSubmit: (String, String, Int, Uri?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> selectedUri = uri }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp)) {
        Text("Post School Need", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp)).background(PrimaryBlue.copy(alpha=0.1f)).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
            if (selectedUri != null) {
                AsyncImage(model = selectedUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
                    Text("Tap to upload photo", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Problem Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Detailed Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Funds Needed (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { onSubmit(title, desc, cost.toIntOrNull() ?: 0, selectedUri) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
            Text("Publish to Alumni", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun AddImpactForm(onSubmit: (String, String, Uri?, Uri?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var beforeUri by remember { mutableStateOf<Uri?>(null) }
    var afterUri by remember { mutableStateOf<Uri?>(null) }

    val beforeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> beforeUri = uri }
    val afterLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> afterUri = uri }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp)) {
        Text("Post Impact Report", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(16.dp)).background(Color.LightGray.copy(alpha=0.3f)).clickable { beforeLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                if (beforeUri != null) AsyncImage(model = beforeUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                    Text("BEFORE", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(16.dp)).background(SecondaryEmerald.copy(alpha=0.1f)).clickable { afterLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                if (afterUri != null) AsyncImage(model = afterUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = SecondaryEmerald)
                    Text("AFTER", color = SecondaryEmerald, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Project Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Impact Details") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { onSubmit(title, desc, beforeUri, afterUri) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)) {
            Text("Publish Impact", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun DonateForm(onDonate: (Int) -> Unit) {
    var amount by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp)) {
        Text("Donate Funds", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Enter the amount you wish to donate. Every rupee helps!", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top=8.dp, bottom=24.dp))

        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { onDonate(amount.toIntOrNull() ?: 0) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background)) {
            Text("Confirm Donation", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

// ==========================================
// 6. UTILITIES
// ==========================================

@Composable
fun CustomBgText(text: String, color: Color, backgroundColor: Color, fontWeight: FontWeight, fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    Surface(color = backgroundColor, shape = RoundedCornerShape(4.dp), modifier = modifier) {
        androidx.compose.material3.Text(
            text = text,
            color = color,
            fontWeight = fontWeight,
            fontSize = fontSize,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ==========================================
// 7. PREVIEWS (For Android Studio)
// ==========================================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewApp() { ShaaleVikasTheme { ShaaleVikasApp() } }

@Preview(showBackground = true)
@Composable
fun PreviewLanding() { ShaaleVikasTheme { LandingScreen({}, {}) } }

@Preview(showBackground = true)
@Composable
fun PreviewDash() { ShaaleVikasTheme { DashScreen() } }

@Preview(showBackground = true)
@Composable
fun PreviewNeeds() {
    ShaaleVikasTheme {
        NeedsScreen(Role.Alumni, listOf(Need(1, "School Name", "Sample Title", "Sample Description", 10000, 5000, true, "123")), {})
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewImpact() {
    ShaaleVikasTheme {
        ImpactScreen(Role.Admin, listOf(ImpactItem(1, "Repaired Desks", "ZPHS", "Done")), {})
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettings() {
    ShaaleVikasTheme { SettingsScreen(Role.Alumni, false, {}, {}, {}) }
}