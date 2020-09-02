import Router from 'next/router'

import { getCsrfToken, parseResponse } from '../Fetch/helpers'

export const onRequest = async ({ dispatch, state: { email } }) => {
  dispatch({ isLoading: true, errors: {} })

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

    Router.push('/?action=password-reset-request-success', '/')
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}

export const onConfirm = async ({
  state: { newPassword1, newPassword2 },
  dispatch,
  uid,
  token,
}) => {
  dispatch({ isLoading: true, errors: {} })

  const csrftoken = getCsrfToken()

  try {
    const response = await fetch(`/api/v1/password/reset/confirm/`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': csrftoken,
      },
      body: JSON.stringify({
        newPassword1,
        newPassword2,
        uid,
        token,
      }),
    })

    if (response.status >= 400) throw response

    Router.push('/?action=password-reset-update-success', '/')
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
