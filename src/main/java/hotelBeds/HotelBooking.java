package hotelBeds;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public class HotelBooking 
{	
	
	File jsonFileAvailability = new File("testData/availability.json");
	String rateKeyOfFirstHotel;
	
			
	int firstmarker=0;
	@Test
	void hotelBookingOperations() throws IOException
	{		
		
		// Signature is generated by SHA256 (Api-Key + Secret + Timestamp (in seconds))
		String apiKey = "hmxem4ev6tgf8zvxqyrgjvpe";
		String Secret = "TAyGhGd8WN";
		String signature = org.apache.commons.codec.digest.DigestUtils.sha256Hex(apiKey + Secret + System.currentTimeMillis() / 1000);
		
		RestAssured.baseURI ="https://api.test.hotelbeds.com";
		
		//To check whether the API is correctly configured
		Response getStatusResponse = RestAssured
									.given()
									.header("API-key", apiKey)
									.header("X-Signature", signature)
									.header("Accept", "application/json")
									.get("/hotel-api/1.0/status");
		
		if(getStatusResponse.getStatusCode()==200)
			System.out.println("API Configuration is done successfully!!");
		else
			System.err.println("API Configuration Failed!!");
		
		
		System.out.println("*********************************************************************");
		
		Response postAvailabilityResponse = RestAssured
											.given()
											.header("API-key", apiKey)
											.header("X-Signature", signature)
											.header("Accept", "application/json")
											.header("Accept-Encoding", "gzip")
											.header("Content-Type", "application/json")
											.body(jsonFileAvailability)
											.post("/hotel-api/1.0/hotels");
		
		
		JsonPath postAvailabilityResponseJson = postAvailabilityResponse.jsonPath();
		List<String> allHotels = postAvailabilityResponseJson.getList("hotels.hotels");
		System.out.println("1) The top 10 available hotels and their rates are given below:");
		int number=10;
		if(number>allHotels.size())
			number=allHotels.size();
		for(int i=0;i<=number;i++)
		{
			System.out.println("________________________________________________________");
			System.out.println("The hotel name:"+postAvailabilityResponseJson.getString("hotels.hotels["+i+"].name"));
			System.out.println("The selling rate:"+postAvailabilityResponseJson.getString("hotels.hotels["+i+"].rooms[0].rates[0].sellingRate"));
			if(postAvailabilityResponseJson.getString("hotels.hotels["+i+"].rooms[0].rates[0].rateType").equals("BOOKABLE") && firstmarker==0 )
			{
				rateKeyOfFirstHotel = postAvailabilityResponseJson.getString("hotels.hotels["+i+"].rooms[0].rates[0].rateKey");
				firstmarker++;
			}	
		}
		
		System.out.println("*********************************************************************");
		
		System.out.println("2) Book the room in the first hotel having rateType=BOOKABLE in the response");
		String jsonStringBooking ="{\r\n" + 
				"  \"holder\": {\r\n" + 
				"    \"name\": \"Lionel\",\r\n" + 
				"    \"surname\": \"Messi\"\r\n" + 
				"  },\r\n" + 
				"  \"rooms\": [\r\n" + 
				"    {\r\n" + 
				"      \"rateKey\": \""+rateKeyOfFirstHotel+"\"\r\n" + 
				"   	}\r\n" + 
				"  ],\r\n" + 
				"  \"clientReference\": \"IntegrationAgency\",\r\n" + 
				"  \"remark\": \"This is a test Booking done from Rest Assured\",\r\n" + 
				"  \"tolerance\": 2\r\n" + 
				"}";
		
		Response postBookingResponse = RestAssured
										.given()
										.header("API-key", apiKey)
										.header("X-Signature", signature)
										.header("Accept", "application/json")
										.header("Accept-Encoding", "gzip")
										.header("Content-Type", "application/json")
										.body(jsonStringBooking)
										.post("/hotel-api/1.0/bookings");

		JsonPath postBookingResponseJson = postBookingResponse.jsonPath();
		String bookingId = postBookingResponseJson.getString("booking.reference");
		
		if(getStatusResponse.getStatusCode()==200)
			System.out.println("Booking done successfully and Booking Id is: "+bookingId);
		else
			System.err.println("API Configuration Failed!!");
		
		System.out.println("*********************************************************************");
		
		System.out.println("3) Getting the Booking details of the recent Booking");
		
		Response getBookingDetailResponse = RestAssured
				.given()
				.header("API-key", apiKey)
				.header("X-Signature", signature)
				.header("Accept", "application/json")
				.header("Accept-Encoding", "gzip")
				.header("Content-Type", "application/json")
				.get("/hotel-api/1.0/bookings/"+bookingId);
		
		getBookingDetailResponse.prettyPrint();
		
		System.out.println("*********************************************************************");
		
		System.out.println("4) Delete the Booking recent Booking");
		Response deleteBookingResponse = RestAssured
				.given()
				.header("API-key", apiKey)
				.header("X-Signature", signature)
				.header("Accept", "application/json")
				.header("Accept-Encoding", "gzip")
				.header("Content-Type", "application/json")
				.param("cancellationFlag","CANCELLATION")
				.when()
				.delete("/hotel-api/1.0/bookings/"+bookingId);

		if(deleteBookingResponse.getStatusCode()==200)
			System.out.println("The booking with Id: "+bookingId+" has been successfully deleted!!");
		else if(deleteBookingResponse.getStatusCode()==400)
			System.out.println("The booking with Id: "+bookingId+" cannot be deleted as this hotel does not allow cancellations!!");
		else
			System.err.println("The booking with Id: "+bookingId+" has not deleted!!");
		
		System.out.println("*********************************************************************");
	
	}
}
