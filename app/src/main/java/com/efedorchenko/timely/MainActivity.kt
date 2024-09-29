package com.efedorchenko.timely

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.efedorchenko.timely.fragment.CalendarPageAdapter
import com.google.android.material.navigation.NavigationView
import com.jakewharton.threetenabp.AndroidThreeTen

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var cardView: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupInsets()
        setupViewPager()
        setupSideMenu()

        cardView = findViewById(R.id.statsCard)
        cardView.background = ContextCompat.getDrawable(this, R.drawable.stats_card_background)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViewPager() {
        AndroidThreeTen.init(this)

        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = CalendarPageAdapter(this)
        viewPager.setCurrentItem(CalendarPageAdapter.CALENDAR_SCROLL_BORDERS / 2, false)
    }


    private fun setupSideMenu() {

        drawerLayout = findViewById(R.id.drawer_layout)
        val menuButton: ImageButton = findViewById(R.id.menu_button)
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)

        val usernameTextView = headerView.findViewById<TextView>(R.id.user_name)
        usernameTextView.text = "Федорченко Евгений Викторович"

        val positionTextView = headerView.findViewById<TextView>(R.id.position)
        val spannablePositionText = SpannableString("Должность: колотильщик паллетов")
        spannablePositionText.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            9,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        positionTextView.text = spannablePositionText

        val rateTextView = headerView.findViewById<TextView>(R.id.rate)
        val spannableRateText = SpannableString("Ставка: 200 руб./ч.")
        spannableRateText.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            6,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        rateTextView.text = spannableRateText

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.fill_period -> {
                }

                R.id.my_team -> {
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
}