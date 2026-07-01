let cards = [];
let current = 0;
let answerVisible = false;
let favorites = [];

/*
 * NEU (passend zu SecurityConfig: CSRF ist aktiv, nicht deaktiviert):
 * Liest das CSRF-Token aus dem von Spring Security gesetzten Cookie
 * "XSRF-TOKEN" und haengt es bei aendernden Requests (POST/PUT/DELETE)
 * als Header "X-XSRF-TOKEN" an. Ohne dieses Token wuerde Spring
 * Security solche Requests mit 403 Forbidden ablehnen.
 */
function getCsrfToken() {
    const match = document.cookie.match(/(^|;)\s*XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[2]) : null;
}

async function ensureCsrfToken() {
    let token = getCsrfToken();
    if (token) {
        console.debug("ensureCsrfToken: existing token found", token);
        return token;
    }

    console.debug("ensureCsrfToken: requesting /api/users/csrf");
    let response = await fetch("/api/users/csrf", {
        credentials: "include"
    });
    if (!response.ok) {
        console.error("CSRF-Token-Endpunkt konnte nicht abgerufen werden.", response.status);
        return null;
    }

    const body = await response.json();
    token = body.token;
    console.debug("ensureCsrfToken: fetched token", token, "body", body);
    if (!token) {
        console.error("CSRF-Token konnte nicht abgerufen werden.");
    }
    return token;
}

async function secureFetch(url, options = {}) 
{
    const method = (options.method || "GET").toUpperCase();
    options.credentials = "include";

    if (
        ["POST","PUT","DELETE","PATCH"]
        .includes(method)
    ) {
        let token = await ensureCsrfToken();
        options.headers = {
            ...(options.headers || {})
        };
        if (token) {
            options.headers["X-XSRF-TOKEN"] = token;
            options.headers["X-CSRF-TOKEN"] = token;
        } else {
            console.warn("secureFetch: kein CSRF-Token verfuegbar", method, url);
        }
        console.debug("secureFetch", method, url, "csrfToken", token, "headers", options.headers);
    }
    return fetch(url, options);
}

function loadFavorites() {
    favorites = JSON.parse(localStorage.getItem("favorites_" + currentUser)) || [];
}
function saveFavorites() {
    localStorage.setItem("favorites_" + currentUser, JSON.stringify(favorites));
}
let quizIndex = 0;
let quizScore = 0;
let quizAnswerShown = false;
let currentUser = null;


async function loadCards() {
    let userRes = await fetch("/api/users/current", {
        credentials: "include"
    });

    if (!userRes.ok) {
        if (userRes.status === 401 || userRes.status === 403) {
            location.href = "login.html";
            return;
        }
        console.error("Fehler beim Abrufen des aktuellen Benutzers:", userRes.status);
        return;
    }

    currentUser = await userRes.text();
    if (!currentUser) {
        location.href = "login.html";
        return;
    }

    let token = await ensureCsrfToken();
    console.debug("loadCards: csrf token on page load", token);

    let response = await fetch("/api/cards", {
        credentials: "include"
    });
    if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
            location.href = "login.html";
            return;
        }
        console.error("Fehler beim Laden der Karten:", response.status);
        return;
    }

    cards = await response.json();

    if (document.getElementById("cards")) {
        showCards();
        loadCategories();
        updateStats();
        loadFavorites();
    }
    if (document.getElementById("testCard")) {
        testIndex = 0;
        testScore = 0;
        showTestCard();
    }
}




// Karten anzeigen
function showCards() {
    showFilteredCards(cards);
}


