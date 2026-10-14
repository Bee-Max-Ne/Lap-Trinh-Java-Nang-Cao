package test;

import huddtds.data.pipeline.EmptyAndCommentFilterHandler;
import huddtds.data.pipeline.LegacyFormatHandler;
import huddtds.data.pipeline.SpmfFormatHandler;
import huddtds.data.pipeline.TransactionLineHandler;
import huddtds.data.pipeline.TransactionParsingPipeline;
import huddtds.model.Element;
import huddtds.model.Transaction;

/**
 * Test class for Chain of Responsibility pattern in transaction parsing pipeline.
 * Standalone runner (không phụ thuộc external JUnit jar).
 */
public class ParsingPipelineChainTest {

    public static void main(String[] args) {
        System.out.println("=== Chạy ParsingPipelineChainTest ===");
        testEmptyAndCommentLines();
        testSpmfFormat();
        testLegacyFormat();
        testPipelineChainDelegation();
        testConveniencePipeline();
        System.out.println("ParsingPipelineChainTest PASSED ✓");
    }

    public static void testEmptyAndCommentLines() {
        System.out.print("1. Kiểm tra lọc dòng trống và dòng comment (#, //)... ");
        EmptyAndCommentFilterHandler filter = new EmptyAndCommentFilterHandler();
        SpmfFormatHandler spmf = new SpmfFormatHandler();
        LegacyFormatHandler legacy = new LegacyFormatHandler();

        filter.setNext(spmf);
        spmf.setNext(legacy);

        TransactionParsingPipeline pipeline = new TransactionParsingPipeline(filter);

        require(pipeline.parse("", 1) == null, "Empty line should return null");
        require(pipeline.parse("   ", 1) == null, "Line with only spaces should return null");
        require(pipeline.parse("\t\t", 1) == null, "Line with only tabs should return null");
        require(pipeline.parse("# comment", 1) == null, "Line starting with # should return null");
        require(pipeline.parse("// comment", 1) == null, "Line starting with // should return null");
        require(pipeline.parse("   # comment", 1) == null, "Line with leading spaces and # should return null");
        System.out.println("OK");
    }

    public static void testSpmfFormat() {
        System.out.print("2. Kiểm tra bóc tách định dạng SPMF/HUIM (items : TU : utils)... ");
        EmptyAndCommentFilterHandler filter = new EmptyAndCommentFilterHandler();
        SpmfFormatHandler spmf = new SpmfFormatHandler();
        LegacyFormatHandler legacy = new LegacyFormatHandler();

        filter.setNext(spmf);
        spmf.setNext(legacy);

        TransactionParsingPipeline pipeline = new TransactionParsingPipeline(filter);

        String spmfLine = "1 2 3 : 6.0 : 1.0 2.0 3.0";
        Transaction tx = pipeline.parse(spmfLine, 42);

        require(tx != null, "SPMF line should be parsed to a Transaction");
        require(tx.getTid() == 42, "TID should be 42");
        require(Math.abs(tx.getTransactionUtility() - 6.0) < 0.0001, "Transaction utility should be 6.0");
        require(tx.getElements().size() == 3, "Transaction should have 3 elements");

        Element e1 = tx.getElements().get(0);
        Element e2 = tx.getElements().get(1);
        Element e3 = tx.getElements().get(2);

        require("1".equals(e1.getItem()), "First element item should be '1'");
        require(Math.abs(e1.getUtility() - 1.0) < 0.0001, "First element utility should be 1.0");
        require("2".equals(e2.getItem()), "Second element item should be '2'");
        require(Math.abs(e2.getUtility() - 2.0) < 0.0001, "Second element utility should be 2.0");
        require("3".equals(e3.getItem()), "Third element item should be '3'");
        require(Math.abs(e3.getUtility() - 3.0) < 0.0001, "Third element utility should be 3.0");
        System.out.println("OK");
    }

    public static void testLegacyFormat() {
        System.out.print("3. Kiểm tra bóc tách định dạng Legacy Running Example (item:quantity)... ");
        EmptyAndCommentFilterHandler filter = new EmptyAndCommentFilterHandler();
        SpmfFormatHandler spmf = new SpmfFormatHandler();
        LegacyFormatHandler legacy = new LegacyFormatHandler();

        filter.setNext(spmf);
        spmf.setNext(legacy);

        TransactionParsingPipeline pipeline = new TransactionParsingPipeline(filter);

        String legacyLine = "a:2 b:3 c:1";
        Transaction tx = pipeline.parse(legacyLine, 99);

        require(tx != null, "Legacy line should be parsed to a Transaction");
        require(tx.getTid() == 99, "TID should be 99");
        require(tx.getElements().size() == 3, "Transaction should have 3 elements");

        Element e1 = tx.getElements().get(0);
        Element e2 = tx.getElements().get(1);
        Element e3 = tx.getElements().get(2);

        require("a".equals(e1.getItem()), "First element item should be 'a'");
        require(e1.getQuantity() == 2, "First element quantity should be 2");
        require("b".equals(e2.getItem()), "Second element item should be 'b'");
        require(e2.getQuantity() == 3, "Second element quantity should be 3");
        require("c".equals(e3.getItem()), "Third element item should be 'c'");
        require(e3.getQuantity() == 1, "Third element quantity should be 1");
        System.out.println("OK");
    }

    public static void testPipelineChainDelegation() {
        System.out.print("4. Kiểm tra sự chuyển tiếp (delegation) trong chuỗi Handler... ");
        final boolean[] firstHandled = {false};
        final boolean[] secondHandled = {false};

        TransactionLineHandler h1 = new TransactionLineHandler() {
            private TransactionLineHandler next;
            @Override
            public void setNext(TransactionLineHandler next) { this.next = next; }
            @Override
            public Transaction handle(String line, int tid) {
                firstHandled[0] = true;
                return next != null ? next.handle(line, tid) : null;
            }
        };

        TransactionLineHandler h2 = new TransactionLineHandler() {
            private TransactionLineHandler next;
            @Override
            public void setNext(TransactionLineHandler next) { this.next = next; }
            @Override
            public Transaction handle(String line, int tid) {
                secondHandled[0] = true;
                return new Transaction(tid);
            }
        };

        h1.setNext(h2);
        TransactionParsingPipeline pipeline = new TransactionParsingPipeline(h1);
        Transaction tx = pipeline.parse("sample", 1);

        require(tx != null, "Transaction should be created by h2");
        require(tx.getTid() == 1, "TID should be 1");
        require(firstHandled[0], "First handler should have executed");
        require(secondHandled[0], "Second handler should have executed");
        System.out.println("OK");
    }

    public static void testConveniencePipeline() {
        System.out.print("5. Kiểm tra pipeline với nhiều định dạng liên tiếp... ");
        TransactionParsingPipeline pipeline = new TransactionParsingPipeline(
                new EmptyAndCommentFilterHandler(),
                new SpmfFormatHandler(),
                new LegacyFormatHandler()
        );

        Transaction tx1 = pipeline.parse("x:1 y:2", 10);
        require(tx1 != null && tx1.getElements().size() == 2, "Legacy format parsed");

        Transaction tx2 = pipeline.parse("1 2 : 5.0 : 2.0 3.0", 20);
        require(tx2 != null && Math.abs(tx2.getTransactionUtility() - 5.0) < 0.0001, "SPMF format parsed");

        Transaction txComment = pipeline.parse("# comment", 30);
        require(txComment == null, "Comment line parsed to null");
        System.out.println("OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}