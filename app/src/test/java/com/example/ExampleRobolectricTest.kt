package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ParkingTicket
import com.example.data.model.RateConfig
import com.example.util.FeeCalculator
import com.example.util.OcrHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MotoPark", appName)
    }

    @Test
    fun `calculate moto fee with helmet storage`() {
        val entryTime = 1000000000L
        val exitTime = entryTime + (2 * 3600 * 1000L) // 2 hours

        val ticket = ParkingTicket(
            id = 1,
            plateNumber = "ABC12D",
            vehicleType = "MOTO",
            entryTimeMillis = entryTime,
            hasHelmet = true,
            helmetCount = 1
        )

        val config = RateConfig(
            motoRatePerHour = 2000.0,
            helmetStorageFee = 1000.0,
            isHelmetFeeActive = true,
            helmetFeeType = "TARIFA_UNICA"
        )

        val fee = FeeCalculator.calculateFee(ticket, config, exitTime)
        assertEquals(4000.0, fee.baseFee, 0.01)
        assertEquals(1000.0, fee.helmetFee, 0.01)
        assertEquals(5000.0, fee.totalAmount, 0.01)
    }

    @Test
    fun `calculate fee with grace period`() {
        val entryTime = 1000000000L
        val exitTime = entryTime + (10 * 60 * 1000L) // 10 minutes (within 15 min grace)

        val ticket = ParkingTicket(
            id = 2,
            plateNumber = "XYZ789",
            vehicleType = "CARRO",
            entryTimeMillis = entryTime,
            hasHelmet = false
        )

        val config = RateConfig(
            carRatePerHour = 4000.0,
            gracePeriodMinutes = 15
        )

        val fee = FeeCalculator.calculateFee(ticket, config, exitTime)
        assertEquals(0.0, fee.totalAmount, 0.01)
    }

    @Test
    fun `test plate regex extraction`() {
        val text = "ESTACIONAMIENTO MOTOPARK\nVEHICULO ENCONTRADO\nPLACA: KXM-45E\nCOLOR: AZUL"
        val plate = OcrHelper.extractPlateCandidate(text)
        assertNotNull(plate)
        assertEquals("KXM45E", plate)
    }

    @Test
    fun `test formatters with different currencies and separators`() {
        val colConfig = RateConfig(
            currencySymbol = "$",
            currencyCode = "COL",
            thousandsSeparator = "PUNTO_DECIMAL_COMA",
            decimalPlaces = 0
        )
        val formattedCol = com.example.util.Formatters.formatCurrency(1250000.0, colConfig)
        assertEquals("$ 1.250.000", formattedCol)

        val solConfig = RateConfig(
            currencySymbol = "S/.",
            currencyCode = "SOL",
            thousandsSeparator = "COMA_DECIMAL_PUNTO",
            decimalPlaces = 2
        )
        val formattedSol = com.example.util.Formatters.formatCurrency(45.50, solConfig)
        assertEquals("S/. 45.50", formattedSol)
    }

    @Test
    fun `calculate bicycle and car fees correctly`() {
        val entryTime = 1000000000L
        val exitTime = entryTime + (3 * 3600 * 1000L) // 3 hours

        val bikeTicket = ParkingTicket(
            id = 3,
            plateNumber = "BICI-01",
            vehicleType = "BICICLETA",
            entryTimeMillis = entryTime,
            hasHelmet = false
        )

        val config = RateConfig(
            bikeRatePerHour = 1000.0,
            gracePeriodMinutes = 0
        )

        val fee = FeeCalculator.calculateFee(bikeTicket, config, exitTime)
        assertEquals(3000.0, fee.totalAmount, 0.01)
    }
}
