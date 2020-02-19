import Router from 'next/router'

import { getCsrfToken } from '../Fetch/helpers'

export const onSubmit = async ({
  newPassword,
  confirmPassword,
  uid,
  token,
  setSubmitError,
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

    Router.push('/reset-password/?action=enter-new-password-success')
  } catch (response) {
    setSubmitError('Something went wrong. Please try again.')
  }
}
