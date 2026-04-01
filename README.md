# Aurora Commerce AI

Aurora Commerce AI is a portfolio-ready e-commerce experience built with pure Java and a cinematic frontend. It is designed to feel like a premium product demo instead of a basic CRUD shop.

## Highlights

- Java 25 backend using `HttpServer` from the standard library
- Live API-driven storefront with products, reviews, and metrics
- AI-inspired features:
  - shopper persona recommendation engine
  - explainable bundle intelligence
  - next-best-action upsell suggestions
- Bold editorial UI for a stronger portfolio presentation

## Run locally

From the project folder:

```bash
mkdir -p out
javac -d out src/com/jeshwin/auroracommerce/AuroraCommerceServer.java
java -cp out com.jeshwin.auroracommerce.AuroraCommerceServer
```

Then open [http://localhost:8080](http://localhost:8080).

## Project structure

- `src/com/jeshwin/auroracommerce/AuroraCommerceServer.java`: backend server and AI logic
- `web/index.html`: storefront structure
- `web/styles.css`: premium visual styling and responsive layout
- `web/app.js`: frontend behavior and API integration

## Portfolio talking points

- Shows you can build a polished full-stack experience in Java without depending on heavy frameworks.
- Demonstrates product thinking, not just coding: the AI features explain their reasoning to the shopper.
- Gives interviewers something interactive to explore, which is much stronger than a static landing page.
