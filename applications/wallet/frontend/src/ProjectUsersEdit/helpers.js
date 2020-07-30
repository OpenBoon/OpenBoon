import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  userId,
  state: { roles: r },
}) => {
  dispatch({ isLoading: true })

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
    dispatch({
      isLoading: false,
      error: 'Something went wrong. Please try again.',
    })
  }
}
