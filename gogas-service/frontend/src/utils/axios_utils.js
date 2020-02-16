import axios from "axios";

const baseUrl = "";

const apiGet = url => axios.get(baseUrl + url);

export const apiPost = (url, data) => axios.post(baseUrl + url, data);
export const apiPut = (url, data) => axios.put(baseUrl + url, data);
export const json = response => {
  return response ? response.data : undefined;
};

export const getJson = (url, data) => {
  const params = [];
  if (data) {
    Object.keys(data).forEach(p => {
      params.push(`${p}=${data[p]}`);
    });
  }
  let _url = url;
  if (params.length) _url += `?${params.join("&")}`;
  return apiGet(_url).then(json);
};

export const postJson = (url, data) => {
  const form = new FormData();
  Object.keys(data).forEach(k => {
    form.append(k, data[k]);
  });
  return apiPost(url, form).then(json);
};

export const postRawJson = (url, data) => {
  return axios({
    method: "post",
    url,
    headers: { "Content-Type": "application/json" },
    data
  }).then(json);
};

export const calldelete = url => {
  return axios({
    method: "delete",
    url
  }).then(json);
};

export const post = (url, data) => {
  const form = new FormData();
  Object.keys(data).forEach(k => {
    form.append(k, data[k]);
  });
  return apiPost(url, form);
};
