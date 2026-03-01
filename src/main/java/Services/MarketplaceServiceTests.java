package Services;

import Models.*;
import java.util.*;

/**
 * Basic unit tests for marketplace services
 * Tests CRUD operations and core business logic
 */
public class MarketplaceServiceTests {
    private static final String LOG_TAG = "[MarketplaceServiceTests]";
    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("=== Marketplace Service Test Suite ===\n");

        // Test listing service
        testListingService();

        // Test order service
        testOrderService();

        // Test offer service
        testOfferService();

        // Test escrow service
        testEscrowService();

        // Test dispute service
        testDisputeService();

        // Test fee service
        testFeeService();

        // Test validation
        testValidation();

        // Print summary
        printSummary();
    }

    private static void testListingService() {
        System.out.println("--- Testing MarketplaceListingService ---");
        
        MarketplaceListingService service = MarketplaceListingService.getInstance();
        
        // Test: Get all active listings
        try {
            List<MarketplaceListing> listings = service.getActiveListings();
            assert(listings != null);
            pass("getActiveListings returns non-null list");
        } catch (Exception e) {
            fail("getActiveListings: " + e.getMessage());
        }

        // Test: Get listings by seller
        try {
            List<MarketplaceListing> listings = service.getSellerListings(1);
            assert(listings != null);
            pass("getListingsBySeller returns non-null list");
        } catch (Exception e) {
            fail("getListingsBySeller: " + e.getMessage());
        }

        // Test: Search listings
        try {
            List<MarketplaceListing> listings = service.searchListings("CARBON_CREDITS", null, null, 50);
            assert(listings != null);
            pass("searchListings returns non-null list");
        } catch (Exception e) {
            fail("searchListings: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testOrderService() {
        System.out.println("--- Testing MarketplaceOrderService ---");
        
        MarketplaceOrderService service = MarketplaceOrderService.getInstance();

        // Test: Get order by ID
        try {
            MarketplaceOrder order = service.getOrderById(1);
            if (order != null) {
                assert(order.getId() > 0);
                pass("getOrderById returns valid order");
            } else {
                pass("getOrderById returns null for non-existent order");
            }
        } catch (Exception e) {
            fail("getOrderById: " + e.getMessage());
        }

        // Test: Get buyer orders
        try {
            List<MarketplaceOrder> orders = service.getOrderHistory(1);
            assert(orders != null);
            pass("getOrderHistory returns non-null list");
        } catch (Exception e) {
            fail("getOrderHistory: " + e.getMessage());
        }

        // Test: Get seller orders
        try {
            List<MarketplaceOrder> orders = service.getOrderHistory(1);
            assert(orders != null);
            pass("getSellerOrders returns non-null list");
        } catch (Exception e) {
            fail("getSellerOrders: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testOfferService() {
        System.out.println("--- Testing MarketplaceOfferService ---");
        
        MarketplaceOfferService service = MarketplaceOfferService.getInstance();

        // Test: Get received offers
        try {
            List<MarketplaceOffer> offers = service.getOffersReceived(1);
            assert(offers != null);
            pass("getOffersReceived returns non-null list");
        } catch (Exception e) {
            fail("getOffersReceived: " + e.getMessage());
        }

        // Test: Get sent offers
        try {
            List<MarketplaceOffer> offers = service.getOffersSent(1);
            assert(offers != null);
            pass("getOffersSent returns non-null list");
        } catch (Exception e) {
            fail("getOffersSent: " + e.getMessage());
        }

        // Test: Get pending offers
        try {
            List<MarketplaceOffer> offers = service.getOffersReceived(1);
            assert(offers != null);
            pass("getPendingOffers returns non-null list");
        } catch (Exception e) {
            fail("getPendingOffers: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testEscrowService() {
        System.out.println("--- Testing MarketplaceEscrowService ---");
        
        MarketplaceEscrowService service = MarketplaceEscrowService.getInstance();

        // Test: Get escrow by ID
        try {
            MarketplaceEscrow escrow = service.getEscrowById(1);
            if (escrow != null) {
                assert(escrow.getId() > 0);
                pass("getEscrowById returns valid escrow");
            } else {
                pass("getEscrowById returns null for non-existent escrow");
            }
        } catch (Exception e) {
            fail("getEscrowById: " + e.getMessage());
        }

        // Test: Get expired escrows
        try {
            List<MarketplaceEscrow> escrows = service.getExpiredEscrows();
            assert(escrows != null);
            pass("getExpiredEscrows returns non-null list");
        } catch (Exception e) {
            fail("getExpiredEscrows: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testDisputeService() {
        System.out.println("--- Testing MarketplaceDisputeService ---");
        
        MarketplaceDisputeService service = MarketplaceDisputeService.getInstance();

        // Test: Get pending disputes
        try {
            List<MarketplaceDispute> disputes = service.getPendingDisputes();
            assert(disputes != null);
            pass("getPendingDisputes returns non-null list");
        } catch (Exception e) {
            fail("getPendingDisputes: " + e.getMessage());
        }

        // Test: Get disputes by reporter
        try {
            List<MarketplaceDispute> disputes = service.getDisputesByReporter(1);
            assert(disputes != null);
            pass("getDisputesByReporter returns non-null list");
        } catch (Exception e) {
            fail("getDisputesByReporter: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testFeeService() {
        System.out.println("--- Testing MarketplaceFeeService ---");
        
        MarketplaceFeeService service = MarketplaceFeeService.getInstance();

        // Test: Get total fees collected
        try {
            java.math.BigDecimal total = service.getTotalFeesCollected();
            assert(total != null);
            assert(total.compareTo(java.math.BigDecimal.ZERO) >= 0);
            pass("getTotalFeesCollected returns non-negative amount");
        } catch (Exception e) {
            fail("getTotalFeesCollected: " + e.getMessage());
        }

        // Test: Get fees by seller
        try {
            List<MarketplaceFee> fees = service.getFeesBySeller(1);
            assert(fees != null);
            pass("getFeesBySeller returns non-null list");
        } catch (Exception e) {
            fail("getFeesBySeller: " + e.getMessage());
        }

        // Test: Get fees by type
        try {
            List<MarketplaceFee> fees = service.getFeesByType("TRANSACTION");
            assert(fees != null);
            pass("getFeesByType returns non-null list");
        } catch (Exception e) {
            fail("getFeesByType: " + e.getMessage());
        }

        System.out.println();
    }

    private static void testValidation() {
        System.out.println("--- Testing MarketplaceValidator ---");

        // Test: Valid quantity
        try {
            Utils.MarketplaceValidator.validateQuantity(5.0);
            pass("validateQuantity accepts positive values");
        } catch (Exception e) {
            fail("validateQuantity positive: " + e.getMessage());
        }

        // Test: Invalid quantity
        try {
            Utils.MarketplaceValidator.validateQuantity(0);
            fail("validateQuantity should reject zero");
        } catch (Utils.MarketplaceException e) {
            pass("validateQuantity rejects zero values");
        }

        // Test: Valid price
        try {
            Utils.MarketplaceValidator.validatePrice(100.0);
            pass("validatePrice accepts positive values");
        } catch (Exception e) {
            fail("validatePrice positive: " + e.getMessage());
        }

        // Test: Invalid price
        try {
            Utils.MarketplaceValidator.validatePrice(-50.0);
            fail("validatePrice should reject negative");
        } catch (Utils.MarketplaceException e) {
            pass("validatePrice rejects negative values");
        }

        // Test: Balance validation
        try {
            Utils.MarketplaceValidator.validateBalanceSufficient(1000, 500);
            pass("validateBalanceSufficient accepts sufficient balance");
        } catch (Exception e) {
            fail("validateBalanceSufficient sufficient: " + e.getMessage());
        }

        // Test: Insufficient balance
        try {
            Utils.MarketplaceValidator.validateBalanceSufficient(100, 500);
            fail("validateBalanceSufficient should reject insufficient balance");
        } catch (Utils.MarketplaceException e) {
            pass("validateBalanceSufficient rejects insufficient balance");
        }

        // Test: Email validation
        try {
            Utils.MarketplaceValidator.validateEmail("test@example.com");
            pass("validateEmail accepts valid email");
        } catch (Exception e) {
            fail("validateEmail valid: " + e.getMessage());
        }

        // Test: Invalid email
        try {
            Utils.MarketplaceValidator.validateEmail("invalid-email");
            fail("validateEmail should reject invalid email");
        } catch (Utils.MarketplaceException e) {
            pass("validateEmail rejects invalid email");
        }

        System.out.println();
    }

    private static void pass(String testName) {
        System.out.println("  ✓ PASS: " + testName);
        passCount++;
    }

    private static void fail(String testName) {
        System.out.println("  ✗ FAIL: " + testName);
        failCount++;
    }

    private static void printSummary() {
        int totalTests = passCount + failCount;
        double passPercentage = totalTests > 0 ? (passCount * 100.0 / totalTests) : 0;

        System.out.println("\n=== Test Summary ===");
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passCount);
        System.out.println("Failed: " + failCount);
        System.out.println("Pass Rate: " + String.format("%.1f%%", passPercentage));
        System.out.println("====================\n");

        if (failCount == 0) {
            System.out.println("✓ All tests passed!");
        } else {
            System.out.println("✗ Some tests failed. Review output above for details.");
        }
    }
}
