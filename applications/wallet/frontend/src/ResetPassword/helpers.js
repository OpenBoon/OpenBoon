import Router from 'next/router'

import { getCsrfToken } from '../Fetch/helpers'

export const onSubmit = async ({ dispatch, state: { email } }) => {
  const csrftoken = getCsrfToken()

  try {
    const response = await fetch(`/api/v1/password/reset/`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': csrftoken,
      },
      body: JSON.stringify({
        email,
      }),
    })

    if (response.status >= 400) throw response

    Router.push('/?action=password-reset-request-success')
  } catch (response) {
    dispatch({ error: 'Something went wrong. Please try again.' })
  }
}
