import { refreshToken } from '@/utils/authService'

/*  
  Wraps fetch to attach the bearer token and perform a refresh + retry on 401. 
  It returns the response so callers can handle non‑auth errors without auto‑logout.
*/

// RequestInit is a built-in type for fetch options
export async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
  const token = localStorage.getItem("accessToken"); // it knows the return type because it is defined in getItem() signature

  // predefined function signature, no need for type annotations
  let res = await fetch(url, {
    ...options,
    headers: {
      ...(options.headers || {}),
      Authorization: `Bearer ${token}`,
    },
    // For protected routes, we only authenticate with JWT (session cookie not needed))
    // credentials: "include" <-- not needed
  });


  // 3 cases:
  // 401 = try refresh
  // refresh success = retry request
  // refresh fail or retry 401 = treat as logged out

  // If 401 (Unauthorized error), try silent refresh
  if (res.status === 401) {
    console.warn("Token expired; attempting refresh...");

    let refreshed = false; // inferred type boolean
    try {
      refreshed = await refreshToken(); // true if refresh succeeded (new token stored)
    } catch (err) { // we are not reading error (.message) so no need for type annotations (unknown)
      return Promise.reject(new Error("Token refresh failed"));
    }

    // Request was successful: try original (authenticated endpoint) request again with new token for extra protection
    if (refreshed) {
      const newToken = localStorage.getItem("accessToken"); // new one from refresh
      res = await fetch(url, {
        ...options,
        headers: {
          ...(options.headers || {}),
          Authorization: `Bearer ${newToken}`,
        },
        // credentials: "include", not needed since we sending JWT to authenticated endpoint request
      });

      if (res.status === 401) {
        console.error("Still 401 after refresh; logging out");
        return Promise.reject(new Error("Unauthorized after refresh"));
      }

    // Refresh was unsuccessful: log out to clear state + redirect to login
    } else {
      return Promise.reject(new Error("Token refresh failed"));
    }
  }

  return res;
}
