package com.example

import com.example.data.RomelRepository
import com.example.model.PriceValidator
import com.example.model.Product
import com.example.model.ProductAvailability
import com.example.model.UserRole
import com.example.ui.navigation.RomelRoute
import com.example.ui.navigation.bottomNavItemsForRole
import com.example.ui.navigation.canAccessRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RomelNavigationTest {

    @Test
    fun `bottom navigation displays Home Products Wishlist Orders and Settings for all roles`() {
        val expectedLabels = listOf("Home", "Products", "Wishlist", "Orders", "Settings")
        UserRole.entries.forEach { role ->
            assertEquals(expectedLabels, bottomNavItemsForRole(role).map { it.label })
        }
    }

    @Test
    fun `firebase role strings parse ADMIN SALES and VIEWER accurately`() {
        assertEquals(UserRole.ADMIN, UserRole.fromRaw("ADMIN"))
        assertEquals(UserRole.SALES_REP, UserRole.fromRaw("SALES"))
        assertEquals(UserRole.SALES_REP, UserRole.fromRaw("SALES_REP"))
        assertEquals(UserRole.VIEWER, UserRole.fromRaw("VIEWER"))
        assertEquals(UserRole.VIEWER, UserRole.fromRaw(null))
    }

    @Test
    fun `admin routes are allowed only for ADMIN role`() {
        val adminRoutes = listOf(
            RomelRoute.ADMIN_DASHBOARD,
            RomelRoute.ADMIN_PRODUCTS,
            RomelRoute.ADD_PRODUCT,
            RomelRoute.addOrEditProduct("RL0602"),
            RomelRoute.PRICE_MANAGEMENT,
            RomelRoute.INVENTORY,
            RomelRoute.REPORTS
        )
        adminRoutes.forEach { route ->
            assertTrue("Expected ADMIN access for $route", canAccessRoute(route, UserRole.ADMIN))
            assertFalse("Expected SALES_REP denied for $route", canAccessRoute(route, UserRole.SALES_REP))
            assertFalse("Expected VIEWER denied for $route", canAccessRoute(route, UserRole.VIEWER))
        }
    }

    @Test
    fun `standard catalog and settings routes including admin login are accessible`() {
        val sharedRoutes = listOf(
            RomelRoute.HOME,
            RomelRoute.PRODUCTS,
            RomelRoute.WISHLIST,
            RomelRoute.ORDERS,
            RomelRoute.ORDER_HISTORY,
            RomelRoute.SETTINGS,
            RomelRoute.ADMIN_LOGIN,
            RomelRoute.CATEGORIES,
            RomelRoute.SEARCH,
            RomelRoute.productDetail("RL0602")
        )
        sharedRoutes.forEach { route ->
            UserRole.entries.forEach { role ->
                assertTrue("Expected $role access for $route", canAccessRoute(route, role))
            }
        }
    }

    @Test
    fun `admin credentials input validation and salted hash verification`() {
        assertNotNull(PriceValidator.validateAdminCredentialsInput("ab", "strongPass1"))
        assertNotNull(PriceValidator.validateAdminCredentialsInput("adminUser", "12345"))
        assertNull(PriceValidator.validateAdminCredentialsInput("storeAdmin", "SecurePass#25"))

        val salt = "0123456789abcdef0123456789abcdef"
        val hashA = RomelRepository.computeCredentialHash(salt, "StoreAdmin", "MySecret#99")
        val hashSameLower = RomelRepository.computeCredentialHash(salt, "  storeadmin ", "MySecret#99")
        val hashWrongPass = RomelRepository.computeCredentialHash(salt, "StoreAdmin", "WrongPass#99")

        assertEquals(64, hashA.length)
        assertEquals(hashA, hashSameLower)
        assertNotEquals(hashA, hashWrongPass)
    }

    @Test
    fun `customer visibility hides UNAVAILABLE products while keeping AVAILABLE and OUT_OF_STOCK`() {
        val avail = Product(
            id = "a1",
            code = "A1",
            name = "Available Switch",
            description = "Two-way wall switch",
            category = "Switches",
            sellingPrice = 240.0,
            availabilityStatus = ProductAvailability.AVAILABLE.name
        )
        val out = Product(code = "A2", name = "Sold Out Switch", availabilityStatus = ProductAvailability.OUT_OF_STOCK.name)
        val hidden = Product(code = "A3", name = "Hidden Switch", availabilityStatus = ProductAvailability.UNAVAILABLE.name)

        assertEquals(240.0, avail.price!!, 0.001)
        assertEquals(avail.effectiveUnitPrice, avail.price)
        assertTrue(avail.isVisibleToCustomers)
        assertTrue(out.isVisibleToCustomers)
        assertFalse(hidden.isVisibleToCustomers)
    }

    @Test
    fun `price validator enforces valid ETB amounts and discount bounds`() {
        val blank = PriceValidator.validateSinglePrice("")
        assertNull(blank.value)
        assertNull(blank.error)

        val valid = PriceValidator.validateSinglePrice("150.256")
        assertEquals(150.26, valid.value!!, 0.001)
        assertNull(valid.error)

        assertNotNull(PriceValidator.validateSinglePrice("abc").error)
        assertNotNull(PriceValidator.validateSinglePrice("0").error)
        assertNotNull(PriceValidator.validateSinglePrice("-45").error)

        val invalidDiscount = PriceValidator.validateAll(
            sellingRaw = "150",
            wholesaleRaw = "130",
            retailRaw = "170",
            discountRaw = "180"
        )
        assertFalse(invalidDiscount.isValid)
        assertNotNull(invalidDiscount.discountError)

        val validAll = PriceValidator.validateAll(
            sellingRaw = "150",
            wholesaleRaw = "130",
            retailRaw = "170",
            discountRaw = "140"
        )
        assertTrue(validAll.isValid)
        assertEquals(150.0, validAll.sellingPrice!!, 0.001)
        assertEquals(140.0, validAll.discountPrice!!, 0.001)
    }

    @Test
    fun `home search filters products in real time by name or category`() {
        val sampleProducts = listOf(
            com.example.model.Product(
                id = "rl0602",
                code = "RL0602",
                name = "Single Pole Two Way Switch",
                category = "European Splendor Series RL06"
            ),
            com.example.model.Product(
                id = "t60_9w",
                code = "T60-9W",
                name = "LED Bulb 9W",
                category = "LED Bulbs"
            ),
            com.example.model.Product(
                id = "rl306032",
                code = "RL306032",
                name = "32 Way Distribution Board",
                category = "Distribution Boards"
            )
        )

        val byName = com.example.ui.MainViewModel.filterHomeProducts(sampleProducts, "two way switch")
        assertEquals(listOf("RL0602"), byName.map { it.code })

        val byCategory = com.example.ui.MainViewModel.filterHomeProducts(sampleProducts, "LED Bulbs")
        assertEquals(listOf("T60-9W"), byCategory.map { it.code })

        val byPartialCategory = com.example.ui.MainViewModel.filterHomeProducts(sampleProducts, "distribution")
        assertEquals(listOf("RL306032"), byPartialCategory.map { it.code })

        val blank = com.example.ui.MainViewModel.filterHomeProducts(sampleProducts, "   ")
        assertEquals(3, blank.size)
    }

    @Test
    fun `wishlist fallback product never invents prices or stock`() {
        val saved = com.example.model.WishlistItem(
            id = "uid_rl0602",
            userId = "uid",
            productId = "rl0602",
            productCode = "RL0602",
            productName = "Single Pole Two Way Switch",
            category = "European Splendor Series RL06",
            imageAsset = "catalog_images/rl0602.jpg"
        )
        val fallback = saved.toFallbackProduct()
        assertEquals("rl0602", fallback.id)
        assertEquals("RL0602", fallback.code)
        assertNull(fallback.sellingPrice)
        assertNull(fallback.stockQuantity)
        assertEquals("Price not set", com.example.model.formatEtb(fallback.effectiveUnitPrice))
    }

    @Test
    fun `order history search filters by order id order number and date`() {
        val utc = java.util.TimeZone.getTimeZone("UTC")
        val novDate = java.util.Calendar.getInstance(utc).apply {
            set(2026, java.util.Calendar.NOVEMBER, 19, 14, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
        val octDate = java.util.Calendar.getInstance(utc).apply {
            set(2026, java.util.Calendar.OCTOBER, 5, 9, 15, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time

        val sampleOrders = listOf(
            com.example.model.SalesOrder(
                id = "ord_doc_alpha_991",
                orderNumber = "SO-2026-1002",
                customerName = "Abebe Electrical",
                status = com.example.model.OrderStatus.CONFIRMED.name,
                totalAmount = 1500.0,
                createdAt = com.google.firebase.Timestamp(novDate)
            ),
            com.example.model.SalesOrder(
                id = "ord_doc_beta_442",
                orderNumber = "QT-2026-1001",
                customerName = "Bole Construction",
                status = com.example.model.OrderStatus.QUOTATION.name,
                totalAmount = 850.0,
                createdAt = com.google.firebase.Timestamp(octDate)
            )
        )

        // Match by Firestore document ID
        assertEquals(
            listOf("SO-2026-1002"),
            com.example.ui.filterOrderHistory(sampleOrders, "alpha_991", timeZone = utc).map { it.orderNumber }
        )

        // Match by display order number
        assertEquals(
            listOf("QT-2026-1001"),
            com.example.ui.filterOrderHistory(sampleOrders, "QT-2026-1001", timeZone = utc).map { it.orderNumber }
        )

        // Match by formatted human date (dd MMM yyyy)
        assertEquals(
            listOf("SO-2026-1002"),
            com.example.ui.filterOrderHistory(sampleOrders, "19 Nov 2026", timeZone = utc).map { it.orderNumber }
        )

        // Match by ISO date (yyyy-MM-dd)
        assertEquals(
            listOf("QT-2026-1001"),
            com.example.ui.filterOrderHistory(sampleOrders, "2026-10-05", timeZone = utc).map { it.orderNumber }
        )

        // Match by slash date (dd/MM/yyyy)
        assertEquals(
            listOf("SO-2026-1002"),
            com.example.ui.filterOrderHistory(sampleOrders, "19/11/2026", timeZone = utc).map { it.orderNumber }
        )

        // Combined status + date filter
        assertTrue(
            com.example.ui.filterOrderHistory(
                orders = sampleOrders,
                query = "19 Nov 2026",
                selectedStatus = com.example.model.OrderStatus.QUOTATION,
                timeZone = utc
            ).isEmpty()
        )
    }
}

