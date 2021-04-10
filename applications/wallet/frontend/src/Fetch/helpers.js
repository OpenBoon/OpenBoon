import { mutate } from 'swr'
import camelCase from 'camelcase'

export const getQueryString = (params = {}) => {
  const queryString = Object.keys(params)
    .filter((p) => params[p] || params[p] === 0)
    .sort()
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
    const contentType = response.headers.get('content-type')

    if (contentType.includes('text/plain')) {
      return response.text()
    }

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

export const revalidate = async ({ key }) => {
  return mutate(key, async () => fetcher(key))
}

export const parseResponse = async ({ response }) => {
  window.scrollTo(0, 0)

  try {
    const errors = await response.json()

    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      if (errorKey === 'detail') {
        acc.global = acc.global
          ? [acc.global, ...errors.detail].join('\n')
          : errors.detail.join('\n')
      }

      if (errorKey === 'errors') {
        acc.global = acc.global
          ? [acc.global, ...errors.errors].join('\n')
          : errors.errors.join('\n')
      }

      if (errorKey !== 'detail' && errorKey !== 'errors') {
        acc[errorKey] = errors[errorKey].join('\n')
      }

      return acc
    }, {})

    if (!Object.keys(parsedErrors).length) {
      return { global: 'Something went wrong. Please try again.' }
    }

    return parsedErrors
  } catch (error) {
    return { global: 'Something went wrong. Please try again.' }
  }
}

export const getRelativeUrl = ({ url }) => {
  if (!url.includes('://')) {
    return url
  }

  const { pathname, search } = new URL(url)

  return decodeURIComponent(`${pathname}${search}`)
}
