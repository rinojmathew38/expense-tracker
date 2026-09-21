package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
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
    var expenses = mutableStateListOf(
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
        private set

    val totalExpense: Double
        get() = expenses.sumOf { it.amount }

    val totalPaid: Double
        get() = expenses.filter { it.isPaid }.sumOf { it.amount }

    val remainingToPay: Double
        get() = totalExpense - totalPaid

    fun togglePaymentStatus(id: String) {
        val index = expenses.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = expenses[index]
            expenses[index] = item.copy(isPaid = !item.isPaid)
        }
    }

    fun addExpense(title: String, amount: Double, type: ExpenseType) {
        expenses.add(0, ExpenseItem(title = title, amount = amount, isPaid = (type == ExpenseType.DAILY), type = type))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ExpenseTrackerScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerScreen(vm: ExpenseViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

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
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
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
                            Text(currencyFormat.format(vm.totalExpense), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Remaining (NP)", fontSize = 13.sp, color = Color.Red.copy(alpha = 0.8f))
                            Text(
                                currencyFormat.format(vm.remainingToPay),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { if (vm.totalExpense > 0) (vm.totalPaid / vm.totalExpense).toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFF2E7D32),
                        trackColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Paid: ${currencyFormat.format(vm.totalPaid)}",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Monthly & Daily Checklist", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.expenses, key = { it.id }) { item ->
                    ExpenseRow(
                        item = item,
                        currencyFormat = currencyFormat,
                        onToggle = { vm.togglePaymentStatus(item.id) }
                    )
                }
            }
        }

        if (showDialog) {
            AddExpenseDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title, amt, type ->
                    vm.addExpense(title, amt, type)
                    showDialog = false
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
