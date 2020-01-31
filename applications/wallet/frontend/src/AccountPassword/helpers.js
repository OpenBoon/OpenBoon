import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { currentPassword, newPassword, confirmPassword },
}) => {
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
      errors: {},
    })

    Router.push('/[projectId]/account', `/${projectId}/account`)
  } catch (response) {
    const errors = await response.json()
    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({ errors: parsedErrors })
  }
}
