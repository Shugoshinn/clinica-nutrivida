import React from 'react';
import ReactDOM from 'react-dom/client';
import { MsalProvider } from '@azure/msal-react';
import { BrowserRouter } from 'react-router-dom';
import { msalInstance } from './auth';
import App from './App';
import './react.css';

async function start() {
  await msalInstance.initialize();
  const redirectResult = await msalInstance.handleRedirectPromise();
  if (redirectResult?.account) msalInstance.setActiveAccount(redirectResult.account);
  if (!msalInstance.getActiveAccount()) {
    const account = msalInstance.getAllAccounts()[0];
    if (account) msalInstance.setActiveAccount(account);
  }

  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <MsalProvider instance={msalInstance}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </MsalProvider>
    </React.StrictMode>
  );
}

start().catch((error: unknown) => console.error('No se pudo iniciar NutriVida:', error));
