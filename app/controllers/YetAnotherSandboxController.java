package controllers;

import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.F.Function0;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class YetAnotherSandboxController extends Controller {
	
	private static final Logger logger = LoggerFactory.getLogger(YetAnotherSandboxController.class);

    @BodyParser.Of(BodyParser.Json.class)
	public static Result syncWork(){
    	JsonNode json = request().body().asJson();
    	int workDuration = json.get("workduration").asInt();

    	try {
    		 longComputation(workDuration);
		} catch (InterruptedException e) {
			logger.warn("Interrupted!" , e);
			return ok("Thread is interuptted!");
		}
		return ok("Work for " + workDuration + " seconds");
	}

    @BodyParser.Of(BodyParser.Json.class)
	public static Promise<Result> asyncWork(){
    	JsonNode json = request().body().asJson();
    	int workDuration = json.get("workduration").asInt();
    	
    	return Promise.promise(new LongComputation(workDuration));
	}
    
    @BodyParser.Of(BodyParser.Json.class)
    public static Result syncAck(){
    	JsonNode json = request().body().asJson();
    	int workDuration = json.get("workduration").asInt();
    	try {
   		 longComputation(workDuration);
		} catch (InterruptedException e) {
			logger.warn("Interrupted!" , e);
			return ok("Thread is interuptted!");
		}
    	
    	return ok("Work for " + workDuration + " seconds synchronously");
    }
    
    @BodyParser.Of(BodyParser.Json.class)
    public static Result asyncAck(){
    	JsonNode json = request().body().asJson();
    	int workDuration = json.get("workduration").asInt();
    	
		Akka.system()
				.scheduler()
				.scheduleOnce(Duration.Zero(),
						new LongComputationRunnable(workDuration),
						Akka.system().dispatcher());
    	return ok("Work in progress for asyncAck\n");
    }
    
    public static int longComputation(int workDuration) throws InterruptedException{
    	logger.info("Long computation for {} seconds", workDuration);
    	TimeUnit.SECONDS.sleep(workDuration);
    	logger.info("Finish long computation for {} seconds", workDuration);
    	return workDuration;
    }
    
    private static class LongComputation implements Function0<Result>{

    	private final int workDuration;
    	private final static Logger longComputationLogger = LoggerFactory.getLogger(LongComputation.class);
    	private final static String formattedResult = "Finish working for %d seconds";
    	
    	LongComputation(int workDuration){
    		this.workDuration = workDuration;
    	}
    	
		@Override
		public Result apply() throws Throwable {
			longComputationLogger.info("Start working for {} seconds", workDuration);
			TimeUnit.SECONDS.sleep(workDuration);
			longComputationLogger.info("Finish working for {} seconds", workDuration);
			return ok(String.format(formattedResult, workDuration));
		}
    }
    
    private static class LongComputationRunnable implements Runnable{
    	
    	private final int workDuration;
    	private final static Logger longComputationRunnableLogger = LoggerFactory.getLogger(LongComputationRunnable.class);
    	
    	LongComputationRunnable(int workDuration){
    		this.workDuration = workDuration;
    	}
    	
    	@Override
    	public void run() {
    		longComputationRunnableLogger.info("Start working for {} seconds", workDuration);
    		try {
				TimeUnit.SECONDS.sleep(workDuration);
			} catch (InterruptedException e) {
				longComputationRunnableLogger.warn("Thread is interrupted");
				return;
			}
    		longComputationRunnableLogger.info("Finish working for {} seconds", workDuration);
    	}
    }

}
