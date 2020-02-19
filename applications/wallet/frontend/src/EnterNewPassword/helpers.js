import Router from 'next/router'

import { getCsrfToken, fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  newPassword,
  confirmPassword,
  uid,
  token,
  setSubmitError,
}) => {
  const csrftoken = getCsrfToken()

  try {
    await fetcher(`/api/v1/password/reset/confirm/`, {
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

    Router.push('/reset-password/?action=enter-new-password-success')
  } catch (response) {
    const error = await response.json()

    const { newPassword2 = [] } = error

    setSubmitError(
      newPassword2.length
        ? newPassword2[0]
        : 'Something went wrong. Please try again.',
    )
  }
}
