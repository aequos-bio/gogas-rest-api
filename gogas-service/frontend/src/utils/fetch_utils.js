import swal from 'sweetalert';
const baseUrl = '';

export const status = response => {
  console.log('response headers', response.headers)
  if (response.status >= 200 && response.status < 300) {
    return Promise.resolve(response);
  }
  return response.text().then(text => Promise.reject(text));
};

const catchError = error => {
  console.error('fetch error:', error);
  swal({
    icon: 'error',
    title: 'Avviso!',
    content: error,
  });
  throw (error);
};

export const json = response => {
  return response ? response.json() : undefined
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
    .then(json)
    .catch(catchError);
}

export const postJson = (url, data) => {
  let form = new FormData();
  Object.keys(data).forEach(k => {
    form.append(k, data[k]);
  });
  return apiPost(url, form)
    .then(json)
    .catch(catchError);
}

export const post = (url, data) => {
  let form = new FormData();
  Object.keys(data).forEach(k => {
    form.append(k, data[k]);
  });
  return apiPost(url, form)
    .catch(catchError);
}

const apiGet = url =>
  fetch(baseUrl + url, { credentials: "include" })
    .then(status)
    .catch(catchError);

const apiPost = (url, data) =>
  fetch(baseUrl + url, { method: 'POST', body: data })
    .then(status)
    .catch(catchError);

// const apiDelete = url =>
//   fetch(baseUrl + url, { method: 'DELETE' })
//     .then(status)
//     .catch(catchError);

// const urlParams = obj =>
//   (obj &&
//     `?${
//       typeof obj === 'object'
//         ? Object.keys(obj)
//           .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(obj[k])}`)
//           .join('&')
//         : String(obj)
//       }`) ||
//   '';
