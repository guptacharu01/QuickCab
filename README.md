📖 Introduction
QuickCab is a full-stack cab booking web app with three users — Riders, Drivers and an Admin. A rider books a cab on a map, tracks the driver live, pays by card and rates the trip. A driver accepts rides and completes trips. The admin manages the whole platform.

✨ Features - 

🗺️ Book a ride by picking pickup & drop points on a map.

💰 Fare estimate shown before booking, with promo codes (try WELCOME50).

📡 Live tracking — driver location and ride status update without refreshing.

💳 Online card payment through Stripe.

⭐ Rate your driver after the trip.

🎫 Support tickets for any problem.

🛡️ Secure login with roles — Rider / Driver / Admin.



🔍 Research
Before coding, I used Ola and Uber as a rider and studied their flow — booking, fare estimate, live tracking, payment and rating. I also researched the Haversine formula to calculate the distance between the pickup and drop points, compared map options (Google Maps needs a paid API key, OpenStreetMap is free), and studied how live tracking and safe card payments work


📚 Libraries

Frontend: React Router (page navigation) • Axios (API calls — attaches the login token automatically) • Leaflet (maps) • Stripe.js (payment form)

Backend: Spring Web (REST APIs) • Spring Data JPA (database access without long SQL) • Spring Security (login & roles) • 

JJWT (creating and checking JWT tokens) • Stripe Java library (payments) • Lombok (less repeated code) • JUnit (testing)

📸 Functionality-
1. 🏠 Landing Page — the home page, from where a new user registers or logs in.

2. 🧭 Rider Dashboard — the rider selects pickup and drop locations on the map, sees the fare estimate, and can apply a promo code before booking.
  
3. 🚕 Driver Dashboard — the driver goes online, sees ride requests, accepts a ride, and updates the trip status step by step (arriving → arrived → start trip → complete trip).

4. 📍 Live Tracking — during the trip, the rider sees the driver's location moving on the map and the status changing automatically, without refreshing the page.

5. 💳 Payment & Rating — after the trip, the rider pays by card through Stripe (test mode) and rates the driver.

6. 🛠️ Admin Dashboard — the admin can see platform statistics, all users, all rides and the support tickets.

▶️ How to Run

Start Docker Desktop

In the project folder, run:
docker compose up --build

In the frontend folder, run:
npm install
npm run dev
Open http://localhost:5173


💡 For card payments, add your Stripe test keys in a .env file (see .env.example). Without keys, the app honestly shows "pay in cash" instead of faking a payment.


🎯 Conclusion
Building QuickCab taught me how a real booking system works end to end — authentication, role-based access, real-time updates with WebSocket, and payment integration. In the future I want to add automatic driver assignment, surge pricing and SMS notifications. 🚀




