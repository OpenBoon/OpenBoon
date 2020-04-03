import { mutate } from 'swr'

let enhancedFetch

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

export const initializeFetcher = () => {
  enhancedFetch = async (url, options = {}) => {
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

      return {}
    }

    if (response.status >= 400) throw response

    try {
      const json = await response.json()
      return json
    } catch (error) {
      return response
    }
  }

  return enhancedFetch
}

export const fetcher = (...args) => {
  return enhancedFetch(...args)
}
