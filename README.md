# PHV Profit Checker 🚗💰

**PHV Profit Checker** is an Android accessibility-based utility app designed for private hire drivers (TADA, Grab) in Singapore. It helps drivers instantly assess **trip profitability** based on **net fare**, **pickup time**, and **trip time** — so they can make smart decisions fast.

---

## 📲 What It Does

When a new TADA job appears on screen:
- 📍 Automatically extracts the **pickup and drop-off addresses**
- 🧠 Uses **Google Maps Directions API** to calculate:
  - Estimated time to reach the pickup
  - Estimated trip duration
- 💵 Computes:
  - **Total time commitment**
  - **Earnings per minute**
- 🪟 Shows a **floating overlay** with:
  - Fare
  - Time breakdown
  - Profitability (S$/min)

---

## ✨ Features

- Works in the background with TADA app
- Floating UI appears only when a valid job is detected
- Handles **multi-stop trips**
- Uses **full addresses** (not just postal codes) for better ETA accuracy
- Automatically logs each trip internally

---

## 🔧 Tech Stack

- **Kotlin**
- **Android AccessibilityService**
- **Google Maps Directions API**
- **Coroutines**
- **Custom floating view**

---

## 🛠️ Installation

> This app is not yet available on the Play Store.

To install:
1. Clone or download the repo
2. Open in Android Studio
3. Insert your own [Google Maps API key](https://developers.google.com/maps/documentation/directions/get-api-key) in `MyAccessibilityService.kt`
4. Build and install the APK on your Android phone
5. Manually enable the Accessibility permission

---

## 🚨 Permissions Required

- Accessibility access (to detect TADA job screen)
- Location access (to compute ETA to pickup)
- Overlay permission (for floating view)

---

## 🚧 Roadmap

- [ ] Trip history export (CSV or JSON)
- [ ] Add support for Grab jobs
- [ ] Graphs or heatmap to visualize earnings
- [ ] Configurable rate thresholds (e.g., hide float if <$0.50/min)

---

## 🧑‍💻 Contributing

Contributions are welcome! If you have feature ideas or want to improve the logic/UI, feel free to fork and submit a pull request.

---

## 📄 License

This project is open-source under the [MIT License](LICENSE).

---

## 🧠 Author

**Shawn Lee**  
Singapore 🇸🇬  
Built with ❤️ to help fellow PHV drivers earn smarter.
