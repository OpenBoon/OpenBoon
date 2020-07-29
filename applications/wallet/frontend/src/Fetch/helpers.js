import { mutate } from 'swr'
import camelCase from 'camelcase'

export const getQueryString = (params = {}) => {
  const queryString = Object.keys(params)
    .filter((p) => params[p])
    .map((p) => `${p}=${params[p]}`)
    .join('&')

  return queryString ? `?${queryString}` : ''
}

export const getCsrfToken = () => {
  if (typeof document === 'undefined') return ''

  const { csrftoken } = Object.fromEntries(
    document.cookie.split(/; */).map((c) => {
      const [key, ...v] = c.split('=')
      return [key, decodeURIComponent(v.join('='))]
    }),
  )

  return csrftoken
}

export const fetcher = async (url, options = {}) => {
  const csrftoken = getCsrfToken()

  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      'X-CSRFToken': csrftoken,
      ...options.headers,
    },
    ...options,
  })

  if ([401, 403].includes(response.status)) {
    mutate('/api/v1/me/', {}, false)
  }

  if (response.status >= 400) throw response

  try {
    const json = await response.json()
    return json
  } catch (error) {
    return response
  }
}

export const getPathname = ({ pathname }) => {
  return pathname
    .replace(/\?(.*)/, '')
    .replace(/^\/[a-z0-9-]{36}/, '/<projectId>')
    .replace(/\/[a-zA-Z0-9-_]{32}$/, '/<assetId>')
    .replace(/\/([a-z-]*)\/[a-z0-9-]{36}/g, (_, category) => {
      const item = category.replace(/s$/, '')
      return `/${category}/<${camelCase(item)}Id>`
    })
}
