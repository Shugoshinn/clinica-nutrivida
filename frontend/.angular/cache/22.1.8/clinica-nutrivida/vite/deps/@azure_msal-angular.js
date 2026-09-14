import { Al as ɵɵinject, Dc as Injector, Dl as ɵɵdefineInjector, Ec as InjectionToken, El as ɵɵdefineInjectable, Fc as NgZone, Fn as Injectable, Ll as map, Pn as Inject, Qn as Optional, Rl as BehaviorSubject, Wi as setClassMetadata, cn as Component, mc as DOCUMENT, oo as ɵɵdirectiveInject, qn as NgModule, ro as ɵɵdefineNgModule, to as ɵɵdefineComponent, tu as __extends, zl as Subject } from "./core-Ck7wK_Go.js";
import { At as catchError, ct as Router, jt as EMPTY, kt as take } from "./router-Bg9saWUd.js";
import { o as from } from "./_platform_location-chunk-H3u03j5u.js";
import { G as concatMap, J as of, K as filter, U as switchMap } from "./http-DS9mOgie.js";
import { s as Location } from "./common-Cdav2D-m.js";
import { P as CommonModule } from "./platform-browser-DUIXScRp.js";
import { D as BrowserConfigurationAuthError, M as InteractionType, N as WrapperSKU, T as blockReloadInHiddenIframes, c as EventMessageUtils, g as NavigationClient, j as InteractionStatus } from "./dist-DihIBeG7.js";
//#region node_modules/rxjs/dist/esm5/internal/scheduler/dateTimestampProvider.js
var dateTimestampProvider = {
	now: function() {
		return (dateTimestampProvider.delegate || Date).now();
	},
	delegate: void 0
};
//#endregion
//#region node_modules/rxjs/dist/esm5/internal/ReplaySubject.js
var ReplaySubject = function(_super) {
	__extends(ReplaySubject, _super);
	function ReplaySubject(_bufferSize, _windowTime, _timestampProvider) {
		if (_bufferSize === void 0) _bufferSize = Infinity;
		if (_windowTime === void 0) _windowTime = Infinity;
		if (_timestampProvider === void 0) _timestampProvider = dateTimestampProvider;
		var _this = _super.call(this) || this;
		_this._bufferSize = _bufferSize;
		_this._windowTime = _windowTime;
		_this._timestampProvider = _timestampProvider;
		_this._buffer = [];
		_this._infiniteTimeWindow = true;
		_this._infiniteTimeWindow = _windowTime === Infinity;
		_this._bufferSize = Math.max(1, _bufferSize);
		_this._windowTime = Math.max(1, _windowTime);
		return _this;
	}
	ReplaySubject.prototype.next = function(value) {
		var _a = this, isStopped = _a.isStopped, _buffer = _a._buffer, _infiniteTimeWindow = _a._infiniteTimeWindow, _timestampProvider = _a._timestampProvider, _windowTime = _a._windowTime;
		if (!isStopped) {
			_buffer.push(value);
			!_infiniteTimeWindow && _buffer.push(_timestampProvider.now() + _windowTime);
		}
		this._trimBuffer();
		_super.prototype.next.call(this, value);
	};
	ReplaySubject.prototype._subscribe = function(subscriber) {
		this._throwIfClosed();
		this._trimBuffer();
		var subscription = this._innerSubscribe(subscriber);
		var _a = this, _infiniteTimeWindow = _a._infiniteTimeWindow;
		var copy = _a._buffer.slice();
		for (var i = 0; i < copy.length && !subscriber.closed; i += _infiniteTimeWindow ? 1 : 2) subscriber.next(copy[i]);
		this._checkFinalizedStatuses(subscriber);
		return subscription;
	};
	ReplaySubject.prototype._trimBuffer = function() {
		var _a = this, _bufferSize = _a._bufferSize, _timestampProvider = _a._timestampProvider, _buffer = _a._buffer, _infiniteTimeWindow = _a._infiniteTimeWindow;
		var adjustedBufferSize = (_infiniteTimeWindow ? 1 : 2) * _bufferSize;
		_bufferSize < Infinity && adjustedBufferSize < _buffer.length && _buffer.splice(0, _buffer.length - adjustedBufferSize);
		if (!_infiniteTimeWindow) {
			var now = _timestampProvider.now();
			var last = 0;
			for (var i = 1; i < _buffer.length && _buffer[i] <= now; i += 2) last = i;
			last && _buffer.splice(0, last + 1);
		}
	};
	return ReplaySubject;
}(Subject);
//#endregion
//#region node_modules/@azure/msal-angular/fesm2022/azure-msal-angular.mjs
var name = "@azure/msal-angular";
var version = "6.2.0";
var MSAL_INSTANCE = new InjectionToken("MSAL_INSTANCE");
var MSAL_GUARD_CONFIG = new InjectionToken("MSAL_GUARD_CONFIG");
var MSAL_INTERCEPTOR_CONFIG = new InjectionToken("MSAL_INTERCEPTOR_CONFIG");
var MSAL_BROADCAST_CONFIG = new InjectionToken("MSAL_BROADCAST_CONFIG");
var MsalBroadcastService = class MsalBroadcastService {
	constructor(msalInstance, ngZone, msalBroadcastConfig) {
		this.msalInstance = msalInstance;
		this.ngZone = ngZone;
		this.msalBroadcastConfig = msalBroadcastConfig;
		if (this.msalBroadcastConfig && this.msalBroadcastConfig.eventsToReplay > 0) {
			this.msalInstance.getLogger().clone(name, version).verbose(`BroadcastService - eventsToReplay set on BroadcastConfig, replaying the last ${this.msalBroadcastConfig.eventsToReplay} events`, "");
			this._msalSubject = new ReplaySubject(this.msalBroadcastConfig.eventsToReplay);
		} else this._msalSubject = new Subject();
		this.msalSubject$ = this._msalSubject.asObservable();
		this._inProgress = new BehaviorSubject(InteractionStatus.Startup);
		this.inProgress$ = this._inProgress.asObservable();
		this.msalInstance.addEventCallback((message) => {
			this.ngZone.run(() => {
				this._msalSubject.next(message);
				const status = EventMessageUtils.getInteractionStatusFromEvent(message, this._inProgress.value);
				if (status !== null) {
					this.msalInstance.getLogger().clone(name, version).verbose(`BroadcastService - ${message.eventType} results in setting inProgress from ${this._inProgress.value} to ${status}`, "");
					this._inProgress.next(status);
				}
			});
		});
	}
	/**
	* Resets inProgress state to None
	*/
	resetInProgressEvent() {
		if (this._inProgress.value === InteractionStatus.Startup) this._inProgress.next(InteractionStatus.None);
	}
	static {
		this.ɵfac = function MsalBroadcastService_Factory(__ngFactoryType__) {
			return new (__ngFactoryType__ || MsalBroadcastService)(ɵɵinject(MSAL_INSTANCE), ɵɵinject(NgZone), ɵɵinject(MSAL_BROADCAST_CONFIG, 8));
		};
	}
	static {
		this.ɵprov = /* @__PURE__ */ ɵɵdefineInjectable({
			token: MsalBroadcastService,
			factory: MsalBroadcastService.ɵfac
		});
	}
};
(() => {
	(typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MsalBroadcastService, [{ type: Injectable }], () => [
		{
			type: void 0,
			decorators: [{
				type: Inject,
				args: [MSAL_INSTANCE]
			}]
		},
		{ type: NgZone },
		{
			type: void 0,
			decorators: [{ type: Optional }, {
				type: Inject,
				args: [MSAL_BROADCAST_CONFIG]
			}]
		}
	], null);
})();
var MsalService = class MsalService {
	constructor(instance, location, injector) {
		this.instance = instance;
		this.location = location;
		this.injector = injector;
		const hash = this.location.path(true).split("#").pop();
		if (hash) this.redirectHash = `#${hash}`;
		this.instance.initializeWrapperLibrary(WrapperSKU.Angular, version);
	}
	initialize() {
		return from(this.instance.initialize());
	}
	/**
	* Ensures the PublicClientApplication has been initialized.
	*/
	withInitialize(call) {
		return from(this.instance.initialize().then(() => call()));
	}
	acquireTokenPopup(request) {
		return this.withInitialize(() => this.instance.acquireTokenPopup(request));
	}
	acquireTokenRedirect(request) {
		return this.withInitialize(() => this.instance.acquireTokenRedirect(request));
	}
	acquireTokenSilent(silentRequest) {
		return this.withInitialize(() => this.instance.acquireTokenSilent(silentRequest));
	}
	handleRedirectObservable(hashOrOptions) {
		const options = typeof hashOrOptions === "string" ? { hash: hashOrOptions } : hashOrOptions || {};
		const hash = options.hash || this.redirectHash;
		const finalOptions = {
			...options,
			...hash ? { hash } : {}
		};
		return from(this.instance.initialize().then(() => this.instance.handleRedirectPromise(finalOptions)).finally(() => {
			this.injector.get(MsalBroadcastService).resetInProgressEvent();
		}));
	}
	loginPopup(request) {
		return this.withInitialize(() => this.instance.loginPopup(request));
	}
	loginRedirect(request) {
		return this.withInitialize(() => this.instance.loginRedirect(request));
	}
	logoutRedirect(logoutRequest) {
		return this.withInitialize(() => this.instance.logoutRedirect(logoutRequest));
	}
	logoutPopup(logoutRequest) {
		return this.withInitialize(() => this.instance.logoutPopup(logoutRequest));
	}
	ssoSilent(request) {
		return this.withInitialize(() => this.instance.ssoSilent(request));
	}
	/**
	* Gets logger for msal-angular.
	* If no logger set, returns logger instance created with same options as msal-browser
	*/
	getLogger() {
		if (!this.logger) this.logger = this.instance.getLogger().clone(name, version);
		return this.logger;
	}
	setLogger(logger) {
		this.logger = logger.clone(name, version);
		this.instance.setLogger(logger);
	}
	static {
		this.ɵfac = function MsalService_Factory(__ngFactoryType__) {
			return new (__ngFactoryType__ || MsalService)(ɵɵinject(MSAL_INSTANCE), ɵɵinject(Location), ɵɵinject(Injector));
		};
	}
	static {
		this.ɵprov = /* @__PURE__ */ ɵɵdefineInjectable({
			token: MsalService,
			factory: MsalService.ɵfac
		});
	}
};
(() => {
	(typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MsalService, [{ type: Injectable }], () => [
		{
			type: void 0,
			decorators: [{
				type: Inject,
				args: [MSAL_INSTANCE]
			}]
		},
		{ type: Location },
		{ type: Injector }
	], null);
})();
var MsalGuard = class MsalGuard {
	constructor(msalGuardConfig, msalBroadcastService, authService, location, router) {
		this.msalGuardConfig = msalGuardConfig;
		this.msalBroadcastService = msalBroadcastService;
		this.authService = authService;
		this.location = location;
		this.router = router;
		this.msalBroadcastService.inProgress$.subscribe();
	}
	/**
	* Parses url string to UrlTree
	* @param url
	*/
	parseUrl(url) {
		return this.router.parseUrl(url);
	}
	/**
	* Builds the absolute url for the destination page
	* @param path Relative path of requested page
	* @returns Full destination url
	*/
	getDestinationUrl(path) {
		this.authService.getLogger().verbose("Guard - getting destination url", "");
		const baseElements = document.getElementsByTagName("base");
		const baseUrl = this.location.normalize(baseElements.length ? baseElements[0].href : window.location.origin);
		const pathUrl = this.location.prepareExternalUrl(path);
		if (pathUrl.startsWith("#")) {
			this.authService.getLogger().verbose("Guard - destination by hash routing", "");
			return `${baseUrl}/${pathUrl}`;
		}
		return `${baseUrl}${path}`;
	}
	/**
	* Interactively prompt the user to login
	* @param url Path of the requested page
	*/
	loginInteractively(state) {
		const authRequest = typeof this.msalGuardConfig.authRequest === "function" ? this.msalGuardConfig.authRequest(this.authService, state) : { ...this.msalGuardConfig.authRequest };
		if (this.msalGuardConfig.interactionType === InteractionType.Popup) {
			this.authService.getLogger().verbose("Guard - logging in by popup", authRequest.correlationId ?? "");
			return this.authService.loginPopup(authRequest).pipe(map((response) => {
				this.authService.getLogger().verbose("Guard - login by popup successful, can activate, setting active account", authRequest.correlationId ?? "");
				this.authService.instance.setActiveAccount(response.account);
				return true;
			}));
		}
		this.authService.getLogger().verbose("Guard - logging in by redirect", authRequest.correlationId ?? "");
		const redirectStartPage = this.getDestinationUrl(state.url);
		return this.authService.loginRedirect({
			redirectStartPage,
			...authRequest
		}).pipe(map(() => false));
	}
	/**
	* Helper which checks for the correct interaction type, prevents page with Guard to be set as redirect, and calls handleRedirectObservable
	* @param state
	*/
	activateHelper(state) {
		if (this.msalGuardConfig.interactionType !== InteractionType.Popup && this.msalGuardConfig.interactionType !== InteractionType.Redirect) throw new BrowserConfigurationAuthError("invalid_interaction_type", "", "Invalid interaction type provided to MSAL Guard. InteractionType.Popup or InteractionType.Redirect must be provided in the MsalGuardConfiguration");
		this.authService.getLogger().verbose("MSAL Guard activated", "");
		if (typeof window === "undefined") {
			this.authService.getLogger().info("Guard - window is undefined, MSAL does not support server-side token acquisition", "");
			return of(true);
		} else try {
			blockReloadInHiddenIframes();
		} catch (error) {
			if (!this.authService.instance.getConfiguration().system.allowRedirectInIframe) {
				this.authService.getLogger().warning("Guard - redirectUri set to page with MSAL Guard. It is recommended to not set redirectUri to a page that requires authentication.", "");
				return of(false);
			}
		}
		/**
		* If a loginFailedRoute is set in the config, set this as the loginFailedRoute
		*/
		if (this.msalGuardConfig.loginFailedRoute) this.loginFailedRoute = this.parseUrl(this.msalGuardConfig.loginFailedRoute);
		const currentPath = this.location.path(true);
		return this.authService.initialize().pipe(concatMap(() => {
			return this.authService.handleRedirectObservable();
		}), concatMap(() => {
			if (!this.authService.instance.getAllAccounts().length) {
				if (state) {
					this.authService.getLogger().verbose("Guard - no accounts retrieved, log in required to activate", "");
					return this.loginInteractively(state);
				}
				this.authService.getLogger().verbose("Guard - no accounts retrieved, no state, cannot load", "");
				return of(false);
			}
			this.authService.getLogger().verbose("Guard - at least 1 account exists, can activate or load", "");
			if (state) {
				const urlContainsCode = this.includesCode(state.url);
				const fragmentContainsCode = !!state.root && !!state.root.fragment && this.includesCode(`#${state.root.fragment}`);
				const hashRouting = this.location.prepareExternalUrl(state.url).indexOf("#") === 0;
				if (urlContainsCode && (fragmentContainsCode || hashRouting)) {
					this.authService.getLogger().info("Guard - Hash contains known code response, stopping navigation.", "");
					if (currentPath.indexOf("#") > -1) return of(this.parseUrl(this.location.path()));
					return of(this.parseUrl(""));
				}
			}
			return of(true);
		}), catchError((error) => {
			this.authService.getLogger().error("Guard - error while logging in, unable to activate", "");
			this.authService.getLogger().errorPii(`Guard - error: ${error.message}`, "");
			/**
			* If a loginFailedRoute is set, checks to see if state is passed before returning route
			*/
			if (this.loginFailedRoute && state) {
				this.authService.getLogger().verbose("Guard - loginFailedRoute set, redirecting", "");
				return of(this.loginFailedRoute);
			}
			return of(false);
		}));
	}
	includesCode(path) {
		return path.lastIndexOf("/code") > -1 && path.lastIndexOf("/code") === path.length - 5 || path.indexOf("#code=") > -1 || path.indexOf("&code=") > -1;
	}
	canActivate(route, state) {
		this.authService.getLogger().verbose("Guard - canActivate", "");
		return this.activateHelper(state);
	}
	canActivateChild(route, state) {
		this.authService.getLogger().verbose("Guard - canActivateChild", "");
		return this.activateHelper(state);
	}
	canMatch() {
		this.authService.getLogger().verbose("Guard - canLoad", "");
		return this.activateHelper();
	}
	static {
		this.ɵfac = function MsalGuard_Factory(__ngFactoryType__) {
			return new (__ngFactoryType__ || MsalGuard)(ɵɵinject(MSAL_GUARD_CONFIG), ɵɵinject(MsalBroadcastService), ɵɵinject(MsalService), ɵɵinject(Location), ɵɵinject(Router));
		};
	}
	static {
		this.ɵprov = /* @__PURE__ */ ɵɵdefineInjectable({
			token: MsalGuard,
			factory: MsalGuard.ɵfac
		});
	}
};
(() => {
	(typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MsalGuard, [{ type: Injectable }], () => [
		{
			type: void 0,
			decorators: [{
				type: Inject,
				args: [MSAL_GUARD_CONFIG]
			}]
		},
		{ type: MsalBroadcastService },
		{ type: MsalService },
		{ type: Location },
		{ type: Router }
	], null);
})();
var MsalInterceptor = class MsalInterceptor {
	constructor(msalInterceptorConfig, authService, location, msalBroadcastService, document) {
		this.msalInterceptorConfig = msalInterceptorConfig;
		this.authService = authService;
		this.location = location;
		this.msalBroadcastService = msalBroadcastService;
		this._document = document;
		if (this.msalInterceptorConfig.strictMatching === void 0) this.authService.getLogger().warning(`[MSAL] strictMatching is enabled by default. See: https://github.com/AzureAD/microsoft-authentication-library-for-js/blob/dev/lib/msal-angular/docs/msal-interceptor.md#strict-matching-strictmatching`, "");
	}
	intercept(req, next) {
		if (this.msalInterceptorConfig.interactionType !== InteractionType.Popup && this.msalInterceptorConfig.interactionType !== InteractionType.Redirect) throw new BrowserConfigurationAuthError("invalid_interaction_type", "", "Invalid interaction type provided to MSAL Interceptor. InteractionType.Popup, InteractionType.Redirect must be provided in the msalInterceptorConfiguration");
		this.authService.getLogger().verbose("MSAL Interceptor activated", "");
		const scopes = this.getScopesForEndpoint(req.url, req.method);
		if (!scopes || scopes.length === 0) {
			this.authService.getLogger().verbose("Interceptor - no scopes for endpoint", "");
			return next.handle(req);
		}
		let account;
		const activeAccount = this.authService.instance.getActiveAccount();
		if (activeAccount) {
			this.authService.getLogger().verbose("Interceptor - active account selected", "");
			account = activeAccount;
		} else {
			this.authService.getLogger().verbose("Interceptor - no active account, fallback to first account", "");
			account = this.authService.instance.getAllAccounts()[0];
		}
		const authRequest = typeof this.msalInterceptorConfig.authRequest === "function" ? this.msalInterceptorConfig.authRequest(this.authService, req, { account }) : {
			...this.msalInterceptorConfig.authRequest,
			account
		};
		this.authService.getLogger().info(`Interceptor - ${scopes.length} scopes found for endpoint`, "");
		this.authService.getLogger().infoPii(`Interceptor - [${scopes}] scopes found for ${req.url}`, "");
		return this.acquireToken(authRequest, scopes, account).pipe(switchMap((result) => {
			this.authService.getLogger().verbose("Interceptor - setting authorization headers", "");
			const headers = req.headers.set("Authorization", `Bearer ${result.accessToken}`);
			const requestClone = req.clone({ headers });
			return next.handle(requestClone);
		}));
	}
	/**
	* Try to acquire token silently. Invoke interaction if acquireTokenSilent rejected with error or resolved with null access token
	* @param authRequest Request
	* @param scopes Array of scopes for the request
	* @param account Account
	* @returns Authentication result
	*/
	acquireToken(authRequest, scopes, account) {
		return this.authService.acquireTokenSilent({
			...authRequest,
			scopes,
			account
		}).pipe(catchError(() => {
			this.authService.getLogger().error("Interceptor - acquireTokenSilent rejected with error. Invoking interaction to resolve.", authRequest.correlationId ?? "");
			return this.msalBroadcastService.inProgress$.pipe(take(1), switchMap((status) => {
				if (status === InteractionStatus.None) return this.acquireTokenInteractively(authRequest, scopes);
				return this.msalBroadcastService.inProgress$.pipe(filter((status) => status === InteractionStatus.None), take(1), switchMap(() => this.acquireToken(authRequest, scopes, account)));
			}));
		}), switchMap((result) => {
			if (!result.accessToken) {
				this.authService.getLogger().error("Interceptor - acquireTokenSilent resolved with null access token. Known issue with B2C tenants, invoking interaction to resolve.", authRequest.correlationId ?? "");
				return this.msalBroadcastService.inProgress$.pipe(filter((status) => status === InteractionStatus.None), take(1), switchMap(() => this.acquireTokenInteractively(authRequest, scopes)));
			}
			return of(result);
		}));
	}
	/**
	* Invoke interaction for the given set of scopes
	* @param authRequest Request
	* @param scopes Array of scopes for the request
	* @returns Result from the interactive request
	*/
	acquireTokenInteractively(authRequest, scopes) {
		if (this.msalInterceptorConfig.interactionType === InteractionType.Popup) {
			this.authService.getLogger().verbose("Interceptor - error acquiring token silently, acquiring by popup", authRequest.correlationId ?? "");
			return this.authService.acquireTokenPopup({
				...authRequest,
				scopes
			});
		}
		this.authService.getLogger().verbose("Interceptor - error acquiring token silently, acquiring by redirect", authRequest.correlationId ?? "");
		const redirectStartPage = window.location.href;
		this.authService.acquireTokenRedirect({
			...authRequest,
			scopes,
			redirectStartPage
		});
		return EMPTY;
	}
	/**
	* Looks up the scopes for the given endpoint from the protectedResourceMap
	* @param endpoint Url of the request
	* @param httpMethod Http method of the request
	* @returns Array of scopes, or null if not found
	*
	*/
	getScopesForEndpoint(endpoint, httpMethod) {
		this.authService.getLogger().verbose("Interceptor - getting scopes for endpoint", "");
		const normalizedEndpoint = this.location.normalize(endpoint);
		const protectedResourcesArray = Array.from(this.msalInterceptorConfig.protectedResourceMap.keys());
		const matchingProtectedResources = this.matchResourcesToEndpoint(protectedResourcesArray, normalizedEndpoint);
		if (matchingProtectedResources.length > 0) return this.matchScopesToEndpoint(this.msalInterceptorConfig.protectedResourceMap, matchingProtectedResources, httpMethod);
		return null;
	}
	/**
	* Finds resource endpoints that match request endpoint
	* @param protectedResourcesEndpoints
	* @param endpoint
	* @returns
	*/
	matchResourcesToEndpoint(protectedResourcesEndpoints, endpoint) {
		const matchingResources = [];
		protectedResourcesEndpoints.forEach((key) => {
			const normalizedKey = this.location.normalize(key);
			const absoluteKey = this.getAbsoluteUrl(normalizedKey);
			const keyComponents = new URL(absoluteKey);
			const absoluteEndpoint = this.getAbsoluteUrl(endpoint);
			const endpointComponents = new URL(absoluteEndpoint);
			if (this.checkUrlComponents(keyComponents, endpointComponents)) matchingResources.push(key);
		});
		return matchingResources;
	}
	/**
	* Compares URL segments between key and endpoint
	* @param key
	* @param endpoint
	* @returns
	*/
	checkUrlComponents(keyComponents, endpointComponents) {
		const urlProperties = [
			"protocol",
			"host",
			"pathname",
			"search",
			"hash"
		];
		const componentMap = {
			protocol: "protocol",
			host: "host",
			pathname: "path",
			search: "search",
			hash: "hash"
		};
		const useStrictMatching = this.msalInterceptorConfig.strictMatching !== false;
		for (const property of urlProperties) if (keyComponents[property]) {
			const decodedInput = decodeURIComponent(keyComponents[property]);
			if (useStrictMatching) {
				const component = componentMap[property];
				if (!this.matchPatternStrict(decodedInput, endpointComponents[property], component)) return false;
			} else if (!this.matchPattern(decodedInput, endpointComponents[property])) return false;
		}
		return true;
	}
	/**
	* Transforms relative urls to absolute urls
	* @param url
	* @returns
	*/
	getAbsoluteUrl(url) {
		const link = this._document.createElement("a");
		link.href = url;
		return link.href;
	}
	/**
	* Finds scopes from first matching endpoint with HTTP method that matches request
	* @param protectedResourceMap Protected resource map
	* @param endpointArray Array of resources that match request endpoint
	* @param httpMethod Http method of the request
	* @returns
	*/
	matchScopesToEndpoint(protectedResourceMap, endpointArray, httpMethod) {
		const allMatchedScopes = [];
		endpointArray.forEach((matchedEndpoint) => {
			const scopesForEndpoint = [];
			const methodAndScopesArray = protectedResourceMap.get(matchedEndpoint);
			if (methodAndScopesArray === null) {
				allMatchedScopes.push(null);
				return;
			}
			if (methodAndScopesArray === void 0) return;
			methodAndScopesArray.forEach((entry) => {
				if (typeof entry === "string") scopesForEndpoint.push(entry);
				else {
					const normalizedRequestMethod = httpMethod.toLowerCase();
					if (entry.httpMethod.toLowerCase() === normalizedRequestMethod) if (entry.scopes === null) allMatchedScopes.push(null);
					else entry.scopes.forEach((scope) => {
						scopesForEndpoint.push(scope);
					});
				}
			});
			if (scopesForEndpoint.length > 0) allMatchedScopes.push(scopesForEndpoint);
		});
		if (allMatchedScopes.length > 0) {
			if (allMatchedScopes.length > 1) this.authService.getLogger().warning("Interceptor - More than 1 matching scopes for endpoint found.", "");
			return allMatchedScopes[0];
		}
		return null;
	}
	/**
	* Tests if a given string matches a given pattern, with support for wildcards and queries.
	* @param pattern Wildcard pattern to string match. Supports "*" for wildcards and "?" for queries
	* @param input String to match against
	*/
	matchPattern(pattern, input) {
		return new RegExp(pattern.replace(/\\/g, "\\\\").replace(/\*/g, "[^ ]*").replace(/\?/g, "\\?")).test(input);
	}
	/**
	* Tests if a given string matches a given pattern using stricter, anchored
	* matching semantics.
	*
	* Differences from `matchPattern` (legacy):
	* - All regex metacharacters (including `.` and `?`) are treated as literals.
	* - The generated regex is anchored with `^` and `$` (full-string match).
	* - `*` wildcard behaviour depends on the URL component:
	*   - `host`: `*` maps to `[^.]*` — matches any characters that do NOT
	*     include `.`, so wildcards stay within a single DNS label.
	*   - All other components: `*` matches any characters.
	*
	* @param pattern - The protectedResourceMap key pattern.
	* @param input - The URL component value from the outgoing request.
	* @param component - Which URL component is being matched.
	* @returns `true` if the full input string matches the pattern.
	*/
	matchPatternStrict(pattern, input, component) {
		let regexBody = pattern.replace(/[.+^${}()|[\]\\*?]/g, "\\$&");
		if (component === "host") regexBody = regexBody.replace(/\\\*/g, "[^.]*");
		else regexBody = regexBody.replace(/\\\*/g, ".*");
		return new RegExp(`^${regexBody}$`).test(input);
	}
	static {
		this.ɵfac = function MsalInterceptor_Factory(__ngFactoryType__) {
			return new (__ngFactoryType__ || MsalInterceptor)(ɵɵinject(MSAL_INTERCEPTOR_CONFIG), ɵɵinject(MsalService), ɵɵinject(Location), ɵɵinject(MsalBroadcastService), ɵɵinject(DOCUMENT));
		};
	}
	static {
		this.ɵprov = /* @__PURE__ */ ɵɵdefineInjectable({
			token: MsalInterceptor,
			factory: MsalInterceptor.ɵfac
		});
	}
};
(() => {
	(typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MsalInterceptor, [{ type: Injectable }], () => [
		{
			type: void 0,
			decorators: [{
				type: Inject,
				args: [MSAL_INTERCEPTOR_CONFIG]
			}]
		},
		{ type: MsalService },
		{ type: Location },
		{ type: MsalBroadcastService },
		{
			type: Document,
			decorators: [{
				type: Inject,
				args: [DOCUMENT]
			}]
		}
	], null);
})();
/**
* This is a dedicated redirect component to be added to Angular apps to
* handle redirects when using @azure/msal-angular.
* Import this component to use redirects in your app.
*/
var MsalRedirectComponent = class MsalRedirectComponent {
	constructor(authService) {
		this.authService = authService;
	}
	ngOnInit() {
		this.authService.getLogger().verbose("MsalRedirectComponent activated", "");
		this.authService.handleRedirectObservable().subscribe();
	}
	static {
		this.ɵfac = function MsalRedirectComponent_Factory(__ngFactoryType__) {
			return new (__ngFactoryType__ || MsalRedirectComponent)(ɵɵdirectiveInject(MsalService));
		};
	}
	static {
		this.ɵcmp = /* @__PURE__ */ ɵɵdefineComponent({
			type: MsalRedirectComponent,
			selectors: [["app-redirect"]],
			standalone: false,
			decls: 0,
			vars: 0,
			template: function MsalRedirectComponent_Template(rf, ctx) {},
			encapsulation: 2
		});
	}
};
(() => {
	(typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MsalRedirectComponent, [{
		type: Component,
		args: [{
			selector: "app-redirect",
			template: "",
			standalone: false
		}]
	}], () => [{ type: MsalService }], null);
})();
var MsalModule = class MsalModule {
	static forRoot(msalInstance, guardConfig, interceptorConfig) {
		return {
			ngModule: MsalModule,
			providers: [
				{
					provide: MSAL_INSTANCE,
					useValue: msalInstance
				},
				{
					provide: MSAL_GUARD_CONFIG,
					useValue: guardConfig
				},
				{
					provide: MSAL_INTERCEPTOR_CONFIG,
					useValue: interceptorConfig
				},
				MsalService
			]
		};
	}
	static {
		this.ɵfac = function MsalModule_Factory(__ngFactoryType__) {
			return new (__ngFactoryType__ || MsalModule)();
		};
	}
	static {
		this.ɵmod = /* @__PURE__ */ ɵɵdefineNgModule({
			type: MsalModule,
			declarations: [MsalRedirectComponent],
			imports: [CommonModule]
		});
	}
	static {
		this.ɵinj = /* @__PURE__ */ ɵɵdefineInjector({
			providers: [MsalGuard, MsalBroadcastService],
			imports: [CommonModule]
		});
	}
};
(() => {
	(typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MsalModule, [{
		type: NgModule,
		args: [{
			declarations: [MsalRedirectComponent],
			imports: [CommonModule],
			providers: [MsalGuard, MsalBroadcastService]
		}]
	}], null, null);
})();
/**
* Custom navigation used for Angular client-side navigation.
* See performance doc for details:
* https://github.com/AzureAD/microsoft-authentication-library-for-js/tree/dev/lib/msal-angular/docs/performance.md
*/
var MsalCustomNavigationClient = class MsalCustomNavigationClient extends NavigationClient {
	constructor(authService, router, location) {
		super();
		this.authService = authService;
		this.router = router;
		this.location = location;
	}
	async navigateInternal(url, options) {
		this.authService.getLogger().trace("MsalCustomNavigationClient called", "");
		this.authService.getLogger().verbose("MsalCustomNavigationClient - navigating", "");
		this.authService.getLogger().verbosePii(`MsalCustomNavigationClient - navigating to url: ${url}`, "");
		if (options.noHistory) return super.navigateInternal(url, options);
		else {
			const urlComponents = new URL(url);
			const newUrl = urlComponents.search ? `${urlComponents.pathname}${urlComponents.search}` : this.location.normalize(urlComponents.pathname);
			await this.router.navigateByUrl(newUrl, { replaceUrl: options.noHistory });
		}
		return Promise.resolve(options.noHistory);
	}
	static {
		this.ɵfac = function MsalCustomNavigationClient_Factory(__ngFactoryType__) {
			return new (__ngFactoryType__ || MsalCustomNavigationClient)(ɵɵinject(MsalService), ɵɵinject(Router), ɵɵinject(Location));
		};
	}
	static {
		this.ɵprov = /* @__PURE__ */ ɵɵdefineInjectable({
			token: MsalCustomNavigationClient,
			factory: MsalCustomNavigationClient.ɵfac
		});
	}
};
(() => {
	(typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MsalCustomNavigationClient, [{ type: Injectable }], () => [
		{ type: MsalService },
		{ type: Router },
		{ type: Location }
	], null);
})();
//#endregion
export { MSAL_BROADCAST_CONFIG, MSAL_GUARD_CONFIG, MSAL_INSTANCE, MSAL_INTERCEPTOR_CONFIG, MsalBroadcastService, MsalCustomNavigationClient, MsalGuard, MsalInterceptor, MsalModule, MsalRedirectComponent, MsalService, version };
