package com.example.tutoring.ui.screens.tutor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.tutoring.network.ApiService
import com.example.tutoring.network.NetworkClient
import com.example.tutoring.ui.screens.tutor.common.CourseCardTutor
import com.example.tutoring.utils.ErrorNotifier
import com.example.tutoring.utils.LoadingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(navController: NavHostController, loadingViewModel: LoadingViewModel = viewModel()) {
    if (loadingViewModel.isHttpLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    var page by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var courses by remember { mutableStateOf(listOf<CourseRegistration>()) }
    var allCourses by remember { mutableStateOf(listOf<CourseRegistration>()) }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val apiService = NetworkClient.createService(ApiService::class.java)
    val pageSize = 10
    fun getAllCourses(){
        scope.launch {
            loadingViewModel.setLoading(true)
            try {
                val response = apiService.listTutorCourses()
                allCourses = (response.data as List<CourseRegistration>).map { course ->
                    course.copy(registrationId = 0) // set default value to match the type
                }
                courses = allCourses.take(pageSize)
                page++
                loadingViewModel.setLoading(false)
            } catch (e: Exception) {
                loadingViewModel.setLoading(false)
            }
        }
    }
    fun loadCourses() {
        if (isLoading) return
        isLoading = true
        scope.launch {
            delay(1000)
            val startIndex = (page - 1) * pageSize
            val endIndex = minOf(startIndex + pageSize, allCourses.size)
            val newCourses = if (startIndex < allCourses.size) {
                allCourses.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
            courses = courses + newCourses
            page++
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        getAllCourses()
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleItemIndex ->
                if (!isLoading && lastVisibleItemIndex == courses.lastIndex) {
                    loadCourses()
                }
            }
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var handleType by remember { mutableStateOf("") }
    var courseName by remember { mutableStateOf("") }
    var courseDescription by remember { mutableStateOf("") }
    var courseSubject by remember { mutableStateOf("") }
    var wannaUpdateCourseId by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Button(
            onClick = {
                showAddDialog = true
                handleType = "add"
           },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Course")
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(text = if (handleType == "add") "Add Course" else "Update Course") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = courseName,
                            onValueChange = { courseName = it },
                            label = { Text("Course Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = courseDescription,
                            onValueChange = { courseDescription = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = courseSubject,
                            onValueChange = { courseSubject = it },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val requestBody = mapOf(
                                    "name" to courseName,
                                    "description" to courseDescription,
                                    "subject" to courseSubject
                                )
                                try {
                                    if(handleType=="add"){
                                        val response = apiService.createCourse(requestBody)
                                        ErrorNotifier.showSuccess("Add course successful!")
                                    }else if(handleType=="update"){
                                        val response = apiService.updateCourse(requestBody, wannaUpdateCourseId)
                                        ErrorNotifier.showSuccess("Add course successful!")
                                    }

                                    showAddDialog = false
                                    // clear form
                                    courseName = ""
                                    courseDescription = ""
                                    courseSubject = ""
                                    // Refresh the course list if needed
                                    getAllCourses()
                                } catch (e: Exception) {
                                    ErrorNotifier.showError(e.message ?: "Failed")
                                }
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(courses.size) { index ->
                val course = courses[index]
                CourseCardTutor(
                    cardType = "courses",
                    course = course,
                    onConfirmClick = {},
                    navController = navController,
                    onDelete = {
                        scope.launch {
                            try {
                                val response = apiService.deleteCourse(course.courseId)
                                ErrorNotifier.showSuccess("Delete successful!")
                                getAllCourses()
                            } catch (e: Exception) {
                                ErrorNotifier.showError(e.message ?: "Failed")
                            }
                        }
                    },
                    onUpdate = { course ->
                        showAddDialog = true
                        handleType = "update"
                        wannaUpdateCourseId = course.courseId
                        courseName = course.courseName
                        courseDescription = course.description
                        courseSubject = course.subject
                    }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

    }
}
