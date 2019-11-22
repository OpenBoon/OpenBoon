/* eslint-disable no-restricted-imports */
import axios from 'axios'
import createAuthRefreshInterceptor from 'axios-auth-refresh'
import { ACCESS_TOKEN, REFRESH_TOKEN } from '../constants/authConstants'

function axiosIntercept(axiosInstance) {
  const refreshAuthTokens = failedRequest => {
    const refreshTokenRaw = localStorage.getItem(REFRESH_TOKEN)
    const refreshTokenParsed =
      refreshTokenRaw && JSON.parse(localStorage[REFRESH_TOKEN]).split(' ')[1]

    return axiosInstance
      .post('/auth/refresh/', { refresh: refreshTokenParsed })
      .then(response => {
        const { access } = response.data
        const stringifiedAccessToken = JSON.stringify(`Bearer ${access}`)
        localStorage.setItem(ACCESS_TOKEN, stringifiedAccessToken)
        failedRequest.response.config.headers.Authorization = stringifiedAccessToken
        return Promise.resolve()
      })
  }

  return createAuthRefreshInterceptor(axiosInstance, refreshAuthTokens)
}

function decorateHeaders(config) {
  const accessToken = localStorage.getItem(ACCESS_TOKEN)
  const authorization = accessToken && JSON.parse(localStorage[ACCESS_TOKEN])

  const headers = {
    ...config.headers,
  }

  if (authorization) {
    headers.Authorization = authorization
  }

  return {
    ...config,
    headers,
  }
}

export function axiosCreate(options = {}) {
  const customDefaultOptions = {
    baseURL: process.env.NODE_ENV === 'development' ? 'http://localhost' : '',
    withCredentials: true,
  }

  const axiosInstance = axios.create({
    ...customDefaultOptions,
    ...options,
  })

  axiosInstance.interceptors.request.use(decorateHeaders, function(error) {
    return Promise.reject(error)
  })

  return axiosIntercept(axiosInstance)
}
