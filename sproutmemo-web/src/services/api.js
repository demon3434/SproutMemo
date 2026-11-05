const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';

// Token storage helpers
export const tokens = {
  getAccess: () => localStorage.getItem('access_token'),
  getRefresh: () => localStorage.getItem('refresh_token'),
  set: (access, refresh) => {
    localStorage.setItem('access_token', access);
    localStorage.setItem('refresh_token', refresh);
  },
  clear: () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
  }
};

let isRefreshing = false;
let refreshSubscribers = [];

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb);
}

function onRefreshed(accessToken) {
  refreshSubscribers.forEach((cb) => cb(accessToken));
  refreshSubscribers = [];
}

async function request(url, options = {}) {
  // Construct absolute URL
  const absoluteUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`;

  // Attach headers
  options.headers = options.headers || {};
  const token = tokens.getAccess();
  if (token && !options.headers['Authorization']) {
    options.headers['Authorization'] = `Bearer ${token}`;
  }

  // Handle Form Data headers properly (fetch sets content-type for multi-part automatically)
  if (!(options.body instanceof FormData) && !options.headers['Content-Type']) {
    options.headers['Content-Type'] = 'application/json';
  }

  try {
    const response = await fetch(absoluteUrl, options);
    
    // If token expired (401 Unauthorized), trigger token rotation
    if (response.status === 401 && tokens.getRefresh()) {
      if (!isRefreshing) {
        isRefreshing = true;
        
        return new Promise(async (resolve, reject) => {
          try {
            const refreshRes = await fetch(`${BASE_URL}/auth/refresh`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ refresh_token: tokens.getRefresh() })
            });

            if (refreshRes.ok) {
              const data = await refreshRes.json();
              if (data.code === 200) {
                const { access_token, refresh_token } = data.data;
                tokens.set(access_token, refresh_token);
                
                // Retry the original request
                options.headers['Authorization'] = `Bearer ${access_token}`;
                const retryResponse = await request(url, options);
                
                // Resolve all other queued requests
                onRefreshed(access_token);
                isRefreshing = false;
                
                resolve(retryResponse);
                return;
              }
            }
            throw new Error('Refresh failed');
          } catch (err) {
            isRefreshing = false;
            tokens.clear();
            window.dispatchEvent(new Event('logout'));
            reject(err);
          }
        });
      }

      // Queue original request until refresh finishes
      return new Promise((resolve) => {
        subscribeTokenRefresh((newAccessToken) => {
          options.headers['Authorization'] = `Bearer ${newAccessToken}`;
          resolve(request(url, options));
        });
      });
    }

    return response;
  } catch (error) {
    console.error('Request failed:', error);
    throw error;
  }
}

export const api = {
  get: (url, options = {}) => request(url, { ...options, method: 'GET' }),
  post: (url, body, options = {}) => request(url, {
    ...options,
    method: 'POST',
    body: body instanceof FormData ? body : JSON.stringify(body)
  }),
  put: (url, body, options = {}) => request(url, {
    ...options,
    method: 'PUT',
    body: body instanceof FormData ? body : JSON.stringify(body)
  }),
  delete: (url, options = {}) => request(url, { ...options, method: 'DELETE' })
};
