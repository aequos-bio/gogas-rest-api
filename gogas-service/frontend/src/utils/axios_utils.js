import axios from 'axios';
const baseUrl = '';

axios.interceptors.response.use(response => {
  console.log('response headers', response.headers)
  //if (response.status >= 200 && response.status < 300) {
    return response;
  //}
  //return response.text().then(text => Promise.reject(text));
}, error => {
 throw error;
});

export const json = response => {
  return response ? response.data : undefined
};

export const getJson = (url, data, auth) => {
  let params = [];
  if (data) {
    Object.keys(data).forEach(p => {
      params.push(p + '=' + data[p]);
    });
  }
  let _url = url;
  if (params.length)
    _url += '?' + params.join('&');
  return apiGet(_url, auth)
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

const apiGet = (url, token) =>
  axios.get(baseUrl + url, token ? {headers:{'Authorization': "Bearer " + token}} : null)

export const apiPost = (url, data) =>
  axios.post(baseUrl + url, data)
;
