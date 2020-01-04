import axios from 'axios';
const baseUrl = '';

export const json = response => {
  return response ? response.data : undefined
};

export const getJson = (url, data) => {
  let params = [];
  if (data) {
    Object.keys(data).forEach(p => {
      params.push(p + '=' + data[p]);
    });
  }
  let _url = url;
  if (params.length)
    _url += '?' + params.join('&');
  return apiGet(_url)
    .then(json);
}

export const postJson = (url, data) => {
  let form = new FormData();
  Object.keys(data).forEach(k => {
    form.append(k, data[k]);
  });
  return apiPost(url, form)
    .then(json);
}

export const post = (url, data) => {
  let form = new FormData();
  Object.keys(data).forEach(k => {
   form.append(k, data[k]);
  });
  return apiPost(url, form);
}

const apiGet = (url) =>
  axios.get(baseUrl + url)

export const apiPost = (url, data) =>
  axios.post(baseUrl + url, data)
;
