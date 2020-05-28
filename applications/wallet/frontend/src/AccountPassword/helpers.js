import { fetcher, getCsrfToken } from '../Fetch/helpers'
import { logout } from '../Authentication/helpers'

export const onSubmit = async ({
  dispatch,
  state: { currentPassword, newPassword, confirmPassword },
}) => {
  dispatch({ isLoading: true })

  try {
    await fetcher(`/api/v1/password/change/`, {
      method: 'POST',
      body: JSON.stringify({
        oldPassword: currentPassword,
        newPassword1: newPassword,
        newPassword2: confirmPassword,
      }),
    })

    dispatch({
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
      success: true,
      isLoading: false,
      errors: {},
    })
  } catch (response) {
    const errors = await response.json()

    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({ success: false, isLoading: false, errors: parsedErrors })
  }
}

export const onReset = async ({ setError, email, googleAuth }) => {
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

    logout({ googleAuth })({
      redirectUrl: '/?action=password-reset-request-success',
    })
  } catch (response) {
    setError('Error. Please try again.')
  }
}
