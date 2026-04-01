const state = {
  products: [],
  cart: []
};

const currency = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0
});

async function loadStorefront() {
  const response = await fetch("/api/storefront");
  const data = await response.json();

  renderMetrics(data.metrics);
  renderPersonas(data.personas);
  renderCatalog(data.products);
  renderReviews(data.reviews);
  state.products = data.products;
  state.cart = [data.products[0].id, data.products[6].id];
  renderCart();
  await fetchRecommendations();
}

function renderMetrics(metrics) {
  const entries = [
    ["Conversion Lift", metrics.conversionLift],
    ["Avg Basket", metrics.avgBasket],
    ["Return Risk", metrics.returnRisk],
    ["AI Confidence", metrics.aiConfidence]
  ];

  document.getElementById("metrics").innerHTML = entries
    .map(([label, value]) => `
      <div class="metric">
        <strong>${value}</strong>
        <span>${label}</span>
      </div>
    `)
    .join("");
}

function renderPersonas(personas) {
  const personaSelect = document.getElementById("persona");
  personaSelect.innerHTML = personas
    .map((persona) => `<option value="${persona}">${persona}</option>`)
    .join("");
}

function renderCatalog(products) {
  const grid = document.getElementById("catalog-grid");
  grid.innerHTML = products
    .map((product) => `
      <article class="product-card" style="background:
        radial-gradient(circle at top right, ${product.accent}22, transparent 32%),
        linear-gradient(180deg, ${product.background}, rgba(7, 17, 31, 0.95));">
        <div class="product-top">
          <div class="product-badge-row">
            <span class="badge">${product.category}</span>
            <span class="badge">Rating ${product.rating}</span>
          </div>
          <div>
            <h3>${product.name}</h3>
            <p>${product.description}</p>
          </div>
          <div class="tag-row">
            ${product.tags.map((tag) => `<span class="tag">${tag}</span>`).join("")}
          </div>
        </div>
        <div class="product-actions">
          <div>
            <strong class="price">${currency.format(product.price)}</strong>
            <p>Story score ${product.storyScore}</p>
          </div>
          <button class="button primary" data-product-id="${product.id}">Add to cart</button>
        </div>
      </article>
    `)
    .join("");

  grid.querySelectorAll("button[data-product-id]").forEach((button) => {
    button.addEventListener("click", () => {
      state.cart.push(button.dataset.productId);
      renderCart();
    });
  });
}

function renderReviews(reviews) {
  document.getElementById("reviews").innerHTML = reviews
    .map((review) => `
      <article class="review-card">
        <p class="eyebrow">Client reaction</p>
        <h3>${review.author}</h3>
        <p>"${review.quote}"</p>
        <strong>${"★".repeat(review.stars)}</strong>
      </article>
    `)
    .join("");
}

async function fetchRecommendations() {
  const persona = document.getElementById("persona").value;
  const mood = document.getElementById("mood").value;
  const budget = document.getElementById("budget").value;
  const response = await fetch(`/api/ai/recommendations?persona=${encodeURIComponent(persona)}&mood=${encodeURIComponent(mood)}&budget=${budget}`);
  const data = await response.json();

  const panel = document.getElementById("recommendation-panel");
  panel.querySelector("h3").textContent = `${data.persona} | ${data.mood} mode`;
  panel.querySelector(".muted").textContent = data.explanation;
  document.getElementById("recommendation-list").innerHTML = data.items
    .map((item) => `
      <div class="recommendation-item">
        <strong>${item.name}</strong>
        <p>${item.description}</p>
        <span>${currency.format(item.price)}</span>
      </div>
    `)
    .join("");
}

async function renderCart() {
  const cartProducts = state.cart
    .map((id) => state.products.find((product) => product.id === id))
    .filter(Boolean);

  document.getElementById("cart-items").innerHTML = cartProducts.length
    ? cartProducts.map((product, index) => `
      <div class="cart-item">
        <strong>${product.name}</strong>
        <p>${product.category} | ${currency.format(product.price)}</p>
        <button class="button secondary" data-cart-index="${index}">Remove</button>
      </div>
    `).join("")
    : `<div class="cart-summary">Your cart is empty. Add a hero product to activate the AI bundle engine.</div>`;

  document.querySelectorAll("[data-cart-index]").forEach((button) => {
    button.addEventListener("click", () => {
      state.cart.splice(Number(button.dataset.cartIndex), 1);
      renderCart();
    });
  });

  const response = await fetch(`/api/ai/cart-insight?items=${encodeURIComponent(state.cart.join(","))}`);
  const insight = await response.json();
  const addOn = insight.nextBestAction;

  document.getElementById("cart-summary").innerHTML = `
    <strong>${currency.format(insight.cartTotal)} | Synergy ${insight.synergyScore}</strong>
    <p>${insight.signal}</p>
    ${addOn ? `<p>Next best action: <strong>${addOn.name}</strong> for ${currency.format(addOn.price)}</p>` : ""}
  `;
}

function bindControls() {
  const budget = document.getElementById("budget");
  budget.addEventListener("input", () => {
    document.getElementById("budget-value").textContent = currency.format(Number(budget.value));
  });

  document.getElementById("concierge-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    await fetchRecommendations();
  });
}

bindControls();
loadStorefront();
