package org.example;

import lombok.AllArgsConstructor;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.join;


public class SingleThreadCrawler {

    public static void main(String[] args) throws Exception {
        SingleThreadCrawler crawler = new SingleThreadCrawler();
        long startTime = System.nanoTime();
        String result = crawler.find("Java_(programming_language)", "Cat", 5, TimeUnit.MINUTES);
        long finishTime = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

        System.out.println("Took "+finishTime+" seconds, result is: " + result);
    }
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(500);

    private final Queue<Runnable> tasks = new LinkedList<>();
    
    private final Set<String> visited = new HashSet<>();

    private final WikiClient client = new WikiClient();

    public String find(String from, String target, long timeout, TimeUnit timeUnit) throws Exception {
        long deadline = System.nanoTime() + timeUnit.toNanos(timeout);
        AtomicReference<Node> result = new AtomicReference<>();
				tasks.offer(getTask(target, new Node(from, null), result));
        while (!executorService.isShutdown()) {
            if (deadline < System.nanoTime()) {
                throw new TimeoutException();
            }
            Runnable poll = tasks.poll();
            if(poll != null){
                executorService.execute(poll);
            }
        }
        if (result.get() != null) {
            List<String> resultList = new ArrayList<>();
            Node search = result.get();
            while (true) {
                resultList.add(search.title);
                if (search.next == null) {
                    break;
                }
                search = search.next;
            }
            Collections.reverse(resultList);

            return join(" > ", resultList);
        }

        return "not found";
    }
    private Runnable getTask(String target, Node node, AtomicReference<Node> result) {
	    return () -> {
		    System.out.println("Get page: " + node.title);
		    Set<String> links = client.getByTitle(node.title);
		    for (String link : links) {
			    String currentLink = link.toLowerCase();
			    if (visited.contains(currentLink)) {
				    continue;
			    }
			    visited.add(currentLink);
			    Node subNode = new Node(link, node);
			    if (target.equalsIgnoreCase(currentLink)) {
				    result.set(subNode);
				    executorService.shutdownNow();
				    break;
			    }
			    tasks.offer(getTask(target, subNode, result));
		    }
	    };
    }


@AllArgsConstructor
    private static class Node {
        String title;
        Node next;
    }
}
