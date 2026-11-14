import axios, { AxiosResponse } from 'axios';

/**
 * POST a multipart/form-data body (e.g. a file upload). Lets the browser set
 * the multipart boundary automatically. Used for the product price-list import.
 */
export function apiPostMultipart(url: string, formData: FormData): Promise<AxiosResponse<any>> {
  return axios.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}
