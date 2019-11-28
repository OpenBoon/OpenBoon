/* eslint-disable no-restricted-imports */
import axios from 'axios'
import createAuthRefreshInterceptor from 'axios-auth-refresh'

import { ACCESS_TOKEN, REFRESH_TOKEN } from '../Authentication/constants'

const axiosIntercept = ({ axiosInstance }) => {
  const refreshAuthTokens = failedRequest => {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN)

    return axiosInstance
      .post('/auth/refresh/', { refresh: refreshToken })
      .then(response => {
        const { access } = response.data
        localStorage.setItem(ACCESS_TOKEN, access)
        failedRequest.response.config.headers.Authorization = `Bearer ${access}`
        return Promise.resolve()
      })
  }

  return createAuthRefreshInterceptor(axiosInstance, refreshAuthTokens)
}

const decorateHeaders = config => {
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

export const axiosCreate = (options = {}) => {
  const customDefaultOptions = {
    baseURL: '',
    withCredentials: true,
  }

  const axiosInstance = axios.create({
    ...customDefaultOptions,
    ...options,
  })

  axiosInstance.interceptors.request.use(decorateHeaders, error =>
    Promise.reject(error),
  )

  return axiosIntercept({ axiosInstance })
}
