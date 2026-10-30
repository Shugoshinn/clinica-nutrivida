import { BrowserCacheLocation, PublicClientApplication, type Configuration } from '@azure/msal-browser';
import { environment } from './environments/environment';

const configuration: Configuration = {
  auth: {
    clientId: environment.msalConfig.clientId,
    authority: environment.msalConfig.authority,
    redirectUri: window.location.origin
  },
  cache: { cacheLocation: BrowserCacheLocation.LocalStorage }
};

export const msalInstance = new PublicClientApplication(configuration);
