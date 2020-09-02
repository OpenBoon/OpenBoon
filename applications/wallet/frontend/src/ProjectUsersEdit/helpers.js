import Router from 'next/router'

import { fetcher, parseResponse } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  userId,
  state: { roles: r },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    const roles = Object.keys(r).filter((key) => r[key])

    await fetcher(`/api/v1/projects/${projectId}/users/${userId}/`, {
      method: 'PUT',
      body: JSON.stringify({ roles }),
    })

    Router.push(
      '/[projectId]/users?action=edit-user-success',
      `/${projectId}/users`,
    )
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({
      isLoading: false,
      errors,
    })
  }
}