// Karte erstellen
async function addCard() {

    let question = document.getElementById("question").value.trim();
    let answer = document.getElementById("answer").value.trim();
    let category = document.getElementById("category").value.trim();

    // ❗ Eingaben prüfen
    if (question === "" || answer === "" || category === "") {
        alert("Alle Felder müssen ausgefüllt sein!");
        return;
    }

    let card = { question, answer, category };

    // UPDATE
    if (window.editId) {
        let res = await secureFetch(`/api/cards/${window.editId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(card)
        });
        if (res.status === 403) {
            alert("Kein Zugriff: Diese Karte gehört dir nicht.");
        }

        window.editId = null;
    }
    // CREATE
    else {
        await secureFetch("/api/cards", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(card)
        });
    }

    document.getElementById("question").value = "";
    document.getElementById("answer").value = "";
    document.getElementById("category").value = "";

    loadCards();
}



// Bearbeiten
function editCard(id) {
    const card = cards.find(c => c.id === id);

    document.getElementById("question").value = card.question;
    document.getElementById("answer").value = card.answer;
    document.getElementById("category").value = card.category;

    window.editId = id;
}

// löschen
async function deleteCard(id)
{
    console.log("Delete clicked:", id);

    let response = await secureFetch(`/api/cards/${id}`,
    {
        method: "DELETE"
    });

    console.log("Status:", response.status);
    await loadCards();
}


// TEST MODUS
function showTestCard() {
    let div = document.getElementById("testCard");
    if (!div) return;

    if (testIndex >= cards.length) {
        div.innerHTML = `
            <h2>🎉 Herzlichen Glückwunsch! 🎉</h2>
            <p>Du hast <b>${testScore}</b> von <b>${cards.length}</b> Punkten erreicht!</p>
        `;
        document.getElementById("testScore").innerHTML = "";
        return;
    }

    let card = cards[testIndex];

    div.innerHTML = `
        <h2>${card.question}</h2>
        <p id="testAnswer">Klicken für Lösung</p>
    `;

    testAnswerShown = false;
    updateTestScore();
}


function showTestAnswer() {
    if (!testAnswerShown) {
        document.getElementById("testAnswer").innerHTML = cards[testIndex].answer;
        testAnswerShown = true;
    }
}
function testCorrect() {
    if (!testAnswerShown) return;
    testScore++;
    testIndex++;
    showTestCard();
}

function testWrong() {
    if (!testAnswerShown) return;
    testIndex++;
    showTestCard();
}
function updateTestScore() {
    document.getElementById("testScore").innerHTML =
        `Punkte: ${testScore} / ${cards.length}`;
}


function showAnswer()
{
    let card=cards[current];
    let answer = document.getElementById("answer");

    if(answerVisible){
        answer.innerHTML=
        "Klicken für Lösung";
    }
    else{
        answer.innerHTML=
        card.answer;
    }
    answerVisible=!answerVisible;
}


function nextCard()
{
    current++;
    if(current>=cards.length)
        div.innerHTML += `
        <div class="card">
            <h3>Herzlichen Glückwunsch</h3>
            <p>Du bist ferting</p>
        </div>
        `;
        loadCards();
}

function loadCategories() {
    let sel = document.getElementById("filterCategory");
    sel.innerHTML = `<option value="">Alle Kategorien</option>`;

    // Kategorien aus Karten extrahieren
    let uniqueCats = [...new Set(cards.map(c => c.category))];

    uniqueCats.forEach(c => {
        sel.innerHTML += `<option value="${c}">${c}</option>`;
    });
}


function applyFilters() {
    let search = document.getElementById("search").value.toLowerCase();
    let filterCat = document.getElementById("filterCategory").value;
    let favOnly = document.getElementById("favOnly").checked;

    let filtered = cards.filter(card => {

        let matchesSearch =
            card.question.toLowerCase().includes(search) ||
            card.answer.toLowerCase().includes(search) ||
            card.category.toLowerCase().includes(search);

        let matchesCategory =
            filterCat === "" || card.category === filterCat;

        let matchesFavorite =
            !favOnly || favorites.includes(card.id);

        return matchesSearch && matchesCategory && matchesFavorite;
    });

    showFilteredCards(filtered);
}


function showFilteredCards(list) {
    let div = document.getElementById("cards");
    div.innerHTML = "";

    list.forEach(card => {
        let isFav = favorites.includes(card.id);
        let star = isFav ? "⭐" : "☆";

        div.innerHTML += `
        <div class="card">
            <h3>Question: ${card.question}</h3>
            <p>Answer: ${card.answer}</p>
            <small>Category: ${card.category}</small>
            <br><br>

            <button onclick="toggleFavorite(${card.id})">${star}</button>
            <button onclick="editCard(${card.id})">Bearbeiten</button>
            <button onclick="deleteCard(${card.id})">Löschen</button>
        </div>
        `;
    });
}


function toggleFavorite(id) {
    if (favorites.includes(id)) {
        favorites = favorites.filter(f => f !== id);
    } else {
        favorites.push(id);
    }

    saveFavorites();
    applyFilters();
    updateStats();
}



function toggleStats() {
    let box = document.getElementById("stats");
    let btn = document.getElementById("statsToggle");

    if (box.style.display === "none") {
        box.style.display = "block";
        btn.innerText = "Statistik verstecken";
    } else {
        box.style.display = "none";
        btn.innerText = "Statistik anzeigen";
    }
}

function updateStats() {
    let total = cards.length;
    let favCount = favorites.filter(id => cards.some(c => c.id === id)).length;

    let categoryCounts = {};
    cards.forEach(c => {
        categoryCounts[c.category] = (categoryCounts[c.category] || 0) + 1;
    });

    let statsDiv = document.getElementById("stats");

    let catHtml = "";
    for (let cat in categoryCounts) {
        catHtml += `<li>${cat}: ${categoryCounts[cat]} Karten</li>`;
    }

    statsDiv.innerHTML = `
        <h3>Statistik</h3>
        <p><b>Gesamtanzahl Karten:</b> ${total}</p>
        <p><b>Favoriten:</b> ${favCount}</p>
        <p><b>Karten pro Kategorie:</b></p>
        <ul>${catHtml}</ul>
    `;
}


loadCards();