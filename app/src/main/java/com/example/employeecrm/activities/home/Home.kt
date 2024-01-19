package com.example.employeecrm.activities.home

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.Toolbar;

import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.employeecrm.APIServices.Apis
import com.example.employeecrm.R
import com.example.employeecrm.activities.admin.AdminDashboard
import com.example.employeecrm.activities.admin.employee.EmployeeList
import com.example.employeecrm.activities.project.Projects
import com.example.employeecrm.databinding.ActivityHomeBinding
import com.example.employeecrm.model.Employee
import com.example.employeecrm.model.LoginManager
import com.example.employeecrm.model.LoginResponse
import com.example.employeecrm.model.Project
import com.example.employeecrm.model.ProjectRequest
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Home : AppCompatActivity() {
//    for bind the data
    private lateinit var binding: ActivityHomeBinding
    //for storing the token
    private lateinit var token: String
//    for employee details
    private val employeeDetails: MutableList<Employee> = mutableListOf()
    // for all project
    private val allProject: MutableList<Project> = mutableListOf()
    //for completed project
    private val completedProject : MutableList<Project> = mutableListOf()
    //for ongoing project
    private val ongoingProject : MutableList<Project> = mutableListOf()
    // for select manager
    private val managers : MutableList<Employee> = mutableListOf()
    //priority
    val priorities = arrayOf("Low", "Medium", "High") // Array of priorities
    val teams = arrayOf("App Development", "Web Development", "Design") // Array of team

    var selectedManagerName = ""
    var selectedPriority = ""
    var selectedTeam =""


    // Base URL of your API
    private val BASE_URL = "http://192.168.26.40:4000/"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //support actionbar
        setUpActionBar()

        val mainContent = findViewById<View>(R.id.main_content)
        val btnCreateNew = mainContent.findViewById<FloatingActionButton>(R.id.btn_create_project)
        val createProjectFormLayout = mainContent.findViewById<LinearLayout>(R.id.ll_create_project)
        val newSubmit =  mainContent.findViewById<Button>(R.id.btn_new_project_submit)

        btnCreateNew.setOnClickListener {
            createProjectFormLayout.visibility = View.VISIBLE
            btnCreateNew.visibility = View.GONE
        }

        //create new project
        newSubmit.setOnClickListener {
            val projectName = mainContent.findViewById<EditText>(R.id.etProjectName).text.trim().toString()
            val des = mainContent.findViewById<EditText>(R.id.etProjectDescription).text.trim().toString()
            val startDate = mainContent.findViewById<EditText>(R.id.etProjectStartDate).text.trim().toString()
            val submissionDate = mainContent.findViewById<EditText>(R.id.etSubmissionDate).text.trim().toString()
            val websiteUrl = mainContent.findViewById<EditText>(R.id.etWebsiteUrl).text.trim().toString()



            Log.d("selected", "$selectedTeam, $selectedManagerName, $selectedPriority")
            Log.d("selected", "$projectName, $des, $startDate, $submissionDate")

            createProjectFormLayout.visibility = View.GONE
            btnCreateNew.visibility = View.VISIBLE

//            invoke to send the project of employee cem
            sendCreateNewProject(projectName, des, startDate, submissionDate, websiteUrl)
        }


        // Checking if a login response is stored and accessing its properties
        val storedLoginResponse = LoginManager.loginResponse

        if (storedLoginResponse != null) {
            token = storedLoginResponse.token
            Log.d("response result", token)
        }

        updateNavigationUserDetails(storedLoginResponse)


//        navigation bar
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.admin_dashboard -> {
                    startActivity(Intent(this, AdminDashboard::class.java))
                    // Optionally, add logic here after starting the activity for the selected item
                }
                R.id.project ->{
                    startActivity(Intent(this, Projects::class.java))
                }
                R.id.employee ->{
                    startActivity(Intent(this, EmployeeList::class.java))
                }
                else -> {
                    // Handle other menu item clicks here if needed
                }
            }
            findViewById<DrawerLayout>(R.id.drawerLayout).closeDrawer(GravityCompat.START)
            true // Return true to indicate the item is selected
        }

        //get profile
        getProfile()

//        get employee details
        getEmployee()

//        invoke project
        getProjects()

        setSpinner()
    }

