# UserMailer Frontend

A React frontend for the UserMailer email campaign management system.

## 🚀 Quick Start

### Prerequisites
- Node.js 18+ installed ([Download here](https://nodejs.org/))
- Your Spring Boot backend running on port 8080

### Running the Frontend

1. **Navigate to the frontend folder:**
   ```bash
   cd frontend
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Start the development server:**
   ```bash
   npm run dev
   ```

4. **Open your browser:**
   Go to `http://localhost:3000`

That's it! The frontend will automatically proxy API calls to your backend at `localhost:8080`.

---

## 📁 Project Structure Explained

```
frontend/
├── index.html          # The single HTML page (React takes over from here)
├── package.json        # Project dependencies (like pom.xml in Java)
├── vite.config.js      # Build tool configuration
└── src/
    ├── main.jsx        # Entry point - starts the React app
    ├── App.jsx         # Main component with routing
    ├── App.css         # Navigation styles
    ├── index.css       # Global styles (colors, buttons, inputs)
    ├── services/
    │   └── api.js      # All backend API calls (like a Java service)
    └── pages/
        ├── LoginPage.jsx       # Login/Register page
        ├── LoginPage.css
        ├── DashboardPage.jsx   # Main dashboard with profiles
        ├── DashboardPage.css
        ├── CampaignPage.jsx    # Create campaign page
        └── CampaignPage.css
```

---

## 🎓 React Concepts for Java Developers

### Components = Classes
```jsx
// A React component is like a Java class that returns UI
function MyComponent() {
  return <div>Hello World</div>;
}
```

### State = Instance Variables (that trigger re-render)
```jsx
// useState creates a variable that, when changed, re-renders the component
const [count, setCount] = useState(0);  // count=0 initially

// To update:
setCount(count + 1);  // This triggers a re-render!
```

### Props = Constructor Parameters
```jsx
// Parent component
<UserCard name="John" email="john@example.com" />

// Child component receives props
function UserCard({ name, email }) {
  return <div>{name} - {email}</div>;
}
```

### useEffect = @PostConstruct
```jsx
// Runs once when component loads
useEffect(() => {
  fetchDataFromBackend();
}, []);  // Empty array = run once
```

### JSX = HTML-like syntax
```jsx
// JSX looks like HTML but is actually JavaScript
// Note: className instead of class, onClick instead of onclick
<button className="btn" onClick={handleClick}>
  Click me
</button>
```

---

## 🔧 Common Commands

| Command | Description |
|---------|-------------|
| `npm install` | Install dependencies (like `mvn install`) |
| `npm run dev` | Start dev server with hot reload |
| `npm run build` | Build for production (creates `dist/` folder) |
| `npm run preview` | Preview the production build |

---

## 🎨 Customizing Styles

All colors are defined as CSS variables in `src/index.css`:

```css
:root {
  --accent-primary: #6366f1;    /* Main purple color */
  --bg-primary: #0a0a0f;        /* Dark background */
  --text-primary: #f4f4f5;      /* Light text */
  /* ... more in index.css */
}
```

Change these to update the entire app's color scheme!

---

## 📝 TODO: Backend Endpoints Needed

The frontend currently uses mock data for some features. To fully work, add these endpoints:

1. **GET /api/profiles** - Get user's profiles (paginated)
2. **POST /api/campaigns** - Create a new campaign
3. **GET /api/campaigns** - List user's campaigns

---

## ❓ FAQ

**Q: Why does my login not work?**
A: Make sure your Spring Boot backend is running on port 8080.

**Q: How do I change the backend URL?**
A: Edit `vite.config.js` and change the proxy target.

**Q: I see CORS errors in console?**
A: Make sure the SecurityConfig.java has CORS enabled (we added this).


