import Router from 'next/router'

import { getCsrfToken } from '../Fetch/helpers'

export const onRequest = async ({ dispatch, state: { email } }) => {
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

export const onConfirm = async ({
  state: { newPassword, confirmPassword },
  dispatch,
  uid,
  token,
}) => {
  const csrftoken = getCsrfToken()

  try {
    const response = await fetch(`/api/v1/password/reset/confirm/`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': csrftoken,
      },
      body: JSON.stringify({
        new_password1: newPassword,
        new_password2: confirmPassword,
        uid,
        token,
      }),
    })

    if (response.status >= 400) throw response

    Router.push('/?action=enter-new-password-success')
  } catch (response) {
    const error = await response.json()

    const { newPassword2 = [] } = error

    dispatch({
      errors: {
        submit: newPassword2.length
          ? newPassword2[0]
          : 'Something went wrong. Please try again.',
      },
    })
  }
}
