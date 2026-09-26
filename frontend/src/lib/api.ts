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

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  try {
    const res = await fetch(url, { ...options, headers });
    
    // Some endpoints might return 401
    if (res.status === 401) {
      if (window.location.pathname !== "/ui/login") {
        window.location.href = "/ui/login";
      }
      throw new ApiError("Требуется авторизация", 401);
    }

    // Try to parse JSON response
    let json;
    try {
      json = await res.json();
    } catch (e) {
      if (!res.ok) throw new ApiError("Ошибка сервера", res.status);
      throw new ApiError("Некорректный формат ответа");
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