//    handle for create new project
    private fun sendCreateNewProject(
    projectName: String,
    des: String,
    startDate: String,
    submissionDate: String,
    websiteUrl: String
) {
            val retrofit =  Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiServices = retrofit.create(Apis::class.java)


            lifecycleScope.launch {
                try {
                    val response = apiServices.newProject( ProjectRequest(projectName, des, startDate, submissionDate, selectedManagerName.lowercase(), selectedPriority.lowercase(), selectedTeam.lowercase(), websiteUrl, false, false), "token=$token")
                    if (response.isSuccessful) {
                        val success = response.body()
                        if (success != null) {
                            // Handle successful login response
                            startActivity(Intent(this@Home, Projects::class.java))
                        } else {
                            //Handle scenario where response body is null
                            Log.d("error new project ", "Empty response body")
                        }
                    } else {
                        // Handle unsuccessful login (e.g., invalid credentials, server errors)
                        val errorBody = response.errorBody()?.string()
                        Log.d("error new project ", "Error: $errorBody")
                    }
                }catch (e: Exception){
                    Log.d(e.message, e.message.toString())
                }
            }
    }

    private fun updateNavigationUserDetails(storedLoginResponse: LoginResponse?) {
        val header = binding.navigationView.getHeaderView(0)
        val userName = header.findViewById<TextView>(R.id.tv_name)
        val userDesignation = header.findViewById<TextView>(R.id.tv_designation)

        if (storedLoginResponse != null && userName != null) {
            userName.text = "Name: ${storedLoginResponse.user.name}"
            userDesignation.text = "Desination: ${storedLoginResponse.user.designation}"
        }
    }


    private fun setUpActionBar() {
//        set drawer layout
        setSupportActionBar(findViewById<Toolbar>(R.id.my_toolbar))

        findViewById<Toolbar>(R.id.my_toolbar).setNavigationIcon(R.drawable.home)

        findViewById<Toolbar>(R.id.my_toolbar).setNavigationOnClickListener {
            Log.d("clicked", "clicked")
            toggleDrawer()
        }
    }

    //    toggle the navigation bar
    private fun toggleDrawer() {
        if (findViewById<DrawerLayout>(R.id.drawerLayout).isDrawerOpen(GravityCompat.START)) {
            findViewById<DrawerLayout>(R.id.drawerLayout).closeDrawer(GravityCompat.START)
        } else {
            findViewById<DrawerLayout>(R.id.drawerLayout).openDrawer(GravityCompat.START)
        }
    }

    private fun getProfile() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(Apis::class.java)




        lifecycleScope.launch {
            try {
                val response = apiService.myProfile("token=$token")
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        // Handle successful login response
                    } else {
                        // Handle scenario where response body is null
                        Log.d("LoginError", "Empty response body")
                    }
                } else {
                    // Handle unsuccessful login (e.g., invalid credentials, server errors)
                    val errorBody = response.errorBody()?.string()
                    Log.d("LoginError", "Error: $errorBody")
                }
            } catch (e: IOException) {
                // Handle network issues or I/O problems
                Log.d("LoginError", "Network error: ${e.message}")
            } catch (e: Exception) {
                // Handle other exceptions
                Log.d("LoginError", "Error: ${e.message}")
            }
        }
    }

    //    handle for get the employee details
    private fun getEmployee() {
        //count the employee
        val mainContent: View = findViewById(R.id.main_content)
        val tvEmployee = mainContent.findViewById<TextView>(R.id.tv_employee)
        val selectedManager = mainContent.findViewById<Spinner>(R.id.spinnerManagerName)

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(Apis::class.java)
        lifecycleScope.launch {
            try {
                val response = apiService.getEmployeeDetails("token=$token")
                if (response.isSuccessful) {
                    val employeeResponse = response.body()
                    if (employeeResponse != null) {
                        // Handle successful login response
                        Log.d("msg","$employeeResponse")
                        for (i in employeeResponse.employee) {
                            employeeDetails.add(i)
                            if(i.designationType == "manager"){
                                managers.add(i)
                            }
                        }

                        //set the value employee card number
                        tvEmployee.text = employeeDetails.size.toString()

                        // Extract manager names from the list of employees
                        val managerNames = managers.filter { it.designationType == "manager" }.map { it.employeeName }.toTypedArray()



                        // Create an ArrayAdapter using the managerNames array and a default spinner layout
                        val adapter = ArrayAdapter(this@Home, android.R.layout.simple_spinner_item, managerNames)

                        // Specify the layout to use when the list of choices appears
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                        // Apply the adapter to the spinner
                        selectedManager.adapter = adapter

                        selectedManager.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                val managerName = managerNames[position] // Get the selected priority
                                // Handle the selected priority as needed
                                val managerId =
                                    employeeDetails.filter { it.employeeName == managerName }
                                        .joinToString { it._id }
                                Log.d("msg", managerId)
                                selectedManagerName = managerId
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                // Handle case when nothing is selected (if needed)
                            }
                        }


                    } else {
                        // Handle scenario where response body is null
                        Log.d("employee error", "Empty response body")
                    }
                } else {
                    // Handle unsuccessful login (e.g., invalid credentials, server errors)
                    val errorBody = response.errorBody()?.string()
                    Log.d("employee error", "Error: $errorBody")
                }
            } catch (e: IOException) {
                // Handle other exceptions
                Log.d("employee error", "Error: ${e.message}")
            }
        }





    }


