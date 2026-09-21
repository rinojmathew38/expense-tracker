package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

enum class ExpenseType { MONTHLY_FIXED, DAILY }

data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val isPaid: Boolean = false,
    val type: ExpenseType = ExpenseType.MONTHLY_FIXED
)

class ExpenseViewModel : ViewModel() {
    private val _monthlyData = mutableStateMapOf<String, MutableList<ExpenseItem>>()
    var selectedCalendar by mutableStateOf(Calendar.getInstance())
        private set

    private fun getMonthKey(cal: Calendar): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
    }

    val currentMonthKey: String
        get() = getMonthKey(selectedCalendar)

    val currentMonthDisplay: String
        get() = SimpleDateFormat("MMMM yyyy", Locale.US).format(selectedCalendar.time)

    fun getExpensesForCurrentMonth(): List<ExpenseItem> {
        if (!_monthlyData.containsKey(currentMonthKey)) {
            _monthlyData[currentMonthKey] = defaultMonthlyChecklist()
        }
        return _monthlyData[currentMonthKey] ?: emptyList()
    }

    private fun defaultMonthlyChecklist(): MutableList<ExpenseItem> {
        return mutableListOf(
            ExpenseItem(title = "NEDC", amount = 10000.0),
            ExpenseItem(title = "PL", amount = 8628.0),
            ExpenseItem(title = "NI", amount = 8000.0),
            ExpenseItem(title = "RR", amount = 9000.0),
            ExpenseItem(title = "MF", amount = 3000.0),
            ExpenseItem(title = "EXD", amount = 3000.0),
            ExpenseItem(title = "SSA", amount = 3000.0),
            ExpenseItem(title = "CC", amount = 12816.0),
            ExpenseItem(title = "CB", amount = 300.0),
            ExpenseItem(title = "EEXP", amount = 300.0),
            ExpenseItem(title = "EDEXP", amount = 2500.0),
            ExpenseItem(title = "MLK", amount = 2000.0),
            ExpenseItem(title = "Ed Cable", amount = 250.0),
            ExpenseItem(title = "Bus Fee", amount = 2000.0)
        )
    }

    fun nextMonth() {
        val newCal = selectedCalendar.clone() as Calendar
        newCal.add(Calendar.MONTH, 1)
        selectedCalendar = newCal
    }

    fun previousMonth() {
        val newCal = selectedCalendar.clone() as Calendar
        newCal.add(Calendar.MONTH, -1)
        selectedCalendar = newCal
    }

    fun setMonthAndYear(year: Int, monthZeroIndexed: Int) {
        val newCal = Calendar.getInstance()
        newCal.set(Calendar.YEAR, year)
        newCal.set(Calendar.MONTH, monthZeroIndexed)
        selectedCalendar = newCal
    }

    fun togglePaymentStatus(id: String) {
        val list = _monthlyData[currentMonthKey] ?: return
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = list[index]
            list[index] = item.copy(isPaid = !item.isPaid)
        }
    }

    fun addExpense(title: String, amount: Double, type: ExpenseType) {
        val list = _monthlyData.getOrPut(currentMonthKey) { defaultMonthlyChecklist() }
        list.add(0, ExpenseItem(title = title, amount = amount, isPaid = (type == ExpenseType.DAILY), type = type))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var showSplash by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        delay(1800)
                        showSplash = false
                    }

                    if (showSplash) {
                        SplashScreen()
                    } else {
                        ExpenseTrackerScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F4C3A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Expense Tracker",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Track & Save Easily",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerScreen(vm: ExpenseViewModel = viewModel()) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val expenses = vm.getExpensesForCurrentMonth()
    val totalExpense = expenses.sumOf { it.amount }
    val totalPaid = expenses.filter { it.isPaid }.sumOf { it.amount }
    val remainingToPay = totalExpense - totalPaid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Tracker", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Month & Year Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.previousMonth() }) {
                        Text("<", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = vm.currentMonthDisplay,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMonthPicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    IconButton(onClick = { vm.nextMonth() }) {
                        Text(">", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Budget Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Budget", fontSize = 13.sp, color = Color.Gray)
                            Text(currencyFormat.format(totalExpense), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Remaining (NP)", fontSize = 13.sp, color = Color.Red.copy(alpha = 0.8f))
                            Text(
                                currencyFormat.format(remainingToPay),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { if (totalExpense > 0) (totalPaid / totalExpense).toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF2E7D32),
                        trackColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Paid: ${currencyFormat.format(totalPaid)}",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Monthly & Daily Checklist", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            // Expense Items
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses, key = { it.id }) { item ->
                    ExpenseRow(
                        item = item,
                        currencyFormat = currencyFormat,
                        onToggle = { vm.togglePaymentStatus(item.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddExpenseDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, amt, type ->
                    vm.addExpense(title, amt, type)
                    showAddDialog = false
                }
            )
        }

        if (showMonthPicker) {
            MonthYearPickerDialog(
                currentCalendar = vm.selectedCalendar,
                onDismiss = { showMonthPicker = false },
                onSelect = { year, month ->
                    vm.setMonthAndYear(year, month)
                    showMonthPicker = false
                }
            )
        }
    }
}

@Composable
fun ExpenseRow(item: ExpenseItem, currencyFormat: NumberFormat, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPaid) Color(0xFFF1F8E9) else Color(0xFFFFF3E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(currencyFormat.format(item.amount), fontSize = 14.sp, color = Color.DarkGray)
            }

            OutlinedButton(
                onClick = onToggle,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (item.isPaid) Color(0xFF2E7D32) else Color(0xFFE65100),
                    contentColor = Color.White
                )
            ) {
                Text(text = if (item.isPaid) "PAID" else "NP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    currentCalendar: Calendar,
    onDismiss: () -> Unit,
    onSelect: (Int, Int) -> Unit
) {
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    var selectedYear by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(currentCalendar.get(Calendar.MONTH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month & Year") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) { Text("<", fontSize = 20.sp) }
                    Text("$selectedYear", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { selectedYear++ }) { Text(">", fontSize = 20.sp) }
                }

                // Month selection grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 3) {
                                val monthIndex = row * 3 + col
                                val isSelected = monthIndex == selectedMonth
                                OutlinedButton(
                                    onClick = { selectedMonth = monthIndex },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(months[monthIndex].take(3), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(selectedYear, selectedMonth) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddExpenseDialog(onDismiss: () -> Unit, onConfirm: (String, Double, ExpenseType) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isDaily by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDaily, onCheckedChange = { isDaily = it })
                    Text("Daily expense (Paid)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && parsedAmount > 0) {
                        val type = if (isDaily) ExpenseType.DAILY else ExpenseType.MONTHLY_FIXED
                        onConfirm(title, parsedAmount, type)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

