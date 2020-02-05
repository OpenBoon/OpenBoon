import Router from 'next/router'

import { getCsrfToken } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  state: { firstName, lastName, email, password },
}) => {
  const csrftoken = getCsrfToken()

  try {
    const response = await fetch(`/api/v1/users/`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': csrftoken,
      },
      body: JSON.stringify({
        firstName,
        lastName,
        email,
        password,
      }),
    })

    if (response.status >= 400) throw response

    Router.push('/?action=create-account-success')
  } catch (response) {
    dispatch({ error: 'Something went wrong. Please try again.' })
  }
}
