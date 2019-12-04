/* eslint-disable no-restricted-imports */
import axios from 'axios'
import createAuthRefreshInterceptor from 'axios-auth-refresh'

import { ACCESS_TOKEN, REFRESH_TOKEN } from '../Authentication/constants'

export const refreshAuthTokens = ({ axiosInstance }) => failedRequest => {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN)

  return axiosInstance
    .post('/auth/refresh/', { refresh: refreshToken })
    .then(({ data: { access } }) => {
      localStorage.setItem(ACCESS_TOKEN, access)
      failedRequest.response.config.headers.Authorization = `Bearer ${access}`
      return Promise.resolve()
    })
}

export const axiosIntercept = ({ axiosInstance }) => {
  return createAuthRefreshInterceptor(
    axiosInstance,
    refreshAuthTokens({ axiosInstance }),
  )
}

export const decorateHeaders = config => {
  const accessToken = localStorage.getItem(ACCESS_TOKEN)
  const authorization = accessToken && `Bearer ${accessToken}`

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

export const errorHandler = error => Promise.reject(error)

export const axiosCreate = (options = {}) => {
  const customDefaultOptions = {
    baseURL: '',
    withCredentials: true,
  }

  const axiosInstance = axios.create({
    ...customDefaultOptions,
    ...options,
  })

  axiosInstance.interceptors.request.use(decorateHeaders, errorHandler)

  return axiosIntercept({ axiosInstance })
}

export const fetcher = ({ axiosInstance }) => (...args) =>
  axiosInstance(...args).then(({ data }) => data)
