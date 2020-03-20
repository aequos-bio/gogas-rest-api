import axios from 'axios';

export const apiGet = url => axios.get(url);
export const apiPost = (url, data) => axios.post(url, data);
export const apiPut = (url, data) => axios.put(url, data);
export const apiDelete = url => axios.delete(url);

export const json = response => {
  return response ? response.data : undefined;
};

export const apiGetJson = (url, data) => {
  const params = [];
  if (data) {
    Object.keys(data).forEach(p => {
      params.push(`${p}=${data[p]}`);
    });
  }
  let _url = url;
  if (params.length) _url += `?${params.join('&')}`;
  return apiGet(_url).then(json);
};
