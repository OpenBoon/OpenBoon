import Router from 'next/router'

import { getCsrfToken } from '../Fetch/helpers'

const BASE_HEADER = {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'X-CSRFToken': getCsrfToken(),
  },
}

export const onSubmit = async ({
  dispatch,
  state: { firstName, lastName, email, password },
}) => {
  try {
    const response = await fetch(`/api/v1/accounts/register`, {
      ...BASE_HEADER,
      body: JSON.stringify({
        email,
        firstName,
        lastName,
        password,
      }),
    })

    if (response.status >= 400) throw response

    Router.push('/?action=create-account-success')
  } catch (response) {
    dispatch({ error: 'Something went wrong. Please try again.' })
  }
}

export const onConfirm = async ({ userId, token }) => {
  try {
    const response = await fetch(`/api/v1/accounts/confirm`, {
      ...BASE_HEADER,
      body: JSON.stringify({
        userId,
        token,
      }),
    })

    if (response.status >= 400) throw response

    Router.push('/?action=account-activate-success')
  } catch (response) {
    Router.push('/create-account?action=account-activate-expired')
  }
}
