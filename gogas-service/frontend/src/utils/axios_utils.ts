import axios, { AxiosResponse, AxiosError } from 'axios';

export const apiGet = (url: string) => axios.get(url);
export const apiPost = (url: string, data?: any) => axios.post(url, data);
export const apiPut = (url: string, data?: any) => axios.put(url, data);
export const apiDelete = (url: string) => axios.delete(url);

export const apiPostText = (url: string, data?: any) => axios.post(url, data, { headers: { 'Content-Type': 'text/plain' }});

/**
 * PUT/POST with an explicit JSON body + Content-Type. Needed for endpoints that
 * accept a bare scalar (e.g. a number) as @RequestBody: axios does not set the
 * application/json content type for primitive bodies, which the backend rejects
 * with HTTP 415. These helpers serialize the value and set the header.
 */
const JSON_HEADERS = { 'Content-Type': 'application/json;charset=UTF-8' };

export const apiPutJson = (url: string, data: any) =>
  axios.put(url, JSON.stringify(data), { headers: JSON_HEADERS });

export const apiPostJsonBody = (url: string, data: any) =>
  axios.post(url, JSON.stringify(data), { headers: JSON_HEADERS });

export function json<T>(response: AxiosResponse<string>) {
  return response ? response.data as T : undefined;
};

export function apiGetJson<T>(url: string, data?: any) {
  const params: string[] = [];
  if (data) {
    Object.keys(data).forEach(p => {
      params.push(`${p}=${data[p]}`);
    });
  }
  let _url = url;
  if (params.length) _url += `?${params.join('&')}`;
  return apiGet(_url).then(json<T>);
};

export function apiPostJson<T>(url: string, data?: any) {
  return apiPost(url, data).then(json<T>);
};

export function extractResponseFromError(error: any) : AxiosResponse | undefined | null {
  if (!axios.isAxiosError(error)) {
    return null;
  }

  return (error as AxiosError).response;
}
