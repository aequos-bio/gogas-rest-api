import axios, { AxiosResponse } from 'axios';

export const apiGet = (url: string) => axios.get(url);
export const apiPost = (url: string, data?: any) => axios.post(url, data);
export const apiPut = (url: string, data?: any) => axios.put(url, data);
export const apiDelete = (url: string) => axios.delete(url);

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
}
