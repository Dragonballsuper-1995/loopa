import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getAnalytics } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-analytics.js";

const firebaseConfig = {
    apiKey: "AIzaSyA5eYGlSMNFSKn0QyFRyGkDwYpRAc9aISY",
    authDomain: "loopa-4e92d.firebaseapp.com",
    projectId: "loopa-4e92d",
    storageBucket: "loopa-4e92d.firebasestorage.app",
    messagingSenderId: "752063756580",
    appId: "1:752063756580:web:4175ffe3b6c7e44329dbb1",
    measurementId: "G-XRL3HP9CT4"
};

const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