//    handle for get project
    private fun getProjects() {
        //count the employee
        val mainContent: View = findViewById(R.id.main_content)
        val tvProject = mainContent.findViewById<TextView>(R.id.tv_all_project)
        val tvOngoing = mainContent.findViewById<TextView>(R.id.tv_ongoing_project)
        val tvCompleted = mainContent.findViewById<TextView>(R.id.tv_completed_project)

        //for api call
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiServices = retrofit.create(Apis::class.java)

        lifecycleScope.launch {
            try {
                val response = apiServices.getProjectDetails("token=$token")
                if (response.isSuccessful) {
                    val projectResponse = response.body()
                    if (projectResponse != null) {
                        for(project in projectResponse.allProject){
//                            push all the project
                            allProject.add(project)
//                            for completed and in comleted project
                            if (project.isCompleted){
                                completedProject.add(project)
                            }else{
                                ongoingProject.add(project)
                            }
                        }
                        Log.d("project details", "$allProject")

//                        count all the project
                        tvProject.text =  "${allProject.size}"

//                        count all completed project
                        tvCompleted.text = "${completedProject.size}"

//                        count all ongoing project
                          tvOngoing.text = "${ongoingProject.size}"

                    } else {
                        // Handle scenario where response body is null
                        Log.d("project error", "Empty response body")
                    }
                } else {
                    // Handle unsuccessful login (e.g., invalid credentials, server errors)
                    val errorBody = response.errorBody()?.string()
                    Log.d("project error", "Error: $errorBody")
                }
            } catch (e: IOException) {
                // Handle other exceptions
                Log.d("project error", "Error: ${e.message}")
            }
        }

    }


    //date picker
    fun showSubmissionDatePickerDialog(view: android.view.View) {
        val submissionDate = findViewById<EditText>(R.id.etSubmissionDate)

        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formattedDate = sdf.format(Date(selection))
            submissionDate.setText(formattedDate)
        }

        picker.show(supportFragmentManager, picker.toString())
    }

    //
    fun showStartDatePickerDialog(view: android.view.View) {
        val startDate = findViewById<EditText>(R.id.etProjectStartDate)

        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formattedDate = sdf.format(Date(selection))
            startDate.setText(formattedDate)
        }

        picker.show(supportFragmentManager, picker.toString())
    }


    // handle for spinner
    private fun setSpinner() {
        val mainContent: View = findViewById(R.id.main_content)
        val spinnerPriority =  mainContent.findViewById<Spinner>(R.id.spinnerPriority)
        val spinnerTeam = mainContent.findViewById<Spinner>(R.id.spinnerTeam)

        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorities)
        val teamAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, teams)

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapter to the spinner
        spinnerPriority.adapter = adapter
        spinnerTeam.adapter = teamAdapter


        spinnerPriority.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val priority = priorities[position] // Get the selected priority
                // Handle the selected priority as needed
                selectedPriority = priority
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case when nothing is selected (if needed)
            }
        }

        spinnerTeam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val team = teams[position] // Get the selected priority
                // Handle the selected priority as needed
                selectedTeam = team
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case when nothing is selected (if needed)
            }
        }

    }

}