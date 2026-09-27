export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data: T;
}

export class ApiError extends Error {
  constructor(public message: string, public status?: number) {
    super(message);
    this.name = "ApiError";
  }
}

function getCsrfToken(): string | null {
  const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
  return match ? match[2] : null;
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const csrf = getCsrfToken();
  const headers: any = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  if (csrf && options.method && options.method !== 'GET') {
    headers["X-XSRF-TOKEN"] = csrf;
  }

  try {
    const res = await fetch(url, { ...options, headers });
    
    // Some endpoints might return 401
    if (res.status === 401) {
      if (window.location.pathname !== "/ui/login") {
        window.location.href = "/ui/login";
      }
      throw new ApiError("Сессия истекла", 401);
    }

    // Try to parse JSON response
    let json;
    try {
      json = await res.json();
    } catch (e) {
      if (!res.ok) throw new ApiError("Ошибка ответа", res.status);
      throw new ApiError("Неожиданный формат ответа");
    }

    if (!res.ok || (json.hasOwnProperty("success") && !json.success)) {
      throw new ApiError(json.message || "Произошла ошибка при выполнении запроса", res.status);
    }

    return (json.hasOwnProperty("success") && json.hasOwnProperty("data")) ? json.data : json;
  } catch (err) {
    if (err instanceof ApiError) throw err;
    throw new ApiError("Ошибка сетевого соединения");
  }
}

export const api = {
  get: <T>(url: string) => request<T>(url),
  post: <T>(url: string, body?: any) => request<T>(url, { method: "POST", body: JSON.stringify(body) }),
  put: <T>(url: string, body?: any) => request<T>(url, { method: "PUT", body: JSON.stringify(body) }),
  delete: <T>(url: string) => request<T>(url, { method: "DELETE" }),
};
