
/*
  Handles token refresh and refresh scheduling only. It calls the backend refresh endpoint 
  and updates localStorage with a new access token and issued-at time.
*/

// Only handles token refresh, no React auth state management here (done in AuthContext)
let refreshTimeoutId: ReturnType<typeof setTimeout> | null = null;

// Schedule one refresh ~60 seconds before token expires (function sig so keep type annotations)
function startRefreshInterval(delayMs: number = 14 * 60 * 1000): void { // default ~14 min
  // Ensures timer exists before clearing it
  // Uses strict equality to null to catch both null and undefined
  if (refreshTimeoutId !== null) {
    clearTimeout(refreshTimeoutId); // This will be type annotated in the clearTimeout call
  }

  // setTimeout expects a callback function as first argument
  // and delay in milliseconds as second argument
  refreshTimeoutId = setTimeout(async () => {
    try {
      const ok = await refreshToken();
      if (ok) { // successful refresh
        // schedule the next one again
        startRefreshInterval(delayMs);
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        console.warn("Silent refresh failed:", err.message);
      } else {
        console.warn("Silent refresh failed with unknown error");
      }
    }
  }, delayMs); // No need to put types here, inferred from the function signature
}

// Function signature... add type annotations
function stopRefreshInterval(): void {
  if (refreshTimeoutId) {
    clearTimeout(refreshTimeoutId);
    refreshTimeoutId = null;
    console.log("Cleared token refresh timeout");
  }
}

// define async to use await inside
async function refreshToken(): Promise<boolean> {
  console.log("Attempting silent token refresh...");

  const res = await fetch("/api/v1/auth/refresh", {
    method: "POST",
    credentials: "include", // send cookies for refresh token (one of the few cases we need cookies)
  });

  if (!res.ok) {
    throw new Error("Refresh failed: " + res.status);
  }


  type RefreshResponse = {
    accessToken: string;
  };

  const data = await res.json();
  console.log("New token received:", data.accessToken);

  // no need for type annotations because setItem is apredefined function signature
  localStorage.setItem("accessToken", data.accessToken);
  localStorage.setItem("accessTokenIssuedAt", Date.now().toString());

  return true; // important signal success to caller which is fetchWithAuth check cases
}

// Export functions for use in AuthContext and elsewhere
export { refreshToken, startRefreshInterval, stopRefreshInterval };
