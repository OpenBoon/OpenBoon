import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  userId,
  state: { permissions: p },
}) => {
  try {
    const permissions = Object.keys(p).filter(key => p[key])

    await fetcher(`/api/v1/projects/${projectId}/users/${userId}/`, {
      method: 'PATCH',
      body: JSON.stringify({ permissions }),
    })

    Router.push(
      '/[projectId]/users?action=edit-user-success',
      `/${projectId}/users?action=edit-user-success`,
    )
  } catch (response) {
    dispatch({ error: 'Something went wrong. Please try again.' })
  }
}
