import React from 'react';
import './index.css';
import ReactDOM from 'react-dom/client';
import App from './App';

console.log("React Entry Configured");

const rootElement = document.getElementById('root');
if (!rootElement) {
  console.error("Root element not found");
  document.body.innerHTML += '<div style="color:red">Root Missing</div>';
} else {
  console.log("Mounting React App...");
  try {
    const root = ReactDOM.createRoot(rootElement);
    root.render(
      <React.StrictMode>
        <App />
      </React.StrictMode>
    );
    console.log("React Render Called");
  } catch (e) {
    console.error("Mount Error", e);
    document.body.innerHTML += '<div style="background:red;color:white">Mount Error: ' + e + '</div>';
  }
}