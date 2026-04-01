package com.jeshwin.auroracommerce;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class AuroraCommerceServer {
    private static final int PORT = 8080;
    private static final Path WEB_ROOT = Path.of("web");
    private static final List<Product> PRODUCTS = List.of(
            new Product("PULSE-X1", "Pulse X1 Adaptive Jacket", "Outerwear", 249,
                    "Climate-reactive shell with modular glow seams and commuting pockets.",
                    List.of("performance", "commuter", "techwear"), 96, 88, 4.9,
                    "#7CFFB2", "#0C1222"),
            new Product("NOVA-RUN", "Nova Run Knit Sneakers", "Footwear", 179,
                    "Featherlight neural-knit runners built for city sprints and long loops.",
                    List.of("running", "comfort", "urban"), 93, 80, 4.8,
                    "#FFD166", "#101522"),
            new Product("LUMA-TOTE", "Luma Smart Carry Tote", "Accessories", 139,
                    "Minimal tote with device sleeve, quick-release loops, and anti-scratch lining.",
                    List.of("minimal", "work", "creator"), 91, 85, 4.7,
                    "#9BE7FF", "#0F172A"),
            new Product("ECHO-FRM", "Echo Frame Audio Glasses", "Wearables", 219,
                    "Directional audio glasses for hands-free playlists, prompts, and calls.",
                    List.of("audio", "future", "lifestyle"), 89, 73, 4.6,
                    "#FF8DA1", "#1A1026"),
            new Product("SOL-MAT", "SolMat Recovery Set", "Wellness", 119,
                    "Travel-ready recovery mat paired with posture bands and guided routines.",
                    List.of("wellness", "recovery", "movement"), 87, 90, 4.8,
                    "#F7FF7C", "#1A1F12"),
            new Product("AETHER-DESK", "Aether Desk Dock", "Workspace", 159,
                    "Desk dock with ambient light ring, cable routing, and modular trays.",
                    List.of("workspace", "focus", "design"), 90, 92, 4.9,
                    "#B794F4", "#161125"),
            new Product("VANTA-PACK", "Vanta Travel Pack", "Bags", 199,
                    "Expandable travel pack with hidden passport panel and magnetic organizer pods.",
                    List.of("travel", "adventure", "utility"), 94, 87, 4.8,
                    "#8EF6E4", "#081A1E"),
            new Product("ARIA-LAMP", "Aria Aura Lamp", "Home", 129,
                    "Mood lamp that shifts scenes from focus blue to golden unwind gradients.",
                    List.of("home", "lighting", "mood"), 86, 95, 4.7,
                    "#F6AD55", "#20140D")
    );
    private static final List<Review> REVIEWS = List.of(
            new Review("Mia", "Feels like a fashion brand built by product designers.", 5),
            new Review("Ethan", "The recommendation engine actually understood my commute-heavy lifestyle.", 5),
            new Review("Sofia", "The bundle suggestions made the site memorable during my interview demo.", 5)
    );
    private static final List<String> PERSONAS = List.of("Urban Athlete", "Creator Minimalist", "Frequent Traveler", "Wellness Hacker");

    private AuroraCommerceServer() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new StaticFileHandler("index.html", "text/html; charset=utf-8"));
        server.createContext("/styles.css", new StaticFileHandler("styles.css", "text/css; charset=utf-8"));
        server.createContext("/app.js", new StaticFileHandler("app.js", "application/javascript; charset=utf-8"));
        server.createContext("/api/storefront", AuroraCommerceServer::handleStorefront);
        server.createContext("/api/ai/recommendations", AuroraCommerceServer::handleRecommendations);
        server.createContext("/api/ai/cart-insight", AuroraCommerceServer::handleCartInsight);
        server.setExecutor(Executors.newFixedThreadPool(8));

        System.out.println("Aurora Commerce AI is live on http://localhost:" + PORT);
        server.start();
    }

    private static void handleStorefront(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"brand\":\"Aurora Commerce AI\",");
        json.append("\"tagline\":\"Luxury e-commerce experience with explainable AI merchandising.\",");
        json.append("\"metrics\":{");
        json.append("\"conversionLift\":\"+18%\",");
        json.append("\"avgBasket\":\"$312\",");
        json.append("\"returnRisk\":\"-11%\",");
        json.append("\"aiConfidence\":\"94%\"");
        json.append("},");
        json.append("\"personas\":").append(toJsonStringArray(PERSONAS)).append(",");
        json.append("\"products\":").append(toJsonArray(PRODUCTS.stream().map(Product::toJson).toList())).append(",");
        json.append("\"reviews\":").append(toJsonArray(REVIEWS.stream().map(Review::toJson).toList()));
        json.append("}");

        sendJson(exchange, 200, json.toString());
    }

    private static void handleRecommendations(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String persona = query.getOrDefault("persona", "Creator Minimalist");
        String mood = query.getOrDefault("mood", "focused");
        int budget = parseInt(query.get("budget"), 320);

        List<Product> recommended = PRODUCTS.stream()
                .sorted(
                        Comparator.comparingInt((Product product) -> matchScore(product, persona, mood, budget)).reversed()
                                .thenComparing(Comparator.comparingDouble(Product::rating).reversed())
                )
                .limit(3)
                .toList();

        String explanation = buildExplanation(persona, mood, budget, recommended);
        String json = "{"
                + "\"persona\":\"" + escape(persona) + "\","
                + "\"mood\":\"" + escape(mood) + "\","
                + "\"budget\":" + budget + ","
                + "\"explanation\":\"" + escape(explanation) + "\","
                + "\"items\":" + toJsonArray(recommended.stream().map(Product::toJson).toList())
                + "}";
        sendJson(exchange, 200, json);
    }

    private static void handleCartInsight(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI());
        Set<String> itemIds = Arrays.stream(query.getOrDefault("items", "").split(","))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        List<Product> cart = PRODUCTS.stream()
                .filter(product -> itemIds.contains(product.id()))
                .toList();

        int total = cart.stream().mapToInt(Product::price).sum();
        int synergy = cart.isEmpty() ? 0 : (int) Math.round(cart.stream().mapToInt(Product::storyScore).average().orElse(0));
        String signal = cartInsight(cart, total);
        Optional<Product> addOn = PRODUCTS.stream()
                .filter(product -> !itemIds.contains(product.id()))
                .max(Comparator.comparingInt(product -> cartBundleScore(cart, product)));

        String json = "{"
                + "\"itemCount\":" + cart.size() + ","
                + "\"cartTotal\":" + total + ","
                + "\"synergyScore\":" + synergy + ","
                + "\"signal\":\"" + escape(signal) + "\","
                + "\"nextBestAction\":" + addOn.map(Product::toJson).orElse("null")
                + "}";
        sendJson(exchange, 200, json);
    }

    private static int matchScore(Product product, String persona, String mood, int budget) {
        int score = product.storyScore() + product.utilityScore();
        String personaText = persona.toLowerCase(Locale.ROOT);
        String moodText = mood.toLowerCase(Locale.ROOT);

        for (String tag : product.tags()) {
            if (personaText.contains("traveler") && (tag.equals("travel") || tag.equals("utility"))) {
                score += 18;
            }
            if (personaText.contains("minimalist") && (tag.equals("minimal") || tag.equals("workspace") || tag.equals("creator"))) {
                score += 17;
            }
            if (personaText.contains("athlete") && (tag.equals("running") || tag.equals("performance") || tag.equals("recovery"))) {
                score += 17;
            }
            if (personaText.contains("wellness") && (tag.equals("wellness") || tag.equals("recovery") || tag.equals("mood"))) {
                score += 16;
            }
            if (moodText.contains("bold") && (tag.equals("future") || tag.equals("audio") || tag.equals("techwear"))) {
                score += 12;
            }
            if (moodText.contains("focused") && (tag.equals("workspace") || tag.equals("minimal") || tag.equals("comfort"))) {
                score += 12;
            }
            if (moodText.contains("escape") && (tag.equals("travel") || tag.equals("adventure") || tag.equals("home"))) {
                score += 12;
            }
        }

        if (product.price() <= budget) {
            score += 20;
        } else {
            score -= Math.min(20, product.price() - budget);
        }

        return score;
    }

    private static String buildExplanation(String persona, String mood, int budget, List<Product> products) {
        String names = products.stream().map(Product::name).collect(Collectors.joining(", "));
        return "The concierge prioritized " + names + " because the " + persona
                + " persona leans toward high-utility pieces, your " + mood
                + " mood suggests stronger lifestyle alignment, and the bundle keeps the basket near $" + budget + ".";
    }

    private static String cartInsight(List<Product> cart, int total) {
        if (cart.isEmpty()) {
            return "Start with one hero product and Aurora will model the rest of the story around it.";
        }

        boolean commute = cart.stream().flatMap(product -> product.tags().stream()).anyMatch(tag -> tag.equals("commuter") || tag.equals("workspace"));
        boolean travel = cart.stream().flatMap(product -> product.tags().stream()).anyMatch(tag -> tag.equals("travel") || tag.equals("adventure"));
        boolean wellness = cart.stream().flatMap(product -> product.tags().stream()).anyMatch(tag -> tag.equals("wellness") || tag.equals("recovery"));

        if (commute && travel) {
            return "Your cart reads like a mobile-creator setup: strong for work trips, daily carry, and airport transitions.";
        }
        if (wellness && total < 300) {
            return "This basket is optimized for recovery-first shoppers; one premium hero item could raise perceived value without feeling random.";
        }
        if (total > 400) {
            return "Aurora flags this as a high-intent bundle with premium tolerance. Surface white-glove shipping and limited-edition scarcity next.";
        }
        return "The mix is balanced, but adding one complementary product would sharpen the narrative and improve cross-category cohesion.";
    }

    private static int cartBundleScore(List<Product> cart, Product candidate) {
        if (cart.isEmpty()) {
            return candidate.storyScore() + candidate.utilityScore();
        }

        int score = candidate.storyScore();
        for (Product existing : cart) {
            for (String tag : existing.tags()) {
                if (candidate.tags().contains(tag)) {
                    score += 14;
                }
            }
        }
        return score + candidate.utilityScore();
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> values = new LinkedHashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return values;
        }

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = urlDecode(parts[0]);
            String value = parts.length > 1 ? urlDecode(parts[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        sendBytes(exchange, status, "application/json; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendBytes(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private static String toJsonArray(List<String> values) {
        return values.stream().collect(Collectors.joining(",", "[", "]"));
    }

    private static String toJsonStringArray(List<String> values) {
        return values.stream()
                .map(value -> "\"" + escape(value) + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private record Product(String id, String name, String category, int price, String description, List<String> tags,
                           int storyScore, int utilityScore, double rating, String accent, String background) {
        String toJson() {
            return "{"
                    + "\"id\":\"" + escape(id) + "\","
                    + "\"name\":\"" + escape(name) + "\","
                    + "\"category\":\"" + escape(category) + "\","
                    + "\"price\":" + price + ","
                    + "\"description\":\"" + escape(description) + "\","
                    + "\"tags\":" + toJsonStringArray(tags) + ","
                    + "\"storyScore\":" + storyScore + ","
                    + "\"utilityScore\":" + utilityScore + ","
                    + "\"rating\":" + rating + ","
                    + "\"accent\":\"" + escape(accent) + "\","
                    + "\"background\":\"" + escape(background) + "\","
                    + "\"launchDate\":\"" + LocalDate.now().minusDays(utilityScore % 17L) + "\""
                    + "}";
        }
    }

    private record Review(String author, String quote, int stars) {
        String toJson() {
            return "{"
                    + "\"author\":\"" + escape(author) + "\","
                    + "\"quote\":\"" + escape(quote) + "\","
                    + "\"stars\":" + stars
                    + "}";
        }
    }

    private static final class StaticFileHandler implements HttpHandler {
        private final String fileName;
        private final String contentType;

        private StaticFileHandler(String fileName, String contentType) {
            this.fileName = fileName;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Path path = WEB_ROOT.resolve(fileName);
            if (!Files.exists(path)) {
                sendBytes(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
                return;
            }
            sendBytes(exchange, 200, contentType, Files.readAllBytes(path));
        }
    }
}
