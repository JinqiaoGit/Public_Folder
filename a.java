import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

public class PersonServlet extends HttpServlet {
	
	private static final String API_NAME = "PersonAPI";
	private static final int THROTTLE_LIMIT = 10; //10 requests per second
	private static final int RATE_LIMIT = 100; //100 requests per second
	
	//Circuit breaker configuration
	private static final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
	        .failureRateThreshold(50)
	        .waitDurationInOpenState(java.time.Duration.ofMillis(1000))
	        .permittedNumberOfCallsInHalfOpenState(2)
	        .slidingWindowSize(5)
	        .minimumNumberOfCalls(5)
	        .build();
	private static final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
	
	//Rate limiter configuration
	private static final RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
	        .limitForPeriod(RATE_LIMIT)
	        .limitRefreshPeriod(java.time.Duration.ofSeconds(1))
	        .timeoutDuration(java.time.Duration.ofMillis(100))
	        .build();
	private static final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(rateLimiterConfig);
	
	//Time limiter configuration
	private static final TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(java.time.Duration.ofSeconds(5))
            .cancelRunningFuture(true)
            .build();
	private static final TimeLimiter timeLimiter = TimeLimiter.of(timeLimiterConfig);
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
		//Check API throttle limit
		RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(API_NAME);
		try {
			rateLimiter.acquirePermission();
		} catch (RequestNotPermitted ex) {
			response.sendError(429, "Too Many Requests");
			return;
		}

		String name = request.getParameter("name");
		String about = request.getParameter("about");
		int birthYear = Integer.parseInt(request.getParameter("birthYear"));
		
		//Code to validate input and throw exception in case of invalid input.
		if(name == null || name.isEmpty() || about == null || about.isEmpty() || birthYear < 0){
			response.sendError(400, "Invalid Input Parameters");
			return;
		}

		try {
			Person person = DataStore.getInstance().putPerson(new Person(name, about, birthYear)); //Assuming password is not required for Person creation.
			JSONObject json = timeLimiter.decorateSupplier(() -> createJSON(person));
			response.getOutputStream().println(json.toString());
		} catch (Exception ex) {
			response.sendError(500, "Internal Server Error");
		}
	}
	
	private JSONObject createJSON(Person person){
		JSONObject json = new JSONObject();
		json.put("name", person.getName());
		json.put("about", person.getAbout());
		json.put("birthYear", person.getBirthYear());
		return json;
	}
}