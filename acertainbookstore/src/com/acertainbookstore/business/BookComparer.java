/**
 * 
 */
package com.acertainbookstore.business;

import java.util.Comparator;

/**
 * @author Simon
 *
 */
public class BookComparer implements Comparator<BookStoreBook> {
	public enum Order {TotalRating, AverageRating, NumOfRatings}
	
	private Order sortingBy = Order.AverageRating;
	
	@Override
	public int compare(BookStoreBook book1, BookStoreBook book2){
		switch(sortingBy){
		case AverageRating: return compareFloat(book1.getAverageRating(), book2.getAverageRating());
		case TotalRating : return compareFloat(book1.getTotalRating(), book2.getTotalRating());
		case NumOfRatings : return compareFloat(book1.getNumTimesRated(), book2.getNumTimesRated());
		default:
			throw new RuntimeException("Something really weird went down. THIS IS UNREACHABLE! :O");
		}
	}
	
	private int compareFloat(float f1, float f2){
		if(f1 - f2 < 0.000000001) return 0;
		if(f1 < f2) return -1;
		return 1;
	}
}
