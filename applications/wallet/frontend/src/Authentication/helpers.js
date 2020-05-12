import Router from 'next/router'
import { cache, mutate } from 'swr'

export const authenticateUser = ({ setErrorMessage }) => async ({
  username,
  password,
  idToken,
}) => {
  cache.clear()

  setErrorMessage('')

  const response = await fetch('/api/v1/login/', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      ...(idToken ? { Authorization: `Bearer ${idToken}` } : {}),
    },
    ...(idToken ? {} : { body: JSON.stringify({ username, password }) }),
  })

  if (response.status === 401) {
    return setErrorMessage('Invalid email or password.')
  }

  if (response.status !== 200) {
    return setErrorMessage('Network error.')
  }

  const user = await response.json()
  const projectId = user.roles ? Object.keys(user.roles)[0] || '' : ''

  return mutate('/api/v1/me/', { ...user, projectId }, false)
}

export const logout = ({ googleAuth }) => async ({ redirectUrl }) => {
  googleAuth.signOut()

  const { csrftoken } = Object.fromEntries(
    document.cookie.split(/; */).map((c) => {
      const [key, ...v] = c.split('=')
      return [key, decodeURIComponent(v.join('='))]
    }),
  )

  await fetch('/api/v1/logout/', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      'X-CSRFToken': csrftoken,
    },
  })

  mutate('/api/v1/me/', {}, false)

  cache.clear()

  localStorage.clear()

  Router.push(redirectUrl)
}
