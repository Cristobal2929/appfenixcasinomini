package com.fenixcasino.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : Activity() {

    private lateinit var tvPuntos: TextView
    private lateinit var contenedorJuegos: FrameLayout
    private lateinit var prefs: SharedPreferences
    private var puntosUsuario: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPuntos = findViewById(R.id.tv_puntos)
        contenedorJuegos = findViewById(R.id.contenedor_juegos)

        prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        puntosUsuario = prefs.getInt("puntos", 100)
        actualizarPuntos()

        mostrarMenuPrincipal()
    }

    private fun actualizarPuntos() {
        tvPuntos.text = "💰 Puntos: $puntosUsuario"
        prefs.edit().putInt("puntos", puntosUsuario).apply()
    }

    private fun mostrarMenuPrincipal() {
        contenedorJuegos.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        fun crearBoton(texto: String, onClick: (View) -> Unit): Button {
            return Button(this).apply {
                text = texto
                setBackgroundColor(Color.parseColor("#C41E3A"))
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                textSize = 18f
                setOnClickListener(onClick)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 20, 0, 0)
                }
            }
        }

        val btnRuleta = crearBoton("🎰 Ruleta") { iniciarJuegoRuleta() }
        val btnRasca = crearBoton("🎟️ Rasca y Gana") { iniciarJuegoRasca() }
        val btnBlackjack = crearBoton("🃏 Blackjack") { iniciarJuegoBlackjack() }
        val btnRuletaCasino = crearBoton("🎱 Ruleta Casino") { iniciarJuegoRuletaCasino() }

        layout.addView(btnRuleta)
        layout.addView(btnRasca)
        layout.addView(btnBlackjack)
        layout.addView(btnRuletaCasino)

        contenedorJuegos.addView(layout)
    }

    // ---------- Juego 1: Ruleta ----------
    private fun iniciarJuegoRuleta() {
        contenedorJuegos.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A1633"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val ruletaView = RuletaView(this)
        val btnGirar = Button(this).apply {
            text = "Girar"
            setBackgroundColor(Color.parseColor("#C41E3A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }
        val btnVolver = Button(this).apply {
            text = "← Volver"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#D4AF37"))
            setTypeface(null, Typeface.BOLD)
        }

        btnVolver.setOnClickListener { mostrarMenuPrincipal() }

        btnGirar.setOnClickListener {
            if (puntosUsuario < 10) {
                Toast.makeText(this, "Sin puntos suficientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            puntosUsuario -= 10
            actualizarPuntos()
            btnGirar.isEnabled = false
            ruletaView.iniciarGiro { multiplicador ->
                val ganancia = multiplicador * 10
                if (ganancia > 0) {
                    puntosUsuario += ganancia
                    Toast.makeText(this, "¡Ganaste $ganancia puntos!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No ganaste puntos", Toast.LENGTH_SHORT).show()
                }
                actualizarPuntos()
                btnGirar.isEnabled = true
            }
        }

        layout.addView(
            ruletaView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        layout.addView(
            btnGirar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        layout.addView(
            btnVolver,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        contenedorJuegos.addView(layout)
    }

    inner class RuletaView(context: Context) : View(context) {
        private val paintSector = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = Color.parseColor("#D4AF37")
        }
        private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        private val rect = RectF()
        private val multiplicadores = arrayOf(0, 0, 0, 2, 2, 3, 5, 10)

        fun iniciarGiro(onResult: (Int) -> Unit) {
            val offset = Random.nextInt(0, 360)
            val animator = ValueAnimator.ofFloat(0f, 1440f + offset)
            animator.duration = 3000
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener {
                rotation = it.animatedValue as Float
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    val angulo = ((rotation % 360) + 360) % 360
                    val sector = ((360 - angulo) / 45).toInt() % 8
                    val mult = multiplicadores[sector]
                    onResult(mult)
                }
            })
            animator.start()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val size = min(width, height).toFloat()
            val radius = size / 2 * 0.9f
            val cx = width / 2f
            val cy = height / 2f
            rect.set(cx - radius, cy - radius, cx + radius, cy + radius)

            for (i in 0 until 8) {
                paintSector.color = if (i % 2 == 0) Color.parseColor("#C41E3A") else Color.parseColor("#1B2B4D")
                canvas.drawArc(rect, i * 45f, 45f, true, paintSector)
                canvas.drawArc(rect, i * 45f, 45f, true, paintBorder)

                val angle = Math.toRadians((i * 45 + 22.5).toDouble())
                val textX = cx + (radius * 0.6 * cos(angle)).toFloat()
                val textY = cy + (radius * 0.6 * sin(angle)).toFloat() + 15f
                canvas.drawText("x${multiplicadores[i]}", textX, textY, paintText)
            }

            val path = Path()
            path.moveTo(cx, cy - radius - 30)
            path.lineTo(cx - 30, cy - radius - 10)
            path.lineTo(cx + 30, cy - radius - 10)
            path.close()
            val paintArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D4AF37")
                style = Paint.Style.FILL
            }
            canvas.drawPath(path, paintArrow)
        }
    }

    // ---------- Juego 2: Rasca y Gana ----------
    private fun iniciarJuegoRasca() {
        contenedorJuegos.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A1633"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val tvSimbolo1 = crearSimboloTextView()
        val tvSimbolo2 = crearSimboloTextView()
        val tvSimbolo3 = crearSimboloTextView()
        val btnComprar = Button(this).apply {
            text = "Comprar carta"
            setBackgroundColor(Color.parseColor("#C41E3A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }
        val btnVolver = Button(this).apply {
            text = "← Volver"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#D4AF37"))
            setTypeface(null, Typeface.BOLD)
        }

        btnVolver.setOnClickListener { mostrarMenuPrincipal() }

        btnComprar.setOnClickListener {
            if (puntosUsuario < 5) {
                Toast.makeText(this, "Sin puntos suficientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            puntosUsuario -= 5
            actualizarPuntos()
            val simbolos = arrayOf("🍒", "🍋", "🔔", "💎", "7️⃣")
            val s1 = simbolos.random()
            val s2 = simbolos.random()
            val s3 = simbolos.random()
            tvSimbolo1.text = s1
            tvSimbolo2.text = s2
            tvSimbolo3.text = s3

            val ganancia = when {
                s1 == s2 && s2 == s3 -> 50
                s1 == s2 || s1 == s3 || s2 == s3 -> 15
                else -> 0
            }
            if (ganancia > 0) {
                puntosUsuario += ganancia
                Toast.makeText(this, "¡Ganaste $ganancia puntos!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No ganaste puntos", Toast.LENGTH_SHORT).show()
            }
            actualizarPuntos()
        }

        layout.addView(tvSimbolo1)
        layout.addView(tvSimbolo2)
        layout.addView(tvSimbolo3)
        layout.addView(btnComprar)
        layout.addView(btnVolver)

        contenedorJuegos.addView(layout)
    }

    private fun crearSimboloTextView(): TextView {
        return TextView(this).apply {
            textSize = 48f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    // ---------- Juego 3: Blackjack ----------
    private fun iniciarJuegoBlackjack() {
        contenedorJuegos.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A1633"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val tvDealer = TextView(this).apply {
            text = "Banca: "
            setBackgroundColor(Color.parseColor("#1B2B4D"))
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(20, 20, 20, 20)
        }
        val tvPlayer = TextView(this).apply {
            text = "Jugador: "
            setBackgroundColor(Color.parseColor("#1B2B4D"))
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(20, 20, 20, 20)
        }

        val btnRepartir = Button(this).apply {
            text = "Repartir"
            setBackgroundColor(Color.parseColor("#C41E3A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }
        val btnPedir = Button(this).apply {
            text = "Pedir carta"
            setBackgroundColor(Color.parseColor("#C41E3A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            isEnabled = false
        }
        val btnPlantarse = Button(this).apply {
            text = "Plantarse"
            setBackgroundColor(Color.parseColor("#C41E3A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            isEnabled = false
        }
        val btnVolver = Button(this).apply {
            text = "← Volver"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#D4AF37"))
            setTypeface(null, Typeface.BOLD)
        }

        btnVolver.setOnClickListener { mostrarMenuPrincipal() }

        var playerCards = mutableListOf<Int>()
        var dealerCards = mutableListOf<Int>()
        var gameActive = false

        fun actualizarPantallas() {
            tvPlayer.text = "Jugador: ${playerCards.joinToString(", ")} (Total: ${playerCards.sum()})"
            val dealerDisplay = if (gameActive) {
                dealerCards[0].toString() + ", ?"
            } else {
                dealerCards.joinToString(", ")
            }
            val dealerTotal = if (gameActive) dealerCards[0] else dealerCards.sum()
            tvDealer.text = "Banca: $dealerDisplay (Total: $dealerTotal)"
        }

        btnRepartir.setOnClickListener {
            if (puntosUsuario < 10) {
                Toast.makeText(this, "Sin puntos suficientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            puntosUsuario -= 10
            actualizarPuntos()
            playerCards.clear()
            dealerCards.clear()
            playerCards.add(Random.nextInt(1, 12))
            playerCards.add(Random.nextInt(1, 12))
            dealerCards.add(Random.nextInt(1, 12))
            dealerCards.add(Random.nextInt(1, 12))
            gameActive = true
            btnPedir.isEnabled = true
            btnPlantarse.isEnabled = true
            actualizarPantallas()
        }

        btnPedir.setOnClickListener {
            playerCards.add(Random.nextInt(1, 12))
            if (playerCards.sum() > 21) {
                Toast.makeText(this, "Te pasaste. Pierdes la apuesta.", Toast.LENGTH_SHORT).show()
                gameActive = false
                btnPedir.isEnabled = false
                btnPlantarse.isEnabled = false
            }
            actualizarPantallas()
        }

        btnPlantarse.setOnClickListener {
            gameActive = false
            while (dealerCards.sum() < 17) {
                dealerCards.add(Random.nextInt(1, 12))
            }
            val playerTotal = playerCards.sum()
            val dealerTotal = dealerCards.sum()
            when {
                playerTotal > 21 -> {
                    Toast.makeText(this, "Te pasaste. Pierdes la apuesta.", Toast.LENGTH_SHORT).show()
                }
                dealerTotal > 21 || playerTotal > dealerTotal -> {
                    puntosUsuario += 20
                    Toast.makeText(this, "¡Ganaste! +20 puntos.", Toast.LENGTH_SHORT).show()
                }
                playerTotal == dealerTotal -> {
                    puntosUsuario += 10
                    Toast.makeText(this, "Empate. Recuperas apuesta.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Pierdes la apuesta.", Toast.LENGTH_SHORT).show()
                }
            }
            actualizarPuntos()
            btnPedir.isEnabled = false
            btnPlantarse.isEnabled = false
            actualizarPantallas()
        }

        layout.addView(tvDealer)
        layout.addView(tvPlayer)
        layout.addView(btnRepartir)
        layout.addView(btnPedir)
        layout.addView(btnPlantarse)
        layout.addView(btnVolver)

        contenedorJuegos.addView(layout)
    }

    // ---------- Juego 4: Ruleta Casino ----------
    private fun iniciarJuegoRuletaCasino() {
        contenedorJuegos.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A1633"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val ruletaCasinoView = RuletaCasinoView(this)
        val btnRojo = Button(this).apply {
            text = "Apostar Rojo"
            setBackgroundColor(Color.parseColor("#C41E3A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }
        val btnNegro = Button(this).apply {
            text = "Apostar Negro"
            setBackgroundColor(Color.parseColor("#C41E3A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }
        val btnVerde = Button(this).apply {
            text = "Apostar Verde"
            setBackgroundColor(Color.parseColor("#C41E3A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }
        val etNumero = EditText(this).apply {
            hint = "Número (0-11) opcional"
            setBackgroundColor(Color.parseColor("#1B2B4D"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val btnGirar = Button(this).apply {
            text = "Girar"
            setBackgroundColor(Color.parseColor("#C41E3A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }
        val btnVolver = Button(this).apply {
            text = "← Volver"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#D4AF37"))
            setTypeface(null, Typeface.BOLD)
        }

        btnVolver.setOnClickListener { mostrarMenuPrincipal() }

        var apuestaColor: String? = null

        fun resetearApuesta() {
            apuestaColor = null
            btnRojo.isEnabled = true
            btnNegro.isEnabled = true
            btnVerde.isEnabled = true
        }

        btnRojo.setOnClickListener {
            apuestaColor = "rojo"
            btnRojo.isEnabled = false
            btnNegro.isEnabled = false
            btnVerde.isEnabled = false
        }
        btnNegro.setOnClickListener {
            apuestaColor = "negro"
            btnRojo.isEnabled = false
            btnNegro.isEnabled = false
            btnVerde.isEnabled = false
        }
        btnVerde.setOnClickListener {
            apuestaColor = "verde"
            btnRojo.isEnabled = false
            btnNegro.isEnabled = false
            btnVerde.isEnabled = false
        }

        btnGirar.setOnClickListener {
            if (apuestaColor == null) {
                Toast.makeText(this, "Selecciona un color para apostar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (puntosUsuario < 15) {
                Toast.makeText(this, "Sin puntos suficientes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val numeroIngresado = etNumero.text.toString().toIntOrNull()
            puntosUsuario -= 15
            actualizarPuntos()
            btnGirar.isEnabled = false
            ruletaCasinoView.iniciarGiro(apuestaColor!!, numeroIngresado) { ganancia ->
                if (ganancia > 0) {
                    puntosUsuario += ganancia
                    Toast.makeText(this, "¡Ganaste $ganancia puntos!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No ganaste puntos", Toast.LENGTH_SHORT).show()
                }
                actualizarPuntos()
                btnGirar.isEnabled = true
                resetearApuesta()
                etNumero.text.clear()
            }
        }

        layout.addView(
            ruletaCasinoView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        layout.addView(btnRojo)
        layout.addView(btnNegro)
        layout.addView(btnVerde)
        layout.addView(etNumero)
        layout.addView(btnGirar)
        layout.addView(btnVolver)

        contenedorJuegos.addView(layout)
    }

    inner class RuletaCasinoView(context: Context) : View(context) {
        private val paintSector = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = Color.parseColor("#D4AF37")
        }
        private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        private val paintBall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
        }
        private val rect = RectF()
        private var ballAngle = 0f

        fun iniciarGiro(colorApostado: String, numeroApostado: Int?, onResult: (Int) -> Unit) {
            val offset = Random.nextInt(0, 360)
            val animator = ValueAnimator.ofFloat(0f, 360f + offset)
            animator.duration = 3500
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener {
                ballAngle = it.animatedValue as Float
                invalidate()
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    val finalAngle = (ballAngle % 360 + 360) % 360
                    val sector = ((finalAngle) / 30).toInt() % 12
                    val colorSector = when {
                        sector == 0 -> "verde"
                        sector % 2 == 1 -> "rojo"
                        else -> "negro"
                    }
                    val ganancia = when {
                        numeroApostado != null && numeroApostado == sector -> 150
                        colorApostado == colorSector -> 30
                        else -> 0
                    }
                    onResult(ganancia)
                }
            })
            animator.start()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val size = min(width, height).toFloat()
            val radius = size / 2 * 0.9f
            val cx = width / 2f
            val cy = height / 2f
            rect.set(cx - radius, cy - radius, cx + radius, cy + radius)

            for (i in 0 until 12) {
                paintSector.color = when {
                    i == 0 -> Color.parseColor("#2E7D32")
                    i % 2 == 1 -> Color.parseColor("#C41E3A")
                    else -> Color.parseColor("#1A1A1A")
                }
                canvas.drawArc(rect, i * 30f, 30f, true, paintSector)
                canvas.drawArc(rect, i * 30f, 30f, true, paintBorder)

                val angle = Math.toRadians((i * 30 + 15).toDouble())
                val textX = cx + (radius * 0.7 * cos(angle)).toFloat()
                val textY = cy + (radius * 0.7 * sin(angle)).toFloat() + 10f
                canvas.drawText(i.toString(), textX, textY, paintText)
            }

            val ballRadius = radius * 0.07f
            val ballX = cx + (radius * 0.8 * cos(Math.toRadians(ballAngle.toDouble()))).toFloat()
            val ballY = cy + (radius * 0.8 * sin(Math.toRadians(ballAngle.toDouble()))).toFloat()
            canvas.drawCircle(ballX, ballY, ballRadius, paintBall)
        }
    }
}