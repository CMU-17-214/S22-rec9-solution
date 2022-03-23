package edu.cmu.cs.cs214.rec09;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Main {
    private static final int NUM_REQUESTS = 100;
    private static final String URL_STR = "http://feature.isri.cmu.edu";
    private static HttpClient client = HttpClient.newHttpClient();

    private static void runWebAPIRequest() throws IOException, InterruptedException {
        // read the request body
        String bodyStr = new String(Files.readAllBytes(Paths.get("src/main/resources/request-body.json")));
        String key = ""; // TODO: fill in your key here
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("https://api.clarifai.com/v2/models/bd367be194cf45149e75f01d59f77ba7/outputs"))
                .header("Authorization", "Key " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }

    private static void runMultipleSynchronous() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(URL_STR))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        Instant start = Instant.now();
        List<HttpResponse<String>> results = new ArrayList<>(100);
        for (int i = 0; i < NUM_REQUESTS; i++) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            results.add(response);
//            System.out.println(response.body());
        }
        System.out.println("Total time sync (ms): " + Duration.between(start, Instant.now()).toMillis());
    }

    private static void runSingleAsync() throws IOException, InterruptedException, ExecutionException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(URL_STR))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        client = HttpClient.newBuilder().executor(executorService).build();

        Instant start = Instant.now();

        CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        responseFuture.thenRun(() -> System.out.println("do other things after finished..."));
        System.out.println("do other things...");
        HttpResponse<String> response = responseFuture.join();
//        System.out.println("The response is:" + response.body());

        System.out.println("Total time single async (ms): " + Duration.between(start, Instant.now()).toMillis());
    }

    private static void runMultipleAsynchronous() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(URL_STR))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        client = HttpClient.newBuilder().executor(executorService).build();
        List<CompletableFuture<HttpResponse<String>>> allFutures = new ArrayList<>(100);
        Semaphore parallelRequestCounter = new Semaphore(10);

        Instant start = Instant.now();

        for (int i = 0; i < NUM_REQUESTS; i++) {
            parallelRequestCounter.acquire();
            CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
//            responseFuture.thenAccept(System.out::println);
            responseFuture.thenRun(() -> parallelRequestCounter.release());
            allFutures.add(responseFuture);
        }

        List<HttpResponse<String>> results = new ArrayList<>(100);
        for (CompletableFuture<HttpResponse<String>> future : allFutures) {
            results.add(future.join());
        }

        System.out.println("Total time multiple async (ms): " + Duration.between(start, Instant.now()).toMillis());
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        // Task 1
//        runWebAPIRequest();
        // Task 2
        runMultipleSynchronous();
//        runSingleAsync();
        runMultipleAsynchronous();

        System.exit(0);
    }
}
