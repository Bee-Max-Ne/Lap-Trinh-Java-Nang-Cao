package test;

import huddtds.model.Element;
import huddtds.model.Transaction;
import huddtds.model.flyweight.ItemFactory;
import huddtds.model.flyweight.ItemFlyweight;
import huddtds.model.pool.TransactionPool;

/**
 * Test class for Flyweight and Object Pool patterns.
 * Viết theo phong cách standalone runner (không phụ thuộc external JUnit jar).
 */
public class FlyweightAndPoolTest {

    public static void main(String[] args) {
        System.out.println("=== Chạy FlyweightAndPoolTest ===");
        testFlyweight();
        testTransactionPool();
        System.out.println("FlyweightAndPoolTest PASSED ✓");
    }

    public static void testFlyweight() {
        System.out.print("1. Kiểm tra Flyweight pattern cho ItemFlyweight và Element... ");
        ItemFactory.clear();

        ItemFlyweight fw1 = ItemFactory.getOrCreate("item1", 1.5);
        ItemFlyweight fw2 = ItemFactory.getOrCreate("item1", 1.5);
        require(fw1 == fw2, "Same itemId and externalUtility should return the same flyweight instance");

        ItemFlyweight fw3 = ItemFactory.getOrCreate("item2", 1.5);
        require(fw1 != fw3, "Different itemId should return different flyweight");

        Element e1 = new Element("item1", 2);
        Element e2 = new Element("item1", 3);
        require(e1.getFlyweight() == e2.getFlyweight(), "Elements with same item should share the same flyweight instance");
        require("item1".equals(e1.getItem()), "Element's getItem should return the itemId");
        require(e1.getQuantity() == 2, "Element's getQuantity should return the quantity");
        require(e1.getUtility() == 0.0, "Element's getUtility should return 0.0 for quantity-based");

        System.out.println("OK");
    }

    public static void testTransactionPool() {
        System.out.print("2. Kiểm tra Object Pool pattern cho TransactionPool... ");
        TransactionPool pool = new TransactionPool(10);

        Transaction tx1 = pool.acquire(1);
        require(tx1 != null, "Acquired transaction should not be null");
        require(tx1.getTid() == 1, "Acquired transaction should have correct TID");
        require(tx1.getElements().isEmpty(), "Newly acquired transaction should have no elements");

        tx1.addElement("item1", 2);
        require(!tx1.getElements().isEmpty(), "Transaction should now have an element");

        pool.release(tx1);

        Transaction tx2 = pool.acquire(2);
        require(tx2 != null, "Acquired transaction should not be null");
        require(tx2.getTid() == 2, "Acquired transaction should have the new TID");
        require(tx2.getElements().isEmpty(), "Released and re-acquired transaction should have no elements");
        require(tx1 == tx2, "Re-acquired transaction should be the same instance as the released one");

        for (int i = 0; i < 20; i++) {
            Transaction tx = pool.acquire(i + 100);
            require(tx != null, "Transaction should not be null");
            require(tx.getTid() == i + 100, "Transaction TID should be correct");
            pool.release(tx);
        }

        System.out.println("OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}