package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = false;

	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

			if (localTest) {
				CertainBookStore store = new CertainBookStore();
				storeManager = store;
				client = store;
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}
	
	
	/**
	 * Buys one copy of the default book and tests if book counter is decremented properly
	 * 
	 * @throws BookStoreException
	 */
	@Test
	public void testBuyOneCopyOfDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		// Add one book, buy one copy of it.
		booksToBuy.add(new BookCopy(TEST_ISBN, 1));
		
		// Try to buy the book
		client.buyBooks(booksToBuy);
		
		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		
		assertTrue(bookInList.getNumCopies() == (NUM_COPIES - 1));
	}
	
	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}
	
	/**
	 * Tests if one book can be rated
	 * 
	 * @throws BookStoreException
	 */
	@Test
	public void testRateBooksOneRating() throws BookStoreException {
		Set<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(TEST_ISBN, 5));
		
		client.rateBooks(booksToRate);
		
		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		
		assertTrue(bookInList.getTotalRating() == 5 
				&& bookInList.getNumTimesRated() == 1
				&& Math.abs(bookInList.getAverageRating() - 5) < 0.001);
	}
	
	/**
	 * Tests if two books can be rated and if the average gets cal
	 * 
	 * @throws BookStoreException
	 */
	@Test 
	public void testRateBooksTwoRatings() throws BookStoreException {
		Set<BookRating> booksToRate1 = new HashSet<BookRating>();
		Set<BookRating> booksToRate2 = new HashSet<BookRating>();
		booksToRate1.add(new BookRating(TEST_ISBN, 5));
		booksToRate2.add(new BookRating(TEST_ISBN, 2));
		
		client.rateBooks(booksToRate1);
		client.rateBooks(booksToRate2);
		
		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		
		assertTrue(bookInList.getTotalRating() == 7 
				&& bookInList.getNumTimesRated() == 2 
				&& Math.abs(bookInList.getAverageRating() - 3.5) < 0.001); // To prevent rounding errors.
	}
	
	/**
	 * Tests if invalid isbn's are caught
	 * 
	 * @throws BookStoreException
	 */
	 @Test
	 public void testRateBooksInvalidISBN() throws BookStoreException {
		 Set<BookRating> booksToRate = new HashSet<BookRating>();
		 booksToRate.add(new BookRating(TEST_ISBN, 1)); // valid
		 booksToRate.add(new BookRating(-1, 1)); // invalid
		 
		 try{
			 client.rateBooks(booksToRate);
			 fail();
		 } catch(BookStoreException ex){
			 ;
		 }
		 
		 List<StockBook> listBooks = storeManager.getBooks();
		 assertTrue(listBooks.size() == 1);
		 StockBook bookInList = listBooks.get(0);
		 
		 assertTrue(bookInList.getTotalRating() == 0
				 && bookInList.getNumTimesRated() == 0);
	 }
	 
	 /**
	  * Tests if negative ratings are caught
	  * 
	  * @throws BookStoreException
	  */
	 @Test 
	 public void testRateBooksNegativeRating() throws BookStoreException{
		 addBooks(12345, 1);
		 Set<BookRating> booksToRate = new HashSet<BookRating>();
		 booksToRate.add(new BookRating(TEST_ISBN, 5)); // Valid
		 booksToRate.add(new BookRating(12345, -1)); // Invalid

		 
		 try{
			 client.rateBooks(booksToRate);
			 fail();
		 } catch (BookStoreException ex){
			 ;
		 }
		 
		 List<StockBook> listBooks = storeManager.getBooks();
		 StockBook bookInList = listBooks.get(0);
		 
		 assertTrue(bookInList.getNumTimesRated() == 0
				 && bookInList.getTotalRating() == 0);
	 }
	 
	 @Test
	 public void testRateBooksNullInput() throws BookStoreException{
		 try{
			 client.rateBooks(null);
			 fail();
		 } catch (BookStoreException ex){
			 ;
		 }
		 
		 List<StockBook> listBooks = storeManager.getBooks();
		 StockBook bookInList = listBooks.get(0);
		 
		 assertTrue(bookInList.getNumTimesRated() == 0
				 && bookInList.getTotalRating() == 0);
	 }
	 
	 @Test
	 public void testGetTopRatedBooks() throws BookStoreException {
		 // Add another book
		 addBooks(123456, 1);
		
		 // Rate TEST_ISBN and 123456
		 Set<BookRating> booksToRate = new HashSet<BookRating>();
		 booksToRate.add(new BookRating(123456, 4));
		 booksToRate.add(new BookRating(TEST_ISBN, 5));
		 
		 client.rateBooks(booksToRate);
		 
		 List<Book> topRatedBooks = client.getTopRatedBooks(1);
		 Book bookInList = topRatedBooks.get(0);
		 StockBook addedBook = getDefaultBook();
		 
		 assertTrue(bookInList.getISBN() == addedBook.getISBN());
	 }
	 
	 @Test
	 public void testGetTopRatedBooksTwo() throws BookStoreException {
		// Add another book
		addBooks(123456, 1);
		addBooks(123457, 1);
				
				 // Rate TEST_ISBN and 123456
		Set<BookRating> booksToRate = new HashSet<BookRating>();
		booksToRate.add(new BookRating(123456, 4));
		booksToRate.add(new BookRating(TEST_ISBN, 5));
		client.rateBooks(booksToRate);
				 
		List<Book> topRatedBooks = client.getTopRatedBooks(2);
		assertTrue(topRatedBooks.size() == 2);
		
		Book bookInList1 = topRatedBooks.get(0);
		Book bookInList2 = topRatedBooks.get(1);
		
		StockBook addedBook = getDefaultBook();
				 
		assertTrue(bookInList1.getISBN() == addedBook.getISBN()
				&& bookInList2.getISBN() == 123456);
	 }
	 
	 @Test
	 public void testGetTopRatedBooksNegativeNumber() throws BookStoreException {
		try{
			client.getTopRatedBooks(-1);
			fail();
		} catch(BookStoreException ex){
			;
		}
		
		List<StockBook> listBooks = storeManager.getBooks();
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();
		 
		assertTrue(bookInList.getISBN() == addedBook.getISBN());
		
	 }
	 
	 @Test
	 public void testGetTopRatedBooksZero() throws BookStoreException {
		 
		 try{
			 client.getTopRatedBooks(0);
			 fail();
		 } catch(BookStoreException ex){
			 ;
		 }
		 
		 List<StockBook> listBooks = storeManager.getBooks();
		 StockBook bookInList = listBooks.get(0);
		 StockBook addedBook = getDefaultBook();
			 
		 assertTrue(bookInList.getISBN() == addedBook.getISBN());
	 }
	 
	 

	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
