import { BrowserCacheLocation, PublicClientApplication, type Configuration } from '@azure/msal-browser';

const configuration: Configuration = {
  auth: {
    clientId: 'db5f13af-8b1a-4d80-9944-17ea6ff024cf',
    authority: 'https://login.microsoftonline.com/common',
    redirectUri: window.location.origin
  },
  cache: { cacheLocation: BrowserCacheLocation.LocalStorage }
};

export const msalInstance = new PublicClientApplication(configuration);
